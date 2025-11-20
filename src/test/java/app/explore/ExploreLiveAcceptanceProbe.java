package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.ExploreDataProvider;
import app.controller.explore.MergedExploreDataProvider;
import app.entity.Post;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExploreLiveAcceptanceProbe {

    private static final String SQLITE_DB_FILENAME = "tattui.db";

    @BeforeAll
    static void headless() {
        System.setProperty("HEADLESS_TESTS", "true");
        Post.setHeadless(true);
        System.out.println("[ExploreLiveAcceptanceProbe] HEADLESS_TESTS enabled");
    }

    @Test
    void printsSomeCounts_withLocalDb() {
        Assumptions.assumeTrue(
                Files.exists(Path.of(SQLITE_DB_FILENAME)),
                "Skipping explore probe: local SQLite database not found");

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
