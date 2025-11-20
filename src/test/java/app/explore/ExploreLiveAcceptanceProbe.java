package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.ExploreDataProvider;
import app.controller.explore.MergedExploreDataProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExploreLiveAcceptanceProbe {

    private static boolean liveEnabled() {
        return System.getenv("EXPLORE_LIVE") != null;
    }

    private static boolean envPresent(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            if (v == null || v.isBlank())
                return false;
        }
        return true;
    }

    private static Connection openEnvConn() throws Exception {
        String url = System.getenv("DATABASE_URL");
        String user = System.getenv("DATABASE_USER");
        String pass = System.getenv("DATABASE_PASSWORD");
        return DriverManager.getConnection(url, user, pass);
    }

    private static boolean dbRoundTripReady(int attempts, long delayMs) {
        if (!envPresent("DATABASE_URL", "DATABASE_USER", "DATABASE_PASSWORD"))
            return false;
        for (int i = 0; i < attempts; i++) {
            try (Connection c = openEnvConn();
                    Statement s = c.createStatement();
                    ResultSet rs = s.executeQuery("SELECT 1")) {
                if (rs.next())
                    return true;
            } catch (Exception _) {
                // ignore and retry
            }
        }
        return false;
    }

    @Test
    void printsSomeCounts_whenLive() {
        Assumptions.assumeTrue(
                liveEnabled() && dbRoundTripReady(8, 1500),
                "Skipping live acceptance probe: EXPLORE_LIVE not set or DB not ready");

        ExploreDataProvider provider = new MergedExploreDataProvider();
        var items = provider.fetch("", ExploreControl.Kind.ALL);
        assertNotNull(items);

        long artists = items.stream().filter(i -> i.kind() == ExploreControl.Kind.ARTISTS).count();
        long designs = items.stream().filter(i -> i.kind() == ExploreControl.Kind.DESIGNS).count();
        long completed = items.stream().filter(i -> i.kind() == ExploreControl.Kind.COMPLETED_TATTOOS).count();

        System.out.println("[ExploreLiveAcceptanceProbe] counts => " +
                "artists=" + artists + ", designs=" + designs + ", completed=" + completed +
                ", total=" + items.size());
    }
}
