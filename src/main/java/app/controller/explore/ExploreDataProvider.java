package app.controller.explore;

import java.util.List;

public interface ExploreDataProvider {
    List<ExploreControl.SearchItem> fetch(String q, ExploreControl.Kind filter);
}
