package app.db;

import app.entity.DatabaseConnector;
import app.entity.Post;
import app.entity.Profile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DbProfilesProbeTest {

    @BeforeAll
    static void headless() {
        // Ensure tests never try to spin up JavaFX image loader
        System.setProperty("HEADLESS_TESTS", "true");
        Post.setHeadless(true);
        System.out.println("[Probe] HEADLESS_TESTS enabled");
    }

    /** Probe 1: list a few profiles with post counts; never fail build. */
    @Test
    public void profilesLikeAnything() {
        System.out.println("\n[Probe] profilesLikeAnything()");
        try {
            List<Profile> profiles = DatabaseConnector.getProfilesLike("");
            int size = (profiles == null ? 0 : profiles.size());
            System.out.println("[Probe] fetched profiles count: " + size);
            if (profiles != null) {
                int count = 0;
                for (Profile p : profiles) {
                    int postCount = (p.getPosts() == null ? 0 : p.getPosts().size());
                    System.out.println("  @" + p.getUsername() + " | posts=" + postCount);
                    if (++count >= 10) break;
                }
            }
        } catch (Throwable t) {
            System.out.println("[Probe] ERROR in profilesLikeAnything:");
            System.out.println("[Probe] cause: " + t.getMessage());
        }
    }

    /** Probe 2: fetch a known user; never fail build. */
    @Test
    public void profileByUsername() {
        final String username = "william"; // change if needed
        System.out.println("\n[Probe] profileByUsername('" + username + "')");
        try {
            Profile p = DatabaseConnector.getProfileByUsername(username);
            if (p == null) {
                System.out.println("[Probe] '" + username + "' not found.");
                return;
            }
            System.out.println("  @" + p.getUsername() + " bio=" + p.getBiography());
            if (p.getPosts() != null) {
                p.getPosts().stream().limit(5).forEach(post ->
                        System.out.println("    • post #" + post.getId() + " : "
                                + post.getCaption() + " | " + post.getPostURL())
                );
            }
        } catch (Throwable t) {
            System.out.println("[Probe] ERROR in profileByUsername:");
            System.out.println("[Probe] cause: " + t.getMessage());
        }
    }
}
