import tc.yigit.bukkit.PortlessRelay;
import tc.yigit.shared.UtilSocket;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class RelayStateTest {
    private static int passed;
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        passed++;
    }
    private static Object read(Object instance, String name) throws Exception {
        Field field = PortlessRelay.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }
    private static PortlessRelay instance(File file) throws Exception {
        PortlessRelay relay = new PortlessRelay(null);
        Field field = PortlessRelay.class.getDeclaredField("stateFile");
        field.setAccessible(true);
        field.set(relay, file);
        return relay;
    }
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("zonely-relay-state-test-").toFile();
        File state = new File(directory, "relay-state-1.txt");
        Method save = PortlessRelay.class.getDeclaredMethod("saveState", long.class, String.class);
        Method load = PortlessRelay.class.getDeclaredMethod("loadState");
        save.setAccessible(true);
        load.setAccessible(true);
        try {
            PortlessRelay beforeRestart = instance(state);
            save.invoke(beforeRestart, 41L, "pending");
            PortlessRelay afterCrash = instance(state);
            load.invoke(afterCrash);
            check(((Long) read(afterCrash, "lastClaimedId")) == 41L, "Crash sonrasi komut ID kalici");
            check("pending".equals(read(afterCrash, "lastResult")), "ACK oncesi durum kalici");
            save.invoke(afterCrash, 41L, "done");
            PortlessRelay afterRestart = instance(state);
            load.invoke(afterRestart);
            check("done".equals(read(afterRestart, "lastResult")), "Basari sonucu yeniden baslatmada kalici");
            save.invoke(afterRestart, 42L, "failed");
            PortlessRelay failed = instance(state);
            load.invoke(failed);
            check("failed".equals(read(failed, "lastResult")), "Hata sonucu yeniden baslatmada kalici");
            Files.write(state.toPath(), "invalid".getBytes(StandardCharsets.US_ASCII));
            boolean rejected = false;
            try { load.invoke(instance(state)); }
            catch (java.lang.reflect.InvocationTargetException expected) { rejected = true; }
            check(rejected, "Bozuk kayit fail-closed");
            check(directory.listFiles().length == 1, "Gecici kayit dosyalari temiz");
            String sample = null;
            for (int i = 0; i < 4096; i++) {
                String candidate = "websender-test-" + i;
                if (MessageDigest.getInstance("SHA-512").digest(candidate.getBytes(StandardCharsets.UTF_8))[0] == 0) { sample = candidate; break; }
            }
            check(sample != null && UtilSocket.hash(sample).length() == 128 && UtilSocket.hash(sample).startsWith("00"), "SHA-512 basindaki sifirlar korunur");
            System.out.println("Java kalici relay kaydi ve SHA-512: " + passed + "/" + passed + " PASS");
        } finally {
            Files.deleteIfExists(state.toPath());
            Files.deleteIfExists(directory.toPath());
        }
    }
}
