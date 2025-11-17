package app.controller.explore;

import app.entity.Profile;
import app.entity.PostWithAuthor;
import app.entity.jdbc.JdbcPostRepository;
import app.entity.PostRepository;
import app.entity.DatabaseConnector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Live Explore provider that merges:
 *  - Artists from DatabaseConnector (read-only)
 *  - Posts from JdbcPostRepository (read-only)
 *
 * NOTE: We intentionally do NOT modify DatabaseConnector.
 * We clean up data (e.g., de-dup styles, thumbnail fallback) here in the provider.
 */
public final class MergedExploreDataProvider implements ExploreDataProvider {

    private final PostRepository posts = new JdbcPostRepository();

    @Override
    public List<ExploreControl.SearchItem> fetch(String q, ExploreControl.Kind filter) {
        String needle = (q == null ? "" : q).trim();
        List<ExploreControl.SearchItem> out = new ArrayList<>();

        // -------- ARTISTS (from DatabaseConnector) --------
        if (filter == ExploreControl.Kind.ARTISTS || filter == ExploreControl.Kind.ALL) {
            try {
                List<Profile> profiles = DatabaseConnector.getProfilesLike(needle);
                for (Profile p : profiles) {
                    // De-dup styles WITHOUT changing DatabaseConnector
                    List<String> tags = (p.getStylesList() == null)
                            ? List.of()
                            : new ArrayList<>(new LinkedHashSet<>(
                            p.getStylesList().stream()
                                    .filter(s -> s != null && !s.isBlank())
                                    .toList()
                    ));

                    // Friendly thumbnail fallback (do NOT write to DB)
                    String thumb = p.getProfilePictureURL();
                    if (thumb == null || thumb.isBlank()) {
                        thumb = "/icons/artist_raven.jpg"; // or your own placeholder
                    }

                    out.add(new ExploreControl.SearchItem(
                            p.getUsername(),
                            ExploreControl.Kind.ARTISTS,
                            thumb,                                      // http(s) or resource path; ExploreBoundary handles both
                            tags,
                            p.getBiography() == null ? "" : p.getBiography()
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // -------- COMPLETED TATTOOS (posts) --------
        if (filter == ExploreControl.Kind.COMPLETED_TATTOOS
                || filter == ExploreControl.Kind.ALL
            // Designs has no backing source yet; showing posts under Designs is optional. Keep it disabled:
            // || filter == ExploreControl.Kind.DESIGNS
        ) {
            try {
                List<PostWithAuthor> rows = needle.isEmpty()
                        ? posts.findLatest(60, 0)
                        : posts.search(needle, 60, 0);
                for (PostWithAuthor row : rows) {
                    var post = row.post();
                    var author = row.author();
                    String title = (author.getUsername() == null ? "unknown" : author.getUsername())
                            + " — " + (post.getCaption() == null || post.getCaption().isBlank()
                            ? "(untitled)"
                            : post.getCaption());

                    out.add(new ExploreControl.SearchItem(
                            title,
                            ExploreControl.Kind.COMPLETED_TATTOOS,       // posts appear under Completed Tattoos
                            post.getPostURL(),                           // Cloudinary URL (http/https)
                            List.of("artist:" + (author.getUsername() == null ? "unknown" : author.getUsername()),
                                    "completed"),
                            post.getCaption() == null ? "" : post.getCaption()
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // -------- DESIGNS (derived from posts; heuristic) --------
        if (filter == ExploreControl.Kind.DESIGNS || filter == ExploreControl.Kind.ALL) {
            try {
                List<PostWithAuthor> rows = needle.isEmpty()
                        ? posts.findLatest(60, 0)
                        : posts.search(needle, 60, 0);

                // Pick posts that look like designs: caption contains "design" or "sketch"
                List<PostWithAuthor> designish = rows.stream()
                        .filter(r -> {
                            String c = r.post().getCaption();
                            if (c == null) return false;
                            String lc = c.toLowerCase();
                            return lc.contains("design") || lc.contains("sketch");
                        })
                        .toList();

                // If none matched, just take the first 4 as demo designs so the UI actions are available
                List<PostWithAuthor> source = designish.isEmpty()
                        ? rows.stream().limit(4).toList()
                        : designish;

                for (PostWithAuthor row : source) {
                    var post = row.post();
                    var author = row.author();

                    out.add(new ExploreControl.SearchItem(
                            (post.getCaption() == null || post.getCaption().isBlank())
                                    ? "Design"
                                    : post.getCaption(),
                            ExploreControl.Kind.DESIGNS,
                            post.getPostURL(), // URL; ExploreBoundary already supports Save/Send for DESIGNS
                            List.of("design", "artist:" + (author.getUsername() == null ? "unknown" : author.getUsername())),
                            post.getCaption() == null ? "" : post.getCaption()
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }


        return out;
    }
}
