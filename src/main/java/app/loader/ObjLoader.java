package app.loader;

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

    private ObjLoader() {}

    public record ModelPart(String name, Node node) {}

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
                String normalized = normalizeMaterialLine(line);
                if (normalized != null) {
                    if (kdColor == null) {
                        kdColor = tryParseDiffuseColor(normalized);
                    }
                    if (mapImage == null) {
                        mapImage = tryParseDiffuseMap(normalized, mtlPath);
                    }
                }
                if (!needsMoreMaterial(kdColor, mapImage)) {
                    break;
                }
            }
        }
        return Optional.of(new MaterialInfo(Optional.ofNullable(kdColor), Optional.ofNullable(mapImage)));
    }

    private static boolean needsMoreMaterial(Color color, Image map) {
        return color == null || map == null;
    }

    private static String normalizeMaterialLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        return trimmed;
    }

    private static Color tryParseDiffuseColor(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("kd")) {
            return null;
        }
        String[] tokens = line.split("\\s+");
        if (tokens.length < 4) {
            return null;
        }
        try {
            double r = clamp01(Double.parseDouble(tokens[1]));
            double g = clamp01(Double.parseDouble(tokens[2]));
            double b = clamp01(Double.parseDouble(tokens[3]));
            return Color.color(r, g, b);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static Image tryParseDiffuseMap(String line, Path mtlPath) {
        String lower = line.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("map_kd")) {
            return null;
        }
        String[] tokens = line.split("\\s+", 2);
        if (tokens.length <= 1) {
            return null;
        }
        Path texturePath = resolveSibling(mtlPath, tokens[1].trim());
        if (!Files.exists(texturePath)) {
            return null;
        }
        return new Image(texturePath.toUri().toString());
    }

    private static Path resolveSibling(Path reference, String relative) {
        Path baseDir = reference.getParent();
        if (baseDir == null) {
            return Paths.get(relative);
        }
        return baseDir.resolve(relative).normalize();
    }

    private static double clamp01(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    private static List<ModelPart> buildParts(ReadableObj obj, PhongMaterial material) {
        Map<String, List<Integer>> facesByMaterial = new LinkedHashMap<>();
        int faceCount = obj.getNumFaces();
        for (int i = 0; i < faceCount; i++) {
            facesByMaterial.computeIfAbsent("default", key -> new ArrayList<>()).add(i);
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

    private static MeshView createMeshView(ReadableObj obj, List<Integer> faceIndices, PhongMaterial baseMaterial) {
        if (faceIndices.isEmpty()) {
            return null;
        }
        TriangleMesh mesh = new TriangleMesh();
        List<float[]> vertices = collectVertices(obj, faceIndices);
        float[] points = flatten(vertices);
        mesh.getPoints().setAll(points);
        List<float[]> texCoords = collectTexCoords(obj, faceIndices);
        if (texCoords.isEmpty()) {
            mesh.getTexCoords().setAll(0.0f, 0.0f);
        } else {
            mesh.getTexCoords().setAll(flatten(texCoords));
        }
        int[] faces = buildFaces(obj, faceIndices);
        mesh.getFaces().setAll(faces);

        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.BACK);
        view.setMaterial(baseMaterial);
        return view;
    }

    private static List<float[]> collectVertices(ReadableObj obj, List<Integer> faceIndices) {
        List<float[]> vertices = new ArrayList<>();
        for (Integer faceIndex : faceIndices) {
            ObjFace face = obj.getFace(faceIndex);
            int numVertices = face.getNumVertices();
            for (int i = 0; i < numVertices; i++) {
                FloatTuple vertex = obj.getVertex(face.getVertexIndex(i));
                vertices.add(new float[]{vertex.getX(), vertex.getY(), vertex.getZ()});
            }
        }
        return vertices;
    }

    private static List<float[]> collectTexCoords(ReadableObj obj, List<Integer> faceIndices) {
        List<float[]> tex = new ArrayList<>();
        for (Integer faceIndex : faceIndices) {
            ObjFace face = obj.getFace(faceIndex);
            int numVertices = face.getNumVertices();
            for (int i = 0; i < numVertices; i++) {
                int texIndex = face.getTexCoordIndex(i);
                if (texIndex >= 0 && texIndex < obj.getNumTexCoords()) {
                    FloatTuple texCoord = obj.getTexCoord(texIndex);
                    tex.add(new float[]{texCoord.getX(), 1f - texCoord.getY()});
                } else {
                    tex.add(new float[]{0f, 0f});
                }
            }
        }
        return tex;
    }

    private static int[] buildFaces(ReadableObj obj, List<Integer> faceIndices) {
        List<Integer> faces = new ArrayList<>();
        int vertexOffset = 0;
        int texOffset = 0;
        for (Integer faceIndex : faceIndices) {
            ObjFace face = obj.getFace(faceIndex);
            int numVertices = face.getNumVertices();
            if (numVertices < 3) {
                continue;
            }
            for (int i = 1; i + 1 < numVertices; i++) {
                int v0 = vertexOffset;
                int v1 = vertexOffset + i;
                int v2 = vertexOffset + i + 1;
                int t0 = texOffset;
                int t1 = texOffset + i;
                int t2 = texOffset + i + 1;
                faces.add(v0);
                faces.add(t0);
                faces.add(v1);
                faces.add(t1);
                faces.add(v2);
                faces.add(t2);
            }
            vertexOffset += numVertices;
            texOffset += numVertices;
        }
        int[] array = new int[faces.size()];
        for (int i = 0; i < faces.size(); i++) {
            array[i] = faces.get(i);
        }
        return array;
    }

    private static float[] flatten(List<float[]> data) {
        float[] flat = new float[data.size() * data.get(0).length];
        int index = 0;
        for (float[] tuple : data) {
            for (float value : tuple) {
                flat[index++] = value;
            }
        }
        return flat;
    }

    private static boolean isLikelyZUp(Obj obj) {
        int total = obj.getNumFaces();
        int zUpCount = 0;
        for (int i = 0; i < total; i++) {
            ObjFace face = obj.getFace(i);
            if (face.getNumVertices() < 3) {
                continue;
            }
            FloatTuple a = obj.getVertex(face.getVertexIndex(0));
            FloatTuple b = obj.getVertex(face.getVertexIndex(1));
            FloatTuple c = obj.getVertex(face.getVertexIndex(2));
            double abx = b.getX() - a.getX();
            double aby = b.getY() - a.getY();
            double abz = b.getZ() - a.getZ();
            double acx = c.getX() - a.getX();
            double acy = c.getY() - a.getY();
            double acz = c.getZ() - a.getZ();
            double nx = aby * acz - abz * acy;
            double ny = abz * acx - abx * acz;
            double nz = abx * acy - aby * acx;
            if (Math.abs(nz) > Math.abs(ny) && Math.abs(nz) > Math.abs(nx)) {
                zUpCount++;
            }
        }
        return zUpCount > total * 0.6;
    }

    private record MaterialInfo(Optional<Color> diffuseColor, Optional<Image> diffuseMap) {}
}
