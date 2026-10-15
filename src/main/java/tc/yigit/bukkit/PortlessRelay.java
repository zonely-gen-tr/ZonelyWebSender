package tc.yigit.bukkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** Adds outbound HTTPS command delivery to the original WebSender Bukkit plugin. */
public final class PortlessRelay {
    private final BukkitMain plugin;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private File stateFile;
    private long lastClaimedId;
    private String lastResult = "failed";
    private BukkitTask task;
    private String endpoint;
    private String auth;
    private long lastError;

    public PortlessRelay(BukkitMain plugin) { this.plugin = plugin; }

    public void start() {
        endpoint = plugin.getConfig().getString("relay-url", "").trim();
        int serverId = plugin.getConfig().getInt("server-id", 0);
        String password = plugin.getConfig().getString("socketPassword", "");
        int pollTicks = Math.max(20, plugin.getConfig().getInt("poll-ticks", 20));
        if (serverId <= 0 || password.length() < 12 || password.equals("BURAYA_EN_AZ_12_KARAKTER_SIFRE") || !endpoint.startsWith("https://")) {
            plugin.getLogger().severe("Portsuz mod: HTTPS relay-url, server-id ve en az 12 karakterli socketPassword gerekli.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        auth = "Basic " + Base64.getEncoder().encodeToString((serverId + ":" + password).getBytes(StandardCharsets.UTF_8));
        plugin.getDataFolder().mkdirs();
        stateFile = new File(plugin.getDataFolder(), "relay-state-" + serverId + ".txt");
        try { loadState(); }
        catch (Exception error) {
            plugin.getLogger().log(Level.SEVERE, "Portsuz komut kaydi okunamadi; tekrar teslim riski nedeniyle eklenti kapatiliyor.", error);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            @Override public void run() { poll(); }
        }, 20L, pollTicks);
        plugin.getLogger().info("WebSender kaynak tabanli HTTPS modu etkin. Yeni gelen TCP portu acilmiyor.");
    }

    public void stop() { if (task != null) task.cancel(); }

    private void poll() {
        if (!busy.compareAndSet(false, true)) return;
        boolean release = true;
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "poll");
            JsonObject response = post(request);
            JsonElement item = response.get("job");
            if (item == null || item.isJsonNull()) return;
            final JsonObject job = item.getAsJsonObject();
            final long id = job.get("id").getAsLong();
            final String action = job.get("action").getAsString();
            final String payload = job.get("payload").getAsString();
            if (id <= lastClaimedId) { ack(id, id == lastClaimedId && "done".equals(lastResult)); return; }
            // Save the claim before Bukkit executes the command. After a crash, a
            // pending claim is acknowledged as failed instead of being replayed.
            saveState(id, "pending");
            plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    boolean success = false;
                    try {
                        if ("command".equals(action)) {
                            String command = payload.startsWith("/") ? payload.substring(1) : payload;
                            success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        } else if ("message".equals(action)) {
                            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', payload));
                            success = true;
                        }
                    } catch (Throwable error) {
                        plugin.getLogger().log(Level.WARNING, "WebSender komutu islenemedi, id=" + id, error);
                    }
                    final boolean acknowledged = success;
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override public void run() {
                            boolean saved = false;
                            try { saveState(id, acknowledged ? "done" : "failed"); saved = true; }
                            catch (Exception error) { logError("Komut sonucu kaydedilemedi: ", error); }
                            finally { ack(id, saved && acknowledged); busy.set(false); }
                        }
                    });
                }
            });
            release = false;
        } catch (Exception error) {
            logError("HTTPS sorgusu basarisiz: ", error);
        } finally {
            if (release) busy.set(false);
        }
    }

    private synchronized void loadState() throws Exception {
        if (!stateFile.exists()) return;
        byte[] content = Files.readAllBytes(stateFile.toPath());
        if (content.length > 128) throw new IllegalStateException("Komut kaydi gecersiz");
        String[] parts = new String(content, StandardCharsets.US_ASCII).trim().split(":");
        if (parts.length != 2 || !(parts[1].equals("pending") || parts[1].equals("done") || parts[1].equals("failed")))
            throw new IllegalStateException("Komut kaydi gecersiz");
        lastClaimedId = Long.parseLong(parts[0]);
        if (lastClaimedId < 0) throw new IllegalStateException("Komut kimligi gecersiz");
        lastResult = parts[1];
    }

    private synchronized void saveState(long id, String result) throws Exception {
        File temp = File.createTempFile("relay-state-", ".tmp", stateFile.getParentFile());
        try {
            try (FileOutputStream stream = new FileOutputStream(temp)) {
                stream.write((id + ":" + result + "\n").getBytes(StandardCharsets.US_ASCII));
                stream.getFD().sync();
            }
            try { Files.move(temp.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException error) {
                Files.move(temp.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            lastClaimedId = id;
            lastResult = result;
        } finally { if (temp.exists()) temp.delete(); }
    }

    private void ack(long id, boolean success) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "ack");
            request.addProperty("id", id);
            request.addProperty("success", success);
            post(request);
        } catch (Exception error) {
            logError("Komut onayi basarisiz: ", error);
        }
    }

    private void logError(String prefix, Exception error) {
        if (System.currentTimeMillis() - lastError > 30000) {
            lastError = System.currentTimeMillis();
            plugin.getLogger().warning(prefix + error.getClass().getSimpleName() + " (" + error.getMessage() + ")");
        }
    }

    private JsonObject post(JsonObject json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bytes.length);
        try {
            try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
            if (conn.getResponseCode() != 200) throw new IllegalStateException("HTTP " + conn.getResponseCode());
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    if (response.length() > 12000) throw new IllegalStateException("Yanit cok buyuk");
                }
            }
            JsonObject result = new JsonParser().parse(response.toString()).getAsJsonObject();
            if (!result.has("ok") || !result.get("ok").getAsBoolean()) throw new IllegalStateException("Yanit hatali");
            return result;
        } finally { conn.disconnect(); }
    }
}
