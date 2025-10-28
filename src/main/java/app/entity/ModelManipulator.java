package app.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.scene.shape.TriangleMesh;

/**
 * Performs proportion manipulation on a character model made of triangle meshes.
 */
public class ModelManipulator {
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

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        List<Float> ySamples = new ArrayList<>();

        for (TriangleMesh mesh : meshes) {
            float[] source = new float[mesh.getPoints().size()];
            mesh.getPoints().toArray(source);
            originalPoints.add(source);
            workingBuffers.add(new float[source.length]);

            for (int i = 0; i < source.length; i += 3) {
                float x = source[i];
                float y = source[i + 1];
                float z = source[i + 2];

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
                ySamples.add(y);
            }
        }

        if (ySamples.isEmpty()) {
            minX = maxX = minY = maxY = minZ = maxZ = 0f;
        }

        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.centerX = (minX + maxX) * 0.5f;
        this.centerZ = (minZ + maxZ) * 0.5f;

        float[] sortedY = new float[ySamples.size()];
        for (int i = 0; i < ySamples.size(); i++) {
            sortedY[i] = ySamples.get(i);
        }
        Arrays.sort(sortedY);

        this.headThreshold = quantile(sortedY, 0.88f);
        this.shoulderLevel = quantile(sortedY, 0.79f);
        this.torsoBottom = quantile(sortedY, 0.42f);
        this.hipLevel = quantile(sortedY, 0.35f);
        this.kneeLevel = quantile(sortedY, 0.20f);

        float height = Math.max(maxY - minY, 1f);
        this.shoulderRadius = Math.max(height * 0.04f, 1f);
        this.hipRadius = Math.max(height * 0.05f, 1f);
        this.armThreshold = Math.max((maxX - minX) * 0.35f, 1f);

        float headSumX = 0f;
        float headSumZ = 0f;
        int headCount = 0;

        float leftArmSumX = 0f;
        float leftArmSumY = 0f;
        float leftArmSumZ = 0f;
        int leftArmCount = 0;

        float rightArmSumX = 0f;
        float rightArmSumY = 0f;
        float rightArmSumZ = 0f;
        int rightArmCount = 0;

        float leftLegSumX = 0f;
        float leftLegSumY = 0f;
        float leftLegSumZ = 0f;
        int leftLegCount = 0;

        float rightLegSumX = 0f;
        float rightLegSumY = 0f;
        float rightLegSumZ = 0f;
        int rightLegCount = 0;

        float armMinY = Float.POSITIVE_INFINITY;
        float legMinY = Float.POSITIVE_INFINITY;

        for (float[] source : originalPoints) {
            for (int i = 0; i < source.length; i += 3) {
                float x = source[i];
                float y = source[i + 1];
                float z = source[i + 2];

                if (y >= headThreshold) {
                    headSumX += x;
                    headSumZ += z;
                    headCount++;
                }

                boolean isArm = Math.abs(x - centerX) > armThreshold && y > kneeLevel;
                if (isArm) {
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
                    armMinY = Math.min(armMinY, y);
                }

                if (y <= hipLevel) {
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
                    legMinY = Math.min(legMinY, y);
                }
            }
        }

        this.hasHead = headCount > 0;
        this.headCenterX = hasHead ? headSumX / headCount : centerX;
        this.headCenterZ = hasHead ? headSumZ / headCount : centerZ;

        leftArmCenter[0] = leftArmCount > 0 ? leftArmSumX / leftArmCount : centerX - armThreshold;
        leftArmCenter[1] = leftArmCount > 0 ? leftArmSumY / leftArmCount : shoulderLevel;
        leftArmCenter[2] = leftArmCount > 0 ? leftArmSumZ / leftArmCount : centerZ;

        rightArmCenter[0] = rightArmCount > 0 ? rightArmSumX / rightArmCount : centerX + armThreshold;
        rightArmCenter[1] = rightArmCount > 0 ? rightArmSumY / rightArmCount : shoulderLevel;
        rightArmCenter[2] = rightArmCount > 0 ? rightArmSumZ / rightArmCount : centerZ;

        float hipOffset = Math.max((maxX - minX) * 0.2f, 1f);
        leftLegCenter[0] = leftLegCount > 0 ? leftLegSumX / leftLegCount : centerX - hipOffset;
        leftLegCenter[1] = leftLegCount > 0 ? leftLegSumY / leftLegCount : hipLevel - (height * 0.25f);
        leftLegCenter[2] = leftLegCount > 0 ? leftLegSumZ / leftLegCount : centerZ;

        rightLegCenter[0] = rightLegCount > 0 ? rightLegSumX / rightLegCount : centerX + hipOffset;
        rightLegCenter[1] = rightLegCount > 0 ? rightLegSumY / rightLegCount : hipLevel - (height * 0.25f);
        rightLegCenter[2] = rightLegCount > 0 ? rightLegSumZ / rightLegCount : centerZ;

        if (Float.isInfinite(armMinY)) {
            armMinY = Math.min(shoulderLevel, kneeLevel + height * 0.1f);
        }
        if (armMinY >= shoulderLevel) {
            armMinY = shoulderLevel - Math.max(height * 0.05f, 1f);
        }
        if (Float.isInfinite(legMinY)) {
            legMinY = minY;
        }

        this.armMinY = armMinY;
        this.legMinY = legMinY;
        this.torsoSpan = Math.max(shoulderLevel - torsoBottom, 1f);
        this.armSpan = Math.max(shoulderLevel - armMinY, 1f);
        this.legSpan = Math.max(hipLevel - legMinY, 1f);
    }

    public void apply(Map<String, Double> params) {
        float headSize = getParam(params, "Head Size");
        float shoulderWidth = getParam(params, "Shoulder Width");
        float torsoWidth = getParam(params, "Torso Width");
        float torsoLength = getParam(params, "Torso Length");
        float hipWidth = getParam(params, "Hip Width");
        float armLength = getParam(params, "Arm Length");
        float armThickness = getParam(params, "Arm Thickness");
        float legLength = getParam(params, "Leg Length");
        float legThickness = getParam(params, "Leg Thickness");

        float torsoDelta = (torsoLength - 1f) * torsoSpan;
        float legDelta = (legLength - 1f) * legSpan;
        float armDelta = (armLength - 1f) * armSpan;

        float modelHeight = Math.max(1f, maxY - minY);
        float shoulderRadius = Math.max(0.08f * modelHeight, 0.35f * torsoSpan);
        float hipRadius = Math.max(0.08f * modelHeight, 0.30f * legSpan);

        for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
            float[] original = originalPoints.get(meshIndex);
            float[] dst = workingBuffers.get(meshIndex);
            System.arraycopy(original, 0, dst, 0, original.length);

            for (int i = 0; i < original.length; i += 3) {
                float x = original[i];
                float y = original[i + 1];
                float z = original[i + 2];

                boolean isArm = Math.abs(x - centerX) > armThreshold && y > kneeLevel;
                float ny;

                if (isArm) {
                    if (y <= shoulderLevel) {
                        float normalized = clamp((shoulderLevel - y) / armSpan, 0f, 1f);
                        ny = shoulderLevel - armSpan * (armLength * normalized) + torsoDelta + legDelta;
                    } else {
                        ny = y + armDelta + torsoDelta + legDelta;
                    }
                } else if (y <= hipLevel) {
                    float normalized = clamp((hipLevel - y) / legSpan, 0f, 1f);
                    ny = hipLevel - legSpan * (legLength * normalized);
                } else if (y <= shoulderLevel) {
                    float normalized = clamp((y - torsoBottom) / torsoSpan, 0f, 1f);
                    ny = torsoBottom + torsoSpan * (torsoLength * normalized) + legDelta;
                } else {
                    ny = y + torsoDelta + legDelta;
                }

                float nx = x;
                float nz = z;

                boolean isLeg = ny <= hipLevel;

                if (hasHead && y >= headThreshold && headSize != 1f) {
                    float weight = smoothStep(headThreshold, maxY, y);
                    if (weight > 0f) {
                        float scale = 1f + (headSize - 1f) * weight;
                        nx = headCenterX + (nx - headCenterX) * scale;
                        nz = headCenterZ + (nz - headCenterZ) * scale;
                    }
                }

                if (Math.abs(shoulderWidth - 1.0) > 1e-3) {
                    float wShoulder = bandWeight(ny, shoulderLevel, shoulderRadius);
                    if (wShoulder > 0f) {
                        float armAtten = isArm ? 0.35f : 1.0f;
                        float sx = f(1.0 + (shoulderWidth - 1.0) * wShoulder * armAtten);
                        float sz = f(1.0 + (shoulderWidth - 1.0) * 0.35 * wShoulder * armAtten);
                        sx = clamp(sx, 0.4f, 2.5f);
                        sz = clamp(sz, 0.4f, 2.5f);
                        nx = centerX + (nx - centerX) * sx;
                        nz = centerZ + (nz - centerZ) * sz;
                    }
                }

                if (!isArm && Math.abs(torsoWidth - 1.0) > 1e-3) {
                    float wTorso = smoothStep(torsoBottom, shoulderLevel, ny);
                    if (wTorso > 0f) {
                        float sx = f(1.0 + (torsoWidth - 1.0) * wTorso);
                        float sz = f(1.0 + (torsoWidth - 1.0) * 0.5 * wTorso);
                        sx = clamp(sx, 0.4f, 2.5f);
                        sz = clamp(sz, 0.4f, 2.5f);
                        nx = centerX + (nx - centerX) * sx;
                        nz = centerZ + (nz - centerZ) * sz;
                    }
                }

                if (Math.abs(hipWidth - 1.0) > 1e-3) {
                    float wHip = bandWeight(ny, hipLevel, hipRadius);
                    if (wHip > 0f) {
                        float legBlendDown = 1.0f - smoothStep(legMinY, hipLevel, ny);
                        float legAtten = isLeg ? (0.5f * (1.0f - 0.7f * legBlendDown)) : 1.0f;
                        float sx = f(1.0 + (hipWidth - 1.0) * wHip * legAtten);
                        float sz = f(1.0 + (hipWidth - 1.0) * 0.45 * wHip * legAtten);
                        sx = clamp(sx, 0.4f, 2.5f);
                        sz = clamp(sz, 0.4f, 2.5f);
                        nx = centerX + (nx - centerX) * sx;
                        nz = centerZ + (nz - centerZ) * sz;
                    }
                }

                if (isArm && armThickness != 1f) {
                    float radialWeight = smoothStep(armMinY, shoulderLevel, ny);
                    if (radialWeight > 0f) {
                        float[] center = x < centerX ? leftArmCenter : rightArmCenter;
                        float scale = 1f + (armThickness - 1f) * radialWeight;
                        nx = center[0] + (nx - center[0]) * scale;
                        nz = center[2] + (nz - center[2]) * scale;
                    }
                }

                if (ny <= hipLevel && legThickness != 1f) {
                    float radialWeight = 1f - smoothStep(legMinY, hipLevel, ny);
                    if (radialWeight > 0f) {
                        float[] center = x < centerX ? leftLegCenter : rightLegCenter;
                        float scale = 1f + (legThickness - 1f) * radialWeight;
                        nx = center[0] + (nx - center[0]) * scale;
                        nz = center[2] + (nz - center[2]) * scale;
                    }
                }

                dst[i] = nx;
                dst[i + 1] = ny;
                dst[i + 2] = nz;
            }

            meshes.get(meshIndex).getPoints().set(0, dst, 0, dst.length);
        }
    }

    private float getParam(Map<String, Double> params, String key) {
        Double value = params.get(key);
        return value == null ? 1f : value.floatValue();
    }

    private float bandWeight(float y, float center, float radius) {
        float d = Math.abs(y - center);
        if (radius <= 1e-6f || d >= radius) {
            return 0f;
        }
        float t = 1f - (d / radius);
        float s = t * t * (3f - 2f * t);
        return s * s;
    }

    private float f(double v) {
        return (float) Math.max(-1e9, Math.min(1e9, v));
    }

    private float smoothStep(float edge0, float edge1, float v) {
        float span = edge1 - edge0;
        if (Math.abs(span) < 1e-5f) {
            return v >= edge1 ? 1f : 0f;
        }
        float t = clamp((v - edge0) / span, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private float falloff(float v, float center, float radius) {
        float d = Math.abs(v - center);
        if (d >= radius) {
            return 0f;
        }
        float n = 1f - (d / radius);
        return n * n;
    }

    private float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private float quantile(float[] sorted, float q) {
        if (sorted.length == 0) {
            return 0f;
        }
        float cq = clamp(q, 0f, 1f);
        int index = (int) Math.floor(cq * (sorted.length - 1));
        index = Math.max(0, Math.min(sorted.length - 1, index));
        return sorted[index];
    }
}
