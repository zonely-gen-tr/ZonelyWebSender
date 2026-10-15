package tc.yigit.shared;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keep the bundled comments without serializing the user's YAML values. */
public final class ConfigFile {
    private static final Pattern KEY = Pattern.compile("^(?:([A-Za-z0-9_-]+)|'([A-Za-z0-9_-]+)'|\"([A-Za-z0-9_-]+)\")\\s*:");

    private ConfigFile() {}

    public static void prepare(File file, InputStream bundled) throws IOException {
        if (bundled == null) throw new IOException("Missing bundled config");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int count;
        while ((count = bundled.read(chunk)) != -1) buffer.write(chunk, 0, count);
        byte[] defaults = buffer.toByteArray();
        Path target = file.toPath();
        if (!Files.exists(target)) {
            Files.createDirectories(target.toAbsolutePath().getParent());
            Files.write(target, defaults, StandardOpenOption.CREATE_NEW);
            return;
        }

        String original = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        boolean bom = original.startsWith("\uFEFF");
        String text = bom ? original.substring(1) : original;
        YamlConfiguration parsed = new YamlConfiguration();
        try {
            parsed.loadFromString(text);
        } catch (InvalidConfigurationException error) {
            // Do not rewrite invalid YAML or include its potentially secret values.
            throw new IOException("Invalid config YAML");
        }
        Set<String> existing = parsed.getKeys(false);
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        Map<String, Setting> settings = settings(new String(defaults, StandardCharsets.UTF_8));
        List<String> output = new ArrayList<String>();
        for (String line : text.split("\\r?\\n", -1)) {
            Setting setting = settings.get(key(line));
            if (setting != null && !endsWith(output, setting.comments)) output.addAll(setting.comments);
            output.add(line);
        }
        for (Map.Entry<String, Setting> entry : settings.entrySet()) {
            if (existing.contains(entry.getKey())) continue;
            if (!output.isEmpty() && !output.get(output.size() - 1).isEmpty()) output.add("");
            output.addAll(entry.getValue().comments);
            // Existing pre-portless installations keep their original TCP mode.
            output.add(entry.getKey().equals("connection-mode") ? "connection-mode: port" : entry.getValue().line);
        }
        String updated = (bom ? "\uFEFF" : "") + String.join(newline, output);
        if (updated.equals(original)) return;
        YamlConfiguration checked = new YamlConfiguration();
        try {
            checked.loadFromString(bom ? updated.substring(1) : updated);
        } catch (InvalidConfigurationException error) {
            throw new IOException("Updated config YAML could not be validated");
        }
        for (String name : parsed.getKeys(true)) {
            if (!parsed.isConfigurationSection(name) && !Objects.equals(parsed.get(name), checked.get(name))) {
                throw new IOException("Config values must remain unchanged");
            }
        }
        Path temporary = Files.createTempFile(target.toAbsolutePath().getParent(), ".config-", ".tmp");
        try {
            Files.write(temporary, updated.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String key(String line) {
        Matcher match = KEY.matcher(line);
        if (!match.find()) return "";
        for (int i = 1; i <= 3; i++) if (match.group(i) != null) return match.group(i);
        return "";
    }

    private static Map<String, Setting> settings(String template) {
        Map<String, Setting> result = new LinkedHashMap<String, Setting>();
        List<String> comments = new ArrayList<String>();
        for (String line : template.split("\\r?\\n")) {
            if (line.startsWith("#")) comments.add(line);
            else {
                String key = key(line);
                if (!key.isEmpty()) result.put(key, new Setting(line, new ArrayList<String>(comments)));
                comments.clear();
            }
        }
        return result;
    }

    private static boolean endsWith(List<String> lines, List<String> suffix) {
        if (lines.size() < suffix.size()) return false;
        return lines.subList(lines.size() - suffix.size(), lines.size()).equals(suffix);
    }

    private static final class Setting {
        final String line;
        final List<String> comments;
        Setting(String line, List<String> comments) { this.line = line; this.comments = comments; }
    }
}
