package app;

import de.javagl.obj.FloatTuple;
import de.javagl.obj.Obj;
import de.javagl.obj.ObjFace;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjUtils;
import de.javagl.obj.ReadableObj;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class for loading OBJ models via the JavaGL OBJ library and converting them to JavaFX meshes.
 */
public final class ObjLoader {
    private static final Color DEFAULT_DIFFUSE = Color.web("#f2d0b4");

    private ObjLoader() {
    }

    public record ModelPart(String name, Node node) {
    }

    public record LoadedModel(List<ModelPart> parts, boolean requiresZUpCorrection, boolean requiresYAxisFlip) {
        public LoadedModel {
            parts = List.copyOf(parts);
        }
    }

    public static LoadedModel load(Path objPath) throws IOException {
        Objects.requireNonNull(objPath, "objPath");
        if (!Files.exists(objPath)) {
            throw new IOException("OBJ file not found: " + objPath);
        }

        Obj obj = readObj(objPath);
        ReadableObj renderable = ObjUtils.convertToRenderable(obj);
        PhongMaterial baseMaterial = buildMaterial(objPath);
        List<ModelPart> parts = buildParts(renderable, baseMaterial);
        if (parts.isEmpty()) {
            throw new IOException("OBJ contained no faces: " + objPath);
        }
        return new LoadedModel(parts, isLikelyZUp(obj), true);
    }

    private static Obj readObj(Path objPath) throws IOException {
        try (InputStream stream = Files.newInputStream(objPath)) {
            return ObjReader.read(stream);
        }
    }

    private static PhongMaterial buildMaterial(Path objPath) throws IOException {
        PhongMaterial material = new PhongMaterial(DEFAULT_DIFFUSE);
        findMaterialInfo(objPath).ifPresent(info -> {
            info.diffuseColor().ifPresent(material::setDiffuseColor);
            info.diffuseMap().ifPresent(material::setDiffuseMap);
            material.setSpecularColor(Color.web("#d8d8d8"));
            material.setSpecularPower(32);
        });
        return material;
    }

    private static Optional<MaterialInfo> findMaterialInfo(Path objPath) throws IOException {
        List<String> mtllibs = extractMtllibStatements(objPath);
        for (String mtlName : mtllibs) {
            Path mtlPath = resolveSibling(objPath, mtlName);
            if (!Files.exists(mtlPath)) {
                continue;
            }
            Optional<MaterialInfo> info = parseMtl(mtlPath);
            if (info.isPresent()) {
                return info;
            }
        }
        return Optional.empty();
    }

    private static List<String> extractMtllibStatements(Path objPath) throws IOException {
        List<String> mtllibs = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(objPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("mtllib")) {
                    String[] tokens = line.split("\\s+", 2);
                    if (tokens.length > 1) {
                        mtllibs.add(tokens[1].trim());
                    }
                }
            }
        }
        return mtllibs;
    }

    private static Optional<MaterialInfo> parseMtl(Path mtlPath) throws IOException {
        Color kdColor = null;
        Image mapImage = null;

        try (BufferedReader reader = Files.newBufferedReader(mtlPath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("kd") && kdColor == null) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 4) {
                        try {
                            double r = clamp01(Double.parseDouble(tokens[1]));
                            double g = clamp01(Double.parseDouble(tokens[2]));
                            double b = clamp01(Double.parseDouble(tokens[3]));
                            kdColor = Color.color(r, g, b);
                        } catch (NumberFormatException ignored) {
                            // fall back to default
                        }
                    }
                } else if (lower.startsWith("map_kd") && mapImage == null) {
                    String[] tokens = line.split("\\s+", 2);
                    if (tokens.length > 1) {
                        Path texturePath = resolveSibling(mtlPath, tokens[1].trim());
                        if (Files.exists(texturePath)) {
                            mapImage = new Image(texturePath.toUri().toString());
                        }
                    }
                }
                if (kdColor != null && mapImage != null) {
                    break;
                }
            }
        }

        return Optional.of(new MaterialInfo(Optional.ofNullable(kdColor), Optional.ofNullable(mapImage)));
    }

    private static Path resolveSibling(Path reference, String relative) {
        Path baseDir = reference.getParent();
        if (baseDir == null) {
            return Paths.get(relative);
        }
        return baseDir.resolve(relative).normalize();
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static List<ModelPart> buildParts(ReadableObj obj, PhongMaterial material) {
        Map<String, List<Integer>> facesByMaterial = new LinkedHashMap<>();
        int faceCount = obj.getNumFaces();
        for (int i = 0; i < faceCount; i++) {
            ObjFace face = obj.getFace(i);
            String bucket = "default";
            facesByMaterial.computeIfAbsent(bucket, key -> new ArrayList<>()).add(i);
        }

        if (facesByMaterial.isEmpty() && faceCount > 0) {
            List<Integer> allFaces = new ArrayList<>(faceCount);
            for (int i = 0; i < faceCount; i++) {
                allFaces.add(i);
            }
            facesByMaterial.put("default", allFaces);
        }

        List<ModelPart> parts = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : facesByMaterial.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.isEmpty()) {
                continue;
            }
            MeshView meshView = createMeshView(obj, indices, material);
            if (meshView != null) {
                parts.add(new ModelPart(entry.getKey(), meshView));
            }
        }
        return parts;
    }

    private static MeshView createMeshView(ReadableObj obj, List<Integer> faceIndices, PhongMaterial material) {
        if (faceIndices.isEmpty()) {
            return null;
        }

        TriangleMesh mesh = new TriangleMesh();
        Map<String, Integer> vertexMap = new LinkedHashMap<>();
        List<Float> points = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        for (Integer faceIdx : faceIndices) {
            ObjFace face = obj.getFace(faceIdx);
            if (face.getNumVertices() != 3) {
                continue;
            }

            for (int v = 0; v < 3; v++) {
                int vertexIndex = face.getVertexIndex(v);
                int texIndex = face.containsTexCoordIndices() ? face.getTexCoordIndex(v) : -1;
                if (texIndex < 0) {
                    texIndex = -1;
                }

                String key = vertexIndex + "/" + texIndex;
                Integer mappedIndex = vertexMap.get(key);
                if (mappedIndex == null) {
                    mappedIndex = vertexMap.size();
                    vertexMap.put(key, mappedIndex);

                    FloatTuple vertex = obj.getVertex(vertexIndex);
                    points.add(vertex.getX());
                    points.add(vertex.getY());
                    points.add(vertex.getZ());

                    if (texIndex >= 0) {
                        FloatTuple tex = obj.getTexCoord(texIndex);
                        float u = tex.getX();
                        float vv = 1f - tex.getY();
                        texCoords.add(u);
                        texCoords.add(vv);
                    } else {
                        texCoords.add(0f);
                        texCoords.add(0f);
                    }
                }

                faces.add(mappedIndex);
                faces.add(mappedIndex);
            }
        }

        if (points.isEmpty()) {
            return null;
        }

        mesh.getPoints().setAll(toFloatArray(points));
        mesh.getTexCoords().setAll(toFloatArray(texCoords));
        mesh.getFaces().setAll(toIntArray(faces));

        int faceTotal = faces.size() / 6;
        int[] smoothingGroups = new int[faceTotal];
        for (int i = 0; i < faceTotal; i++) {
            smoothingGroups[i] = 1;
        }
        mesh.getFaceSmoothingGroups().setAll(smoothingGroups);

        MeshView meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.BACK);
        meshView.setMaterial(material);
        return meshView;
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] data = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            data[i] = values.get(i);
        }
        return data;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] data = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            data[i] = values.get(i);
        }
        return data;
    }

    private static boolean isLikelyZUp(ReadableObj obj) {
        int vertexCount = obj.getNumVertices();
        if (vertexCount == 0) {
            return false;
        }

        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < vertexCount; i++) {
            FloatTuple vertex = obj.getVertex(i);
            float y = vertex.getY();
            float z = vertex.getZ();
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        }

        float spanY = maxY - minY;
        float spanZ = maxZ - minZ;

        if (spanY <= 0f) {
            return spanZ > 0f;
        }

        return spanZ > spanY * 1.2f;
    }

    private record MaterialInfo(Optional<Color> diffuseColor, Optional<Image> diffuseMap) {
    }
}
