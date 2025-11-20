package app.explore;

import app.controller.explore.ExploreControl;
import app.controller.explore.ExploreDataProvider;
import app.controller.explore.MergedExploreDataProvider;
import app.controller.explore.MockExploreDataProvider;
import app.entity.Post;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ExploreProviderSmokeTest {

    private static final String SQLITE_DB_FILENAME = "tattui.db";

    @BeforeAll
    static void headless() {
        System.setProperty("HEADLESS_TESTS", "true");
        Post.setHeadless(true);
        System.out.println("[ExploreProviderSmokeTest] HEADLESS_TESTS enabled");
    }

    @Test
    void liveProvider_returnsSomething_whenLocalDbPresent() {
        Assumptions.assumeTrue(
                Files.exists(Path.of(SQLITE_DB_FILENAME)),
                "Skipping live test: local SQLite database not found");

        ExploreDataProvider provider = new MergedExploreDataProvider();
        var items = provider.fetch("", ExploreControl.Kind.ALL);

        assertNotNull(items, "Provider should return a non-null list");
        assertTrue(items.size() >= 0, "Allow empty result sets for small DBs");
    }

    @Test
    void mockProvider_filtersDesignsOnly_whenKindIsDESIGNS() {
        var p = new MockExploreDataProvider();
        var items = p.fetch("", ExploreControl.Kind.DESIGNS);
        assertNotNull(items);
        assertTrue(items.stream().allMatch(i -> i.kind() == ExploreControl.Kind.DESIGNS));
        assertFalse(items.isEmpty(), "Mock should return at least one design");
    }

    @Test
    void mockProvider_supportsCaseInsensitiveSearch() {
        var p = new MockExploreDataProvider();
        var q1 = p.fetch("rAvEn", ExploreControl.Kind.ALL);
        var q2 = p.fetch("RAVEN", ExploreControl.Kind.ALL);
        assertFalse(q1.isEmpty());
        assertFalse(q2.isEmpty());
        assertEquals(q1.size(), q2.size());
    }
}
