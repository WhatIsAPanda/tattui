package app.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.shape.TriangleMesh;

/**
 * Performs proportion manipulation on a character model made of triangle meshes.
 */
public final class ModelManipulator {
    private final List<TriangleMesh> meshes;
    private final List<float[]> originalPoints;
    private final List<float[]> workingBuffers;

    private final float minX;
    private final float maxX;
    private final float minY;
    private final float maxY;
    private final float minZ;
    private final float maxZ;
    private final float centerX;
    private final float centerZ;

    private final float headThreshold;
    private final float shoulderLevel;
    private final float torsoBottom;
    private final float hipLevel;
    private final float kneeLevel;

    private final float torsoSpan;
    private final float armSpan;
    private final float legSpan;
    private final float armThreshold;

    private final float headCenterX;
    private final float headCenterZ;
    private final float[] leftArmCenter = new float[3];
    private final float[] rightArmCenter = new float[3];
    private final float[] leftLegCenter = new float[3];
    private final float[] rightLegCenter = new float[3];

    private final float armMinY;
    private final float legMinY;
    private final float shoulderRadius;
    private final float hipRadius;
    private final boolean hasHead;

    public ModelManipulator(List<TriangleMesh> meshes) {
        this.meshes = new ArrayList<>(meshes);
        this.originalPoints = new ArrayList<>(meshes.size());
        this.workingBuffers = new ArrayList<>(meshes.size());

        BoundsData bounds = captureMeshData();
        this.minX = bounds.minX();
        this.maxX = bounds.maxX();
        this.minY = bounds.minY();
        this.maxY = bounds.maxY();
        this.minZ = bounds.minZ();
        this.maxZ = bounds.maxZ();
        this.centerX = (minX + maxX) * 0.5f;
        this.centerZ = (minZ + maxZ) * 0.5f;

        Thresholds thresholds = computeThresholds(bounds.sortedY());
        this.headThreshold = thresholds.headThreshold();
        this.shoulderLevel = thresholds.shoulderLevel();
        this.torsoBottom = thresholds.torsoBottom();
        this.hipLevel = thresholds.hipLevel();
        this.kneeLevel = thresholds.kneeLevel();

        float height = Math.max(maxY - minY, 1f);
        this.shoulderRadius = Math.max(height * 0.04f, 1f);
        this.hipRadius = Math.max(height * 0.05f, 1f);
        this.armThreshold = Math.max((maxX - minX) * 0.35f, 1f);

        LimbData limbData = computeLimbData(thresholds);
        this.hasHead = limbData.hasHead();
        this.headCenterX = limbData.headCenterX();
        this.headCenterZ = limbData.headCenterZ();
        copyCenter(limbData.leftArmCenter(), leftArmCenter);
        copyCenter(limbData.rightArmCenter(), rightArmCenter);
        copyCenter(limbData.leftLegCenter(), leftLegCenter);
        copyCenter(limbData.rightLegCenter(), rightLegCenter);

        this.armMinY = limbData.armMinY();
        this.legMinY = limbData.legMinY();
        this.torsoSpan = Math.max(shoulderLevel - torsoBottom, 1f);
        this.armSpan = Math.max(shoulderLevel - armMinY, 1f);
        this.legSpan = Math.max(hipLevel - legMinY, 1f);
    }

    public void apply(Map<String, Double> params) {
        if (meshes.isEmpty()) {
            return;
        }

        ProportionSettings settings = ProportionSettings.from(params);
        for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
            transformMesh(meshIndex, settings);
        }
    }

    private void transformMesh(int meshIndex, ProportionSettings settings) {
        TriangleMesh mesh = meshes.get(meshIndex);
        float[] source = originalPoints.get(meshIndex);
        float[] working = workingBuffers.get(meshIndex);
        System.arraycopy(source, 0, working, 0, source.length);

        for (int i = 0; i < working.length; i += 3) {
            adjustVertex(source, working, i, settings);
        }

        mesh.getPoints().setAll(working);
    }

    private void adjustVertex(float[] source, float[] working, int index, ProportionSettings settings) {
        VertexPosition position = new VertexPosition(source[index], source[index + 1], source[index + 2]);
        RegionFlags region = classifyRegion(position.x, position.y);
        applyTorsoTransform(position, region.torsoZone(), settings);
        if (region.arm()) {
            float[] armCenter = region.left() ? leftArmCenter : rightArmCenter;
            applyArmTransform(position, armCenter, settings);
        }
        if (region.leg()) {
            float[] legCenter = region.left() ? leftLegCenter : rightLegCenter;
            applyLegTransform(position, legCenter, settings);
        }
        working[index] = position.x;
        working[index + 1] = position.y;
        working[index + 2] = position.z;
    }

    private void applyTorsoTransform(VertexPosition position, TorsoZone zone, ProportionSettings settings) {
        switch (zone) {
            case HEAD -> {
                position.x = scaleAround(position.x, headCenterX, settings.headScale());
                position.z = scaleAround(position.z, headCenterZ, settings.headScale());
            }
            case UPPER -> position.x = scaleAround(position.x, centerX, settings.shoulderWidth());
            case LOWER -> {
                position.x = scaleAround(position.x, centerX, settings.torsoWidth());
                position.y = stretchAround(position.y, torsoBottom, torsoSpan, settings.torsoLength());
            }
            case PELVIS -> {
                position.x = scaleAround(position.x, centerX, settings.hipWidth());
                position.y = stretchAround(position.y, hipLevel, torsoSpan * 0.5f, settings.torsoLength());
            }
            case NONE -> {
                // Intentionally left blank: torso-neutral vertices require no adjustment.
            }
        }
    }

    private void applyArmTransform(VertexPosition position, float[] armCenter, ProportionSettings settings) {
        position.x = scaleAround(position.x, armCenter[0], settings.armThickness());
        position.z = scaleAround(position.z, armCenter[2], settings.armThickness());
        position.y = stretchAround(position.y, armCenter[1], armSpan, settings.armLength());
    }

    private void applyLegTransform(VertexPosition position, float[] legCenter, ProportionSettings settings) {
        position.x = scaleAround(position.x, legCenter[0], settings.legThickness());
        position.z = scaleAround(position.z, legCenter[2], settings.legThickness());
        position.y = stretchAround(position.y, legCenter[1], legSpan, settings.legLength());
    }

    private BoundsData captureMeshData() {
        float minX2 = Float.POSITIVE_INFINITY;
        float maxX2 = Float.NEGATIVE_INFINITY;
        float minY2 = Float.POSITIVE_INFINITY;
        float maxY2 = Float.NEGATIVE_INFINITY;
        float minZ2 = Float.POSITIVE_INFINITY;
        float maxZ2 = Float.NEGATIVE_INFINITY;
        List<Float> ySamples = new ArrayList<>();

        for (TriangleMesh mesh : this.meshes) {
            float[] source = new float[mesh.getPoints().size()];
            mesh.getPoints().toArray(source);
            originalPoints.add(source);
            workingBuffers.add(new float[source.length]);
            for (int i = 0; i < source.length; i += 3) {
                float x = source[i];
                float y = source[i + 1];
                float z = source[i + 2];
                minX2 = Math.min(minX2, x);
                maxX2 = Math.max(maxX2, x);
                minY2 = Math.min(minY2, y);
                maxY2 = Math.max(maxY2, y);
                minZ2 = Math.min(minZ2, z);
                maxZ2 = Math.max(maxZ2, z);
                ySamples.add(y);
            }
        }

        if (ySamples.isEmpty()) {
            minX2 = maxX2 = minY2 = maxY2 = minZ2 = maxZ2 = 0f;
        }

        return new BoundsData(minX2, maxX2, minY2, maxY2, minZ2, maxZ2, toSortedArray(ySamples));
    }

    private Thresholds computeThresholds(float[] sortedY) {
        return new Thresholds(
            quantile(sortedY, 0.88f),
            quantile(sortedY, 0.79f),
            quantile(sortedY, 0.42f),
            quantile(sortedY, 0.35f),
            quantile(sortedY, 0.20f)
        );
    }

    private LimbData computeLimbData(Thresholds thresholds) {
        LimbAccumulator accumulator = new LimbAccumulator(thresholds);
        for (float[] source : originalPoints) {
            accumulator.process(source);
        }
        return accumulator.build();
    }

    private final class LimbAccumulator {
        private final Thresholds thresholds;
        private float headSumX;
        private float headSumZ;
        private int headCount;

        private float leftArmSumX;
        private float leftArmSumY;
        private float leftArmSumZ;
        private int leftArmCount;

        private float rightArmSumX;
        private float rightArmSumY;
        private float rightArmSumZ;
        private int rightArmCount;

        private float leftLegSumX;
        private float leftLegSumY;
        private float leftLegSumZ;
        private int leftLegCount;

        private float rightLegSumX;
        private float rightLegSumY;
        private float rightLegSumZ;
        private int rightLegCount;

        private float armMinYSample = Float.POSITIVE_INFINITY;
        private float legMinYSample = Float.POSITIVE_INFINITY;

        LimbAccumulator(Thresholds thresholds) {
            this.thresholds = thresholds;
        }

        void process(float[] source) {
            for (int i = 0; i < source.length; i += 3) {
                processVertex(source[i], source[i + 1], source[i + 2]);
            }
        }

        private void processVertex(float x, float y, float z) {
            if (y >= thresholds.headThreshold()) {
                accumulateHead(x, z);
            }
            if (isArmCandidate(x, y)) {
                accumulateArm(x, y, z);
            }
            if (isLegCandidate(y)) {
                accumulateLeg(x, y, z);
            }
        }

        private void accumulateHead(float x, float z) {
            headSumX += x;
            headSumZ += z;
            headCount++;
        }

        private boolean isArmCandidate(float x, float y) {
            return Math.abs(x - centerX) > armThreshold && y > thresholds.kneeLevel();
        }

        private void accumulateArm(float x, float y, float z) {
            if (x < centerX) {
                leftArmSumX += x;
                leftArmSumY += y;
                leftArmSumZ += z;
                leftArmCount++;
            } else {
                rightArmSumX += x;
                rightArmSumY += y;
                rightArmSumZ += z;
                rightArmCount++;
            }
            armMinYSample = Math.min(armMinYSample, y);
        }

        private boolean isLegCandidate(float y) {
            return y <= thresholds.hipLevel();
        }

        private void accumulateLeg(float x, float y, float z) {
            if (x < centerX) {
                leftLegSumX += x;
                leftLegSumY += y;
                leftLegSumZ += z;
                leftLegCount++;
            } else {
                rightLegSumX += x;
                rightLegSumY += y;
                rightLegSumZ += z;
                rightLegCount++;
            }
            legMinYSample = Math.min(legMinYSample, y);
        }

        LimbData build() {
            boolean resolvedHasHead = headCount > 0;
            float resolvedHeadCenterX = resolvedHasHead ? headSumX / headCount : centerX;
            float resolvedHeadCenterZ = resolvedHasHead ? headSumZ / headCount : centerZ;

            float[] leftArmCenterResolved = buildCenter(
                leftArmCount, leftArmSumX, leftArmSumY, leftArmSumZ,
                centerX - armThreshold, thresholds.shoulderLevel(), centerZ
            );
            float[] rightArmCenterResolved = buildCenter(
                rightArmCount, rightArmSumX, rightArmSumY, rightArmSumZ,
                centerX + armThreshold, thresholds.shoulderLevel(), centerZ
            );

            float hipOffset = Math.max((maxX - minX) * 0.2f, 1f);
            float[] leftLegCenterResolved = buildCenter(
                leftLegCount, leftLegSumX, leftLegSumY, leftLegSumZ,
                centerX - hipOffset, thresholds.hipLevel(), centerZ
            );
            float[] rightLegCenterResolved = buildCenter(
                rightLegCount, rightLegSumX, rightLegSumY, rightLegSumZ,
                centerX + hipOffset, thresholds.hipLevel(), centerZ
            );

            float resolvedArmMinY = Float.isFinite(armMinYSample) ? armMinYSample : thresholds.shoulderLevel() - 150f;
            float resolvedLegMinY = Float.isFinite(legMinYSample) ? legMinYSample : minY;

            return new LimbData(
                resolvedHasHead,
                resolvedHeadCenterX,
                resolvedHeadCenterZ,
                leftArmCenterResolved,
                rightArmCenterResolved,
                leftLegCenterResolved,
                rightLegCenterResolved,
                resolvedArmMinY,
                resolvedLegMinY
            );
        }

        private float[] buildCenter(int count, float sumX, float sumY, float sumZ, float fallbackX, float fallbackY, float fallbackZ) {
            if (count > 0) {
                return new float[]{sumX / count, sumY / count, sumZ / count};
            }
            return new float[]{fallbackX, fallbackY, fallbackZ};
        }
    }

    private void copyCenter(float[] source, float[] target) {
        System.arraycopy(source, 0, target, 0, target.length);
    }

    private RegionFlags classifyRegion(float x, float y) {
        TorsoZone zone = determineTorsoZone(y);
        boolean arm = y >= armMinY && y >= kneeLevel && Math.abs(x - centerX) > armThreshold;
        boolean leg = y <= hipLevel && y >= legMinY;
        boolean left = x < centerX;
        return new RegionFlags(zone, arm, leg, left);
    }

    private TorsoZone determineTorsoZone(float y) {
        if (hasHead && y >= headThreshold) {
            return TorsoZone.HEAD;
        }
        if (y >= shoulderLevel - torsoSpan * 0.6f) {
            return TorsoZone.UPPER;
        }
        if (y >= torsoBottom && y < shoulderLevel - torsoSpan * 0.6f) {
            return TorsoZone.LOWER;
        }
        if (y >= hipLevel - torsoSpan * 0.3f && y < torsoBottom) {
            return TorsoZone.PELVIS;
        }
        return TorsoZone.NONE;
    }

    private float[] toSortedArray(List<Float> values) {
        float[] sorted = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            sorted[i] = values.get(i);
        }
        Arrays.sort(sorted);
        return sorted;
    }

    private float quantile(float[] sorted, float quantile) {
        if (sorted.length == 0) {
            return 0f;
        }
        float index = quantile * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = Math.min(sorted.length - 1, lower + 1);
        float fraction = index - lower;
        if (lower == upper) {
            return sorted[lower];
        }
        return sorted[lower] * (1 - fraction) + sorted[upper] * fraction;
    }

    private float scaleAround(float value, float pivot, float scale) {
        return (value - pivot) * scale + pivot;
    }

    private float stretchAround(float value, float pivot, float span, float scale) {
        float offset = value - pivot;
        float ratio = span != 0 ? offset / span : 0f;
        return pivot + offset * scale + ratio * span * (scale - 1f) * 0.05f;
    }

    private static final class VertexPosition {
        float x;
        float y;
        float z;

        VertexPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private record BoundsData(float minX, float maxX, float minY, float maxY, float minZ, float maxZ, float[] sortedY) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BoundsData(
                float otherMinX,
                float otherMaxX,
                float otherMinY,
                float otherMaxY,
                float otherMinZ,
                float otherMaxZ,
                float[] otherSortedY
            ))) {
                return false;
            }
            return Float.compare(minX, otherMinX) == 0
                && Float.compare(maxX, otherMaxX) == 0
                && Float.compare(minY, otherMinY) == 0
                && Float.compare(maxY, otherMaxY) == 0
                && Float.compare(minZ, otherMinZ) == 0
                && Float.compare(maxZ, otherMaxZ) == 0
                && Arrays.equals(sortedY, otherSortedY);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(minX, maxX, minY, maxY, minZ, maxZ);
            result = 31 * result + Arrays.hashCode(sortedY);
            return result;
        }

        @Override
        public String toString() {
            return "BoundsData[minX=" + minX
                + ", maxX=" + maxX
                + ", minY=" + minY
                + ", maxY=" + maxY
                + ", minZ=" + minZ
                + ", maxZ=" + maxZ
                + ", sortedY=" + Arrays.toString(sortedY)
                + "]";
        }
    }

    private record Thresholds(float headThreshold, float shoulderLevel, float torsoBottom, float hipLevel, float kneeLevel) {}

    private record LimbData(
        boolean hasHead,
        float headCenterX,
        float headCenterZ,
        float[] leftArmCenter,
        float[] rightArmCenter,
        float[] leftLegCenter,
        float[] rightLegCenter,
        float armMinY,
        float legMinY
    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LimbData(
                boolean otherHasHead,
                float otherHeadCenterX,
                float otherHeadCenterZ,
                float[] otherLeftArmCenter,
                float[] otherRightArmCenter,
                float[] otherLeftLegCenter,
                float[] otherRightLegCenter,
                float otherArmMinY,
                float otherLegMinY
            ))) {
                return false;
            }
            return hasHead == otherHasHead
                && Float.compare(headCenterX, otherHeadCenterX) == 0
                && Float.compare(headCenterZ, otherHeadCenterZ) == 0
                && Float.compare(armMinY, otherArmMinY) == 0
                && Float.compare(legMinY, otherLegMinY) == 0
                && Arrays.equals(leftArmCenter, otherLeftArmCenter)
                && Arrays.equals(rightArmCenter, otherRightArmCenter)
                && Arrays.equals(leftLegCenter, otherLeftLegCenter)
                && Arrays.equals(rightLegCenter, otherRightLegCenter);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(hasHead, headCenterX, headCenterZ, armMinY, legMinY);
            result = 31 * result + Arrays.hashCode(leftArmCenter);
            result = 31 * result + Arrays.hashCode(rightArmCenter);
            result = 31 * result + Arrays.hashCode(leftLegCenter);
            result = 31 * result + Arrays.hashCode(rightLegCenter);
            return result;
        }

        @Override
        public String toString() {
            return "LimbData[hasHead=" + hasHead
                + ", headCenterX=" + headCenterX
                + ", headCenterZ=" + headCenterZ
                + ", leftArmCenter=" + Arrays.toString(leftArmCenter)
                + ", rightArmCenter=" + Arrays.toString(rightArmCenter)
                + ", leftLegCenter=" + Arrays.toString(leftLegCenter)
                + ", rightLegCenter=" + Arrays.toString(rightLegCenter)
                + ", armMinY=" + armMinY
                + ", legMinY=" + legMinY
                + "]";
        }
    }

    private record ProportionSettings(
        float headScale,
        float shoulderWidth,
        float torsoWidth,
        float torsoLength,
        float hipWidth,
        float armLength,
        float armThickness,
        float legLength,
        float legThickness
    ) {
        static ProportionSettings from(Map<String, Double> params) {
            return new ProportionSettings(
                value(params, "Head Size"),
                value(params, "Shoulder Width"),
                value(params, "Torso Width"),
                value(params, "Torso Length"),
                value(params, "Hip Width"),
                value(params, "Arm Length"),
                value(params, "Arm Thickness"),
                value(params, "Leg Length"),
                value(params, "Leg Thickness")
            );
        }

        private static float value(Map<String, Double> params, String key) {
            return params.getOrDefault(key, 1.0).floatValue();
        }
    }

    private enum TorsoZone { NONE, HEAD, UPPER, LOWER, PELVIS }

    private record RegionFlags(TorsoZone torsoZone, boolean arm, boolean leg, boolean left) {}
}
