package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.MergedExploreDataProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class ExploreLiveAcceptanceProbe {

    private static boolean liveEnabled() {
        return System.getenv("EXPLORE_LIVE") != null;
    }

    @Test
    void printsSomeCounts_whenLive() {
        // Skip unless live mode is explicitly enabled
        Assumptions.assumeTrue(liveEnabled(), "Skipping: EXPLORE_LIVE not set");

        var provider = new MergedExploreDataProvider();
        var all = provider.fetch("", ExploreControl.Kind.ALL);
        System.out.println("[Probe] live items=" + (all == null ? 0 : all.size()));
    }
}
