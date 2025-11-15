package app.controller.explore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ExploreControl {

    public enum Kind { ALL, ARTISTS, DESIGNS, COMPLETED_TATTOOS }

    public record SearchItem(String title, Kind kind, String thumbnail, List<String> tags, String hoverText) {}

    private final List<SearchItem> allItems = mockData();

    /** Filter logic with no JavaFX dependencies. */
    public List<SearchItem> filter(String q, Kind filter) {
        String needle = (q == null ? "" : q).trim().toLowerCase(Locale.ROOT);
        return allItems.stream()
                .filter(it -> filter == Kind.ALL || it.kind() == filter)
                .filter(it -> needle.isEmpty()
                        || it.title().toLowerCase(Locale.ROOT).contains(needle)
                        || it.tags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
    }

    private List<SearchItem> mockData() {
        List<SearchItem> list = new ArrayList<>();

        // Artist(s)
        list.add(new SearchItem(
                "Raven — abstract blackwork",
                Kind.ARTISTS,
                "/icons/artist_raven.jpg",
                List.of("blackwork", "abstract", "linework"),
                "Raven is a punk-studio artist known for bold blackwork and geometric abstractions."
        ));

        // Completed tattoos
        list.add(new SearchItem(
                "Dragon forearm (completed)",
                Kind.COMPLETED_TATTOOS,
                "/icons/completed_dragon_forearm.jpg",
                List.of("forearm", "dragon", "completed"),
                "Healed forearm piece with a stylized dragon—placed in the 3D workspace."
        ));
        list.add(new SearchItem(
                "Koi sleeve segment (completed)",
                Kind.COMPLETED_TATTOOS,
                "/icons/completed_koi_leg.jpg",
                List.of("leg", "koi", "color", "completed"),
                "Color koi segment wrapped on a darker skin tone for realistic preview."
        ));

        // Designs
        list.add(new SearchItem("Koi design", Kind.DESIGNS, "/icons/design_koi.png",
                List.of("koi", "color", "japanese"), "Vibrant koi with rainbow scales."));
        list.add(new SearchItem("Mandala", Kind.DESIGNS, "/icons/design_mandala.png",
                List.of("mandala", "ornamental"), "Symmetrical mandala—great for sternum or back."));
        list.add(new SearchItem("Floral cluster", Kind.DESIGNS, "/icons/design_flowers.png",
                List.of("flowers", "botanical"), "Bold, high-contrast floral cluster."));
        list.add(new SearchItem("Simple dragon", Kind.DESIGNS, "/icons/design_simple_dragon.png",
                List.of("dragon", "blackwork"), "Minimal blackwork dragon motif."));
        list.add(new SearchItem("Phoenix sketch", Kind.DESIGNS, "/icons/design_phoenix.png",
                List.of("phoenix", "mythical"), "Rising phoenix sketch—dynamic flow."));
        list.add(new SearchItem("Sugar skull + rose", Kind.DESIGNS, "/icons/design_skull_rose.png",
                List.of("skull", "neo traditional"), "Sugar-skull with floral accents."));
        list.add(new SearchItem("Playful snake", Kind.DESIGNS, "/icons/design_snake.png",
                List.of("snake", "fun"), "Cartoon snake—good for forearm or calf."));

        return list;
    }
}
