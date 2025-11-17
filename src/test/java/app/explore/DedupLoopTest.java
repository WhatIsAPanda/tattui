package app.explore;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class DedupLoopTest {

    @Test
    void linkedHashSet_dedupsAndPreservesOrder() {
        List<String> in = List.of("style1", "style1", "style2", "style1", "style2", "style3");
        List<String> out = new ArrayList<>(new LinkedHashSet<>(in));
        assertEquals(List.of("style1", "style2", "style3"), out);
    }
}
