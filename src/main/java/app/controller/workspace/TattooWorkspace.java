package app.controller.workspace;

import app.entity.Tattoo;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Manages tattoo selection, painting, and texture updates for the workspace model.
 */
public final class TattooWorkspace {
    private final List<Tattoo> tattoos = new ArrayList<>();
    private final List<PhongMaterial> materials = new ArrayList<>();
    private Color skinTone = Color.WHITE;

    private int selectedIndex = -1;
    private Image pendingImage;
    private Image baseTemplate;
    private WritableImage tintedBase;
    private Canvas paintCanvas;
    private GraphicsContext graphics;
    private WritableImage renderedTexture;
    private int canvasWidth;
    private int canvasHeight;

    public void configureMaterials(List<PhongMaterial> mats) {
        materials.clear();
        materials.addAll(mats);
    }

    public void resetMaterials() {
        materials.clear();
        paintMaterials(null);
    }

    public void configureSurface(Image baseTexture, double width, double height) {
        this.canvasWidth = (int) Math.max(1, Math.round(width));
        this.canvasHeight = (int) Math.max(1, Math.round(height));
        this.baseTemplate = baseTexture != null ? baseTexture : createSolidTemplate(canvasWidth, canvasHeight, Color.rgb(225, 200, 180));
        this.paintCanvas = new Canvas(width, height);
        this.graphics = paintCanvas.getGraphicsContext2D();
        this.renderedTexture = new WritableImage(canvasWidth, canvasHeight);
        rebuildTintedBase();
        repaint();
    }

    public void clearSurface() {
        baseTemplate = null;
        tintedBase = null;
        paintCanvas = null;
        graphics = null;
        renderedTexture = null;
        pendingImage = null;
        clearAllTattoos();
        paintMaterials(null);
    }

    public void updateSkinTone(Color tint) {
        Color next = tint != null ? tint : Color.WHITE;
        if (Objects.equals(next, this.skinTone)) {
            return;
        }
        this.skinTone = next;
        rebuildTintedBase();
        repaint();
    }

    public boolean isPlacementAvailable() {
        return graphics != null && paintCanvas != null && renderedTexture != null;
    }

    public void preparePendingTattoo(Image image) {
        pendingImage = image;
        clearSelection();
    }

    public void clearPendingTattoo() {
        pendingImage = null;
    }

    public boolean hasPendingImage() {
        return pendingImage != null;
    }

    public Optional<Image> consumePendingImage() {
        Image image = pendingImage;
        pendingImage = null;
        return Optional.ofNullable(image);
    }

    public Optional<Tattoo> selected() {
        if (hasSelection()) {
            return Optional.of(tattoos.get(selectedIndex));
        }
        return Optional.empty();
    }

    public void clearSelection() {
        selectedIndex = -1;
    }

    public void clearAllTattoos() {
        tattoos.clear();
        selectedIndex = -1;
        repaint();
    }

    public void addTattoo(Tattoo tattoo) {
        Objects.requireNonNull(tattoo, "tattoo");
        tattoos.add(tattoo);
        selectedIndex = tattoos.size() - 1;
        repaint();
    }

    public boolean deleteSelectedTattoo() {
        if (!hasSelection()) {
            return false;
        }
        tattoos.remove(selectedIndex);
        if (tattoos.isEmpty()) {
            selectedIndex = -1;
        } else {
            selectedIndex = Math.min(selectedIndex, tattoos.size() - 1);
        }
        repaint();
        return true;
    }

    public void updateSelectedTattoo(Tattoo tattoo) {
        if (!hasSelection()) {
            return;
        }
        tattoos.set(selectedIndex, Objects.requireNonNull(tattoo, "tattoo"));
        repaint();
    }

    public void updateSelection(UnaryOperator<Tattoo> updater) {
        if (!hasSelection()) {
            return;
        }
        Tattoo current = tattoos.get(selectedIndex);
        Tattoo updated = updater.apply(current);
        if (updated != null) {
            tattoos.set(selectedIndex, updated);
            repaint();
        }
    }

    public boolean selectTattooAt(double u, double v) {
        int index = findTattooIndexAt(u, v);
        if (index >= 0) {
            selectedIndex = index;
            return true;
        }
        selectedIndex = -1;
        return false;
    }

    public void repaint() {
        if (graphics == null || paintCanvas == null || renderedTexture == null) {
            return;
        }
        double width = paintCanvas.getWidth();
        double height = paintCanvas.getHeight();

        graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
        graphics.setGlobalAlpha(1.0);
        graphics.clearRect(0, 0, width, height);
        if (tintedBase != null) {
            graphics.drawImage(tintedBase, 0, 0, width, height);
        } else {
            graphics.setFill(skinTone);
            graphics.fillRect(0, 0, width, height);
        }

        for (Tattoo tattoo : tattoos) {
            if (tattoo.image() == null) {
                continue;
            }
            double imgWidth = tattoo.image().getWidth();
            double imgHeight = tattoo.image().getHeight();
            double px = tattoo.u() * width;
            double py = tattoo.v() * height;
            double drawW = imgWidth * tattoo.widthScale();
            double drawH = imgHeight * tattoo.heightScale();

            graphics.save();
            graphics.translate(px, py);
            graphics.scale(1, -1);
            graphics.rotate(-tattoo.rotation());
            graphics.setGlobalAlpha(tattoo.alpha());
            graphics.setGlobalBlendMode(BlendMode.SRC_OVER);
            graphics.drawImage(tattoo.image(), -drawW / 2.0, -drawH / 2.0, drawW, drawH);
            graphics.restore();
        }

        paintCanvas.snapshot(null, renderedTexture);
        paintMaterials(renderedTexture);
    }

    public WritableImage renderedTexture() {
        return renderedTexture;
    }

    public Image baseTexture() {
        return tintedBase;
    }

    public List<Tattoo> exportableTattoos() {
        return List.copyOf(tattoos);
    }

    public void replaceTattoos(List<Tattoo> replacements) {
        tattoos.clear();
        if (replacements != null && !replacements.isEmpty()) {
            tattoos.addAll(replacements);
        }
        selectedIndex = -1;
        repaint();
    }

    private void paintMaterials(WritableImage texture) {
        for (PhongMaterial pm : materials) {
            pm.setDiffuseMap(texture);
            if (texture != null) {
                pm.setDiffuseColor(Color.WHITE);
            }
        }
    }

    private void rebuildTintedBase() {
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return;
        }
        PixelReader reader = baseTemplate != null ? baseTemplate.getPixelReader() : null;
        WritableImage tinted = new WritableImage(canvasWidth, canvasHeight);
        PixelWriter writer = tinted.getPixelWriter();
        if (reader == null) {
            fillSolid(writer, canvasWidth, canvasHeight, skinTone);
        } else {
            for (int y = 0; y < canvasHeight; y++) {
                for (int x = 0; x < canvasWidth; x++) {
                    Color sample = reader.getColor(x, y);
                    Color tintedColor = Color.color(
                        sample.getRed() * skinTone.getRed(),
                        sample.getGreen() * skinTone.getGreen(),
                        sample.getBlue() * skinTone.getBlue(),
                        sample.getOpacity()
                    );
                    writer.setColor(x, y, tintedColor);
                }
            }
        }
        tintedBase = tinted;
    }

    private Image createSolidTemplate(int width, int height, Color fill) {
        WritableImage image = new WritableImage(width, height);
        fillSolid(image.getPixelWriter(), width, height, fill);
        return image;
    }

    private void fillSolid(PixelWriter writer, int width, int height, Color fill) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, fill);
            }
        }
    }

    private int findTattooIndexAt(double u, double v) {
        if (!isPlacementAvailable() || tattoos.isEmpty()) {
            return -1;
        }
        for (int i = tattoos.size() - 1; i >= 0; i--) {
            Tattoo tattoo = tattoos.get(i);
            if (tattoo.image() != null && containsUV(tattoo, u, v)) {
                return i;
            }
        }
        return -1;
    }

    private boolean containsUV(Tattoo tattoo, double u, double v) {
        if (paintCanvas == null) {
            return false;
        }
        double width = Math.max(1.0, paintCanvas.getWidth());
        double height = Math.max(1.0, paintCanvas.getHeight());
        double halfWidth = (tattoo.image().getWidth() * tattoo.widthScale()) / (2.0 * width);
        double halfHeight = (tattoo.image().getHeight() * tattoo.heightScale()) / (2.0 * height);
        if (halfWidth <= 0 || halfHeight <= 0) {
            return false;
        }
        double dx = Math.abs(u - tattoo.u());
        double dy = Math.abs(v - tattoo.v());
        double normX = dx / halfWidth;
        double normY = dy / halfHeight;
        return normX * normX + normY * normY <= 1.15;
    }

    private boolean hasSelection() {
        return selectedIndex >= 0 && selectedIndex < tattoos.size();
    }
}
