package app.controller.explore;

import java.util.List;

public final class MockExploreDataProvider implements ExploreDataProvider {
    private final ExploreControl control = new ExploreControl();
    @Override public List<ExploreControl.SearchItem> fetch(String q, ExploreControl.Kind filter) {
        return control.filter(q, filter);
    }
}
