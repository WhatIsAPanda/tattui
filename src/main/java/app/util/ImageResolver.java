package app.util;

import javafx.scene.image.Image;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Central place for resolving image paths.
 * <p>
 * Supports http(s) URLs, classpath resources, {@code file:} URLs, Windows/Unix
 * paths,
 * {@code ~/}-relative paths, and hostnames without schemes. Backslashes are
 * normalized
 * so strings such as {@code src\main\resources\...} work on every platform.
 */
public final class ImageResolver {

    private ImageResolver() {
    }

    public static Image load(String candidate) {
        return load(0, 0, true, true, false, candidate);
    }

    public static Image load(String candidate, boolean backgroundLoading) {
        return load(0, 0, true, true, backgroundLoading, candidate);
    }

    public static Image load(String candidate, double width, double height, boolean preserveRatio, boolean smooth) {
        return load(width, height, preserveRatio, smooth, false, candidate);
    }

    public static Image load(String candidate, double width, double height,
            boolean preserveRatio, boolean smooth, boolean backgroundLoading) {
        return load(width, height, preserveRatio, smooth, backgroundLoading, candidate);
    }

    public static Image loadAny(String... candidates) {
        return load(0, 0, true, true, false, candidates);
    }

    public static Image loadAny(double width, double height,
            boolean preserveRatio, boolean smooth, boolean backgroundLoading,
            String... candidates) {
        return load(width, height, preserveRatio, smooth, backgroundLoading, candidates);
    }

    private static Image load(double width, double height,
            boolean preserveRatio, boolean smooth, boolean backgroundLoading,
            String... candidates) {
        String resolved = resolveAny(candidates);
        if (resolved == null) {
            throw new IllegalArgumentException("Unable to resolve image from provided sources.");
        }
        return new Image(resolved, width, height, preserveRatio, smooth, backgroundLoading);
    }

    public static String resolveAny(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            String resolved = resolve(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    public static String resolve(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            return normalized;
        }

        Path filePath = resolveFilePath(normalized);
        if (filePath != null) {
            return filePath.toUri().toString();
        }

        URL resource = findResource(normalized);
        if (resource != null) {
            return resource.toExternalForm();
        }

        if (looksLikeHost(normalized)) {
            return "https://" + normalized;
        }

        return null;
    }

    private static Path resolveFilePath(String normalized) {
        try {
            if (normalized.startsWith("~/")) {
                Path home = Paths.get(System.getProperty("user.home", ""));
                if (home != null) {
                    Path candidate = home.resolve(normalized.substring(2));
                    if (Files.exists(candidate)) {
                        return candidate.toAbsolutePath().normalize();
                    }
                }
            }

            Path path = Paths.get(normalized);
            if (Files.exists(path)) {
                return path.toAbsolutePath().normalize();
            }

            if (!path.isAbsolute()) {
                Path base = Paths.get(System.getProperty("user.dir", ""));
                Path candidate = base.resolve(path).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            }
        } catch (InvalidPathException ignored) {
            // Ignore invalid paths
        }
        return null;
    }

    private static URL findResource(String normalized) {
        URL url = ImageResolver.class.getResource(normalized);
        if (url == null && !normalized.startsWith("/")) {
            url = ImageResolver.class.getResource("/" + normalized);
        }
        return url;
    }

    private static boolean looksLikeHost(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || trimmed.startsWith("/")
                || trimmed.contains("://")
                || trimmed.contains(" ")) {
            return false;
        }
        if (!trimmed.contains(".")) {
            return false;
        }
        return !looksLikeWindowsDrive(trimmed);
    }

    private static boolean looksLikeWindowsDrive(String value) {
        return value.length() > 2
                && Character.isLetter(value.charAt(0))
                && value.charAt(1) == ':';
    }
}
