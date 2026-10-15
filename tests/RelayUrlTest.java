import tc.yigit.bukkit.PortlessRelay;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public final class RelayUrlTest {
    public static void main(String[] args) throws Exception {
        Method resolve = PortlessRelay.class.getDeclaredMethod("resolveEndpoint", String.class);
        resolve.setAccessible(true);
        String suffix = "/Files/Controllers/ControllersAll.php?main=Handlers&container=MainHandlers&zonely_websender_relay=1";
        String[][] valid = {
            {"https://example.test", "https://example.test" + suffix},
            {"https://example.test/", "https://example.test" + suffix},
            {"  https://example.test///  ", "https://example.test" + suffix},
            {"example.test/", "https://example.test" + suffix},
            {"HTTPS://EXAMPLE.TEST:443/", "https://example.test" + suffix},
            {"https://example.test/zonely/", "https://example.test/zonely" + suffix},
            {"https://example.test/a%20b/", "https://example.test/a%20b" + suffix},
            {"https://example.test" + suffix, "https://example.test" + suffix},
            {"https://example.test/zonely/Files/Controllers/ControllersAll.php/?main=Handlers&container=MainHandlers&zonely_websender_relay=1", "https://example.test/zonely" + suffix},
            {"https://example.test/Files/Src/MVC/Http/Endpoints/websender-relay.php", "https://example.test" + suffix},
            {"https://example.test:8443/", "https://example.test:8443" + suffix}
        };
        int passed = 0;
        for (String[] item : valid) {
            if (!item[1].equals(resolve.invoke(null, item[0]))) throw new AssertionError("Site adresi eslemesi: " + item[0]);
            passed++;
        }
        String[] invalid = {null, "", "http://example.test", "ftp://example.test", "https://user:pass@example.test", "https://example.test/#x", "https://example.test/?x=1", "https://example.test:0", "https://example.test:65536", "https://bad host.test/"};
        for (String item : invalid) {
            try {
                resolve.invoke(null, item);
                throw new AssertionError("Gecersiz adres kabul edildi");
            } catch (InvocationTargetException expected) {
                if (!(expected.getCause() instanceof IllegalArgumentException)) throw expected;
            }
            passed++;
        }
        System.out.println("Site adresi / slash / eski tam adres / HTTPS dogrulamasi: " + passed + "/" + passed + " PASS");
    }
}
