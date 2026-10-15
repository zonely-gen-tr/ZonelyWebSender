import tc.yigit.shared.ConfigFile;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

public final class ConfigFileTest {
    private static int passed;
    private static byte[] bundled;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        passed++;
    }
    private static void prepare(Path file) throws Exception {
        ConfigFile.prepare(file.toFile(), new ByteArrayInputStream(bundled));
    }
    private static String read(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(text.startsWith("\uFEFF") ? text.substring(1) : text);
        return config;
    }
    public static void main(String[] args) throws Exception {
        bundled = Files.readAllBytes(Paths.get(args[0]));
        Path directory = Files.createTempDirectory("zonely-config-test-");
        try {
            Path fresh = directory.resolve("fresh.yml");
            prepare(fresh);
            check(Arrays.equals(bundled, Files.readAllBytes(fresh)), "Fresh install copies the commented resource exactly");
            check("portless".equals(yaml(read(fresh)).getString("connection-mode")), "Fresh default is portless");
            prepare(fresh);
            check(Arrays.equals(bundled, Files.readAllBytes(fresh)), "Restart preserves fresh file byte for byte");

            String[] fixtures = {
                "connection-mode: portless\nrelay-url: 'https://site.test/'\nsocketPassword: '  O''Brien Şifre # ! : 汉  '\npoll-ticks: 43\nsocketPort: 12345\nsenderPrefix: '[Custom]'\n",
                "# User header\nconnection-mode: port\nsocketPort: 28765 # my TCP port\nsocketPassword: 'saved-password'\ncustom-option: true\n",
                "socketPort: 19876\nsocketPassword: 'legacy-secret'\n",
                "\uFEFF'connection-mode': portless\r\n\"socketPort\": 27865\r\nsocketPassword: 'secret'\r\ncustom:\r\n  values: [a, b]\r\n",
                "connection-mode: portless\nsocketPassword: |-\n  first line # literal\n  second line\nrelay-url: 'https://site.test/sub/'\n",
            };
            for (int i = 0; i < fixtures.length; i++) {
                Path file = directory.resolve("existing-" + i + ".yml");
                Files.write(file, fixtures[i].getBytes(StandardCharsets.UTF_8));
                YamlConfiguration before = yaml(fixtures[i]);
                prepare(file);
                String output = read(file);
                YamlConfiguration after = yaml(output);
                for (String key : before.getKeys(true)) {
                    if (!before.isConfigurationSection(key)) check(Objects.equals(before.get(key), after.get(key)), "Existing value preserved: " + key);
                }
                check(output.contains("# PORTSUZ:") && output.contains("# Yalnizca PORTLU modda"), "Both connection instructions restored");
                check(output.contains("# En az 12 karakter"), "Password instructions restored");
                if (i == 1) check(output.contains("# User header") && output.contains("# my TCP port"), "User comments preserved");
                if (i == 2) check("port".equals(after.getString("connection-mode")), "Legacy TCP install stays TCP");
                if (i == 3) check(output.startsWith("\uFEFF") && !output.replace("\r\n", "").contains("\n"), "BOM and Windows line endings preserved");
                byte[] once = Files.readAllBytes(file);
                prepare(file);
                check(Arrays.equals(once, Files.readAllBytes(file)), "Repeated startup does not duplicate comments or change values");
            }
            Path invalid = directory.resolve("invalid.yml");
            byte[] broken = "socketPassword: [broken\n".getBytes(StandardCharsets.UTF_8);
            Files.write(invalid, broken);
            try { prepare(invalid); throw new AssertionError("Invalid YAML accepted"); }
            catch (IOException expected) { check(Arrays.equals(broken, Files.readAllBytes(invalid)), "Invalid file remains untouched"); }
            System.out.println("Config creation / comment restoration / saved values / legacy mode / restart: " + passed + "/" + passed + " PASS");
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.list(directory)) {
                for (Path path : (Iterable<Path>) paths::iterator) Files.delete(path);
            }
            Files.delete(directory);
        }
    }
}
