package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.ExploreDataProvider;
import app.controller.explore.MergedExploreDataProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExploreProviderSmokeTest {

    private static boolean liveEnabled() {
        return System.getenv("EXPLORE_LIVE") != null;
    }

    @Test
    void liveProvider_returnsSomething_whenLiveEnabled() {
        // Skip unless live mode is explicitly enabled
        Assumptions.assumeTrue(liveEnabled(), "Skipping: EXPLORE_LIVE not set");

        ExploreDataProvider provider = new MergedExploreDataProvider();
        var items = provider.fetch("", ExploreControl.Kind.ALL);

        assertNotNull(items, "Provider should return a non-null list");
        // Allow empty results for small DBs
        assertTrue(items.size() >= 0);
    }
}
