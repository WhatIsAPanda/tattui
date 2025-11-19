package app.controller.explore;

import app.entity.PostRepository;
import app.entity.PostWithAuthor;
import app.entity.jdbc.JdbcPostRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JdbcExploreDataProvider implements ExploreDataProvider {
    private final PostRepository repo = new JdbcPostRepository();

    @Override
    public List<ExploreControl.SearchItem> fetch(String q, ExploreControl.Kind filter) {
        try {
            List<PostWithAuthor> rows = (q == null || q.isBlank())
                    ? repo.findLatest(30, 0)
                    : repo.search(q.toLowerCase(Locale.ROOT), 30, 0);

            List<ExploreControl.SearchItem> items = new ArrayList<>();
            for (var row : rows) {
                var post = row.post();
                var author = row.author();

                String title = author.getUsername() + " — " + (post.getCaption() == null ? "(untitled)" : post.getCaption());
                var tags = List.of("artist:" + author.getUsername(), "completed");

                items.add(new ExploreControl.SearchItem(
                        title,
                        ExploreControl.Kind.COMPLETED_TATTOOS,           // adjust if you add a type flag later
                        post.getPostURL(),                                // Cloudinary URL
                        tags,
                        post.getCaption() == null ? "" : post.getCaption()
                ));
            }
            return items.stream().filter(it -> filter == ExploreControl.Kind.ALL || it.kind() == filter).toList();
        } catch (SQLException _) {
            return List.of();
        }
    }
}
