package app.controller.explore;

import app.entity.Profile;
import app.entity.PostWithAuthor;
import app.entity.DesignWithAuthor;
import app.entity.jdbc.JdbcDesignRepository;
import app.entity.jdbc.JdbcPostRepository;
import app.entity.DesignRepository;
import app.entity.PostRepository;
import app.entity.DatabaseConnector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Live Explore provider that merges:
 * - Artists from DatabaseConnector (read-only)
 * - Posts from JdbcPostRepository (read-only)
 *
 * NOTE: We intentionally do NOT modify DatabaseConnector.
 * We clean up data (e.g., de-dup styles, thumbnail fallback) here in the
 * provider.
 */
public final class MergedExploreDataProvider implements ExploreDataProvider {

    private final PostRepository posts = new JdbcPostRepository();
    private final DesignRepository designs = new JdbcDesignRepository();
    private static final int FETCH_LIMIT = 60;
    private static final String DEFAULT_THUMB = "/icons/artist_raven.jpg";
    private static final String UNKNOW_STRING = "unknown";

    @Override
    public List<ExploreControl.SearchItem> fetch(String q, ExploreControl.Kind filter) {
        String needle = (q == null ? "" : q).trim();
        List<ExploreControl.SearchItem> out = new ArrayList<>();
        List<PostWithAuthor> completedRows = handlesCompleted(filter) ? queryPosts(needle) : List.of();
        List<DesignWithAuthor> designRows = handlesDesigns(filter) ? queryDesigns(needle) : List.of();

        if (handlesArtists(filter)) {
            out.addAll(fetchArtists(needle));
        }
        if (handlesCompleted(filter)) {
            out.addAll(fetchCompletedPosts(completedRows));
        }
        if (handlesDesigns(filter)) {
            out.addAll(fetchDesigns(designRows));
        }

        return out;
    }

    private boolean handlesArtists(ExploreControl.Kind filter) {
        return filter == ExploreControl.Kind.ARTISTS || filter == ExploreControl.Kind.ALL;
    }

    private boolean handlesCompleted(ExploreControl.Kind filter) {
        return filter == ExploreControl.Kind.COMPLETED_TATTOOS || filter == ExploreControl.Kind.ALL;
    }

    private boolean handlesDesigns(ExploreControl.Kind filter) {
        return filter == ExploreControl.Kind.DESIGNS || filter == ExploreControl.Kind.ALL;
    }

    private List<ExploreControl.SearchItem> fetchArtists(String needle) {
        List<ExploreControl.SearchItem> items = new ArrayList<>();
        try {
            List<Profile> profiles = DatabaseConnector.getProfilesLike(needle);
            for (Profile p : profiles) {
                items.add(new ExploreControl.SearchItem(
                        p.getUsername(),
                        ExploreControl.Kind.ARTISTS,
                        resolveThumbnail(p),
                        dedupeTags(p.getStylesList()),
                        p.getBiography() == null ? "" : p.getBiography()));
            }
        } catch (SQLException _) {
            // Leave list empty when DB lookups fail.
        }
        return items;
    }

    private String resolveThumbnail(Profile p) {
        String thumb = p.getProfilePictureURL();
        return (thumb == null || thumb.isBlank()) ? DEFAULT_THUMB : thumb;
    }

    private List<String> dedupeTags(List<String> styles) {
        if (styles == null) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(
                styles.stream().filter(s -> s != null && !s.isBlank()).toList()));
    }

    private List<ExploreControl.SearchItem> fetchCompletedPosts(List<PostWithAuthor> rows) {
        List<ExploreControl.SearchItem> items = new ArrayList<>();
        for (PostWithAuthor row : rows) {
            var post = row.post();
            var author = row.author();
            String authorName = author.getUsername() == null ? UNKNOW_STRING : author.getUsername();
            String caption = post.getCaption();
            String title = authorName + " — " + (caption == null || caption.isBlank() ? "(untitled)" : caption);

            items.add(new ExploreControl.SearchItem(
                    title,
                    ExploreControl.Kind.COMPLETED_TATTOOS,
                    post.getPostURL(),
                    List.of("artist:" + authorName, "completed"),
                    caption == null ? "" : caption));
        }
        return items;
    }

    private List<PostWithAuthor> queryPosts(String needle) {
        try {
            return needle.isEmpty()
                    ? posts.findLatest(FETCH_LIMIT, 0)
                    : posts.search(needle, FETCH_LIMIT, 0);
        } catch (SQLException _) {
            return List.of();
        }
    }

    private List<ExploreControl.SearchItem> fetchDesigns(List<DesignWithAuthor> rows) {
        List<ExploreControl.SearchItem> items = new ArrayList<>();
        for (DesignWithAuthor row : rows) {
            var design = row.design();
            var author = row.author();
            String authorName = author.getUsername() == null ? UNKNOW_STRING : author.getUsername();
            String title = (design.name() == null || design.name().isBlank())
                    ? "Design by " + authorName
                    : design.name();
            items.add(new ExploreControl.SearchItem(
                    title,
                    ExploreControl.Kind.DESIGNS,
                    resolveDesignThumbnail(design.pictureUrl()),
                    List.of("design",
                            "artist:" + authorName),
                    "Design by " + authorName));
        }
        return items;
    }

    private List<DesignWithAuthor> queryDesigns(String needle) {
        try {
            return needle.isEmpty()
                    ? designs.findLatest(FETCH_LIMIT, 0)
                    : designs.search(needle, FETCH_LIMIT, 0);
        } catch (SQLException _) {
            return List.of();
        }
    }

    private String resolveDesignThumbnail(String url) {
        return (url == null || url.isBlank()) ? DEFAULT_THUMB : url.trim();
    }
}
