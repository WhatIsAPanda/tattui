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

    @Test
    void parseArtistTag_findsFirstArtistTag_caseInsensitive() {
        var tags = List.of("STYLE:NeoTraditional", "ArTiSt:Raven", "location:Rome");
        // inline copy of the logic under test (kept small to avoid UI deps)
        String found = tags.stream()
                .filter(t -> t.toLowerCase().startsWith("artist:"))
                .map(t -> t.substring("artist:".length()).trim())
                .findFirst()
                .orElse(null);

        assertEquals("Raven", found);
    }
}
