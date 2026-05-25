package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ColorPicker {
    private static final int[] PALETTE_COLORS = {
            0xFF000000, 0xFF7F7F7F, 0xFFC3C3C3, 0xFF880015,
            0xFFED1C24, 0xFFB97A57, 0xFFFF7F27, 0xFFB5E61D,
            0xFFFFC90E, 0xFFFFF200, 0xFFEFE4B0, 0xFF22B14C,
            0xFF3F48CC, 0xFF00A2E8, 0xFFA349A4, 0xFF7092BE,
            0xFF99D9EA, 0xFFC8BFE7, 0xFFFFAEC9, 0xFFFFFFFF
    };

    private int centerX;
    private int centerY;
    private final int squareSize;
    private final int radius;

    private boolean visible;
    private int selectedColor;
    private int selectedIndex;

    private boolean shouldShow = false;
    private float showAnimation = 0.0f;
    private static final int SHOW_ANIMATION_DURATION = 8;

    private float selectionAnimation = 0.0f;
    private static final int SELECTION_ANIMATION_DURATION = 10;

    private int hoveredIndex = -1;
    private final float[] hoverAnimations = new float[PALETTE_COLORS.length];
    private static final int HOVER_ANIMATION_DURATION = 6;

    private float pulseAnimation = 0.0f;

    public ColorPicker(int centerX, int centerY) {
        this(centerX, centerY, 24, 120);
    }

    public ColorPicker(int centerX, int centerY, int squareSize, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.squareSize = squareSize;
        this.radius = radius;
        this.visible = false;
        this.selectedColor = 0xFFFFFFFF;
        this.selectedIndex = -1;
    }

    public void setPosition(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void toggle() {
        if (shouldShow) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        shouldShow = true;
    }

    public void hide() {
        shouldShow = false;
    }

    public void tick() {
        if (shouldShow && showAnimation < 1.0f) {
            showAnimation = Math.min(1.0f, showAnimation + 1.0f / SHOW_ANIMATION_DURATION);
            if (showAnimation >= 1.0f) {
                visible = true;
            }
        } else if (!shouldShow && showAnimation > 0.0f) {
            showAnimation = Math.max(0.0f, showAnimation - 1.0f / SHOW_ANIMATION_DURATION);
            if (showAnimation <= 0.0f) {
                visible = false;
            }
        }

        if (selectionAnimation > 0.0f) {
            selectionAnimation = Math.max(0.0f, selectionAnimation - 1.0f / SELECTION_ANIMATION_DURATION);
        }

        for (int i = 0; i < hoverAnimations.length; i++) {
            if (i == hoveredIndex && hoverAnimations[i] < 1.0f) {
                hoverAnimations[i] = Math.min(1.0f, hoverAnimations[i] + 1.0f / HOVER_ANIMATION_DURATION);
            } else if (i != hoveredIndex && hoverAnimations[i] > 0.0f) {
                hoverAnimations[i] = Math.max(0.0f, hoverAnimations[i] - 1.0f / HOVER_ANIMATION_DURATION);
            }
        }

        pulseAnimation += 0.15f;
        if (pulseAnimation > 2 * Math.PI) {
            pulseAnimation -= 2 * Math.PI;
        }
    }

    public void handleMouseMove(double mouseX, double mouseY) {
        if (!isVisible()) {
            hoveredIndex = -1;
            return;
        }

        hoveredIndex = -1;
        float showScale = getShowScale();

        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            double angle = 2 * Math.PI * i / PALETTE_COLORS.length;
            double cx = centerX + Math.cos(angle) * radius * showScale;
            double cy = centerY + Math.sin(angle) * radius * showScale;
            int size = Math.max(1, Math.round(squareSize * showScale));
            int x = (int) Math.round(cx - size / 2.0);
            int y = (int) Math.round(cy - size / 2.0);

            if (mouseX >= x && mouseX <= x + size &&
                    mouseY >= y && mouseY <= y + size) {
                hoveredIndex = i;
                break;
            }
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY) {
        if (!isVisible()) {
            return false;
        }

        float showScale = getShowScale();
        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            double angle = 2 * Math.PI * i / PALETTE_COLORS.length;
            double cx = centerX + Math.cos(angle) * radius * showScale;
            double cy = centerY + Math.sin(angle) * radius * showScale;
            int size = Math.max(1, Math.round(squareSize * showScale));
            int x = (int) Math.round(cx - size / 2.0);
            int y = (int) Math.round(cy - size / 2.0);

            if (mouseX >= x && mouseX <= x + size &&
                    mouseY >= y && mouseY <= y + size) {
                selectedColor = PALETTE_COLORS[i];
                selectedIndex = i;
                selectionAnimation = 1.0f;
                return true;
            }
        }
        return false;
    }

    public void render(GuiGraphicsExtractor context) {
        if (showAnimation <= 0.0f) {
            return;
        }

        float showScale = getShowScale();
        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            double angle = 2 * Math.PI * i / PALETTE_COLORS.length;
            double cx = centerX + Math.cos(angle) * radius * showScale;
            double cy = centerY + Math.sin(angle) * radius * showScale;

            float hoverProgress = smoothStep(hoverAnimations[i]);
            float hoverScale = 1.0f + 0.2f * hoverProgress;

            float selectionScale = 1.0f;
            if (i == selectedIndex) {
                float selectionProgress = smoothStep(selectionAnimation);
                selectionScale = 1.0f + 0.4f * selectionProgress;

                float pulse = (float) Math.sin(pulseAnimation) * 0.1f + 1.0f;
                selectionScale *= pulse;
            }

            int size = Math.max(1, Math.round(squareSize * showScale * hoverScale * selectionScale));
            int x = (int) Math.round(cx - size / 2.0);
            int y = (int) Math.round(cy - size / 2.0);

            if (i == selectedIndex) {
                int pad = 3;
                context.fill(x - pad, y - pad, x + size + pad, y + size + pad, 0xFFFFFFFF);
            }

            if (hoverProgress > 0.0f) {
                int hoverAlpha = (int) (255 * hoverProgress * 0.6f);
                int hoverColor = (hoverAlpha << 24) | 0x00FFFFFF;
                int hoverPad = 2;
                context.fill(x - hoverPad, y - hoverPad, x + size + hoverPad, y + size + hoverPad, hoverColor);
            }

            context.fill(x, y, x + size, y + size, PALETTE_COLORS[i]);
            context.outline(x, y, size, size, 0xFF000000);
        }
    }

    private float getShowScale() {
        float showProgress = smoothStep(showAnimation);
        return 0.3f + 0.7f * showProgress;
    }

    private static float smoothStep(float x) {
        if (x <= 0.0f) {
            return 0.0f;
        }
        if (x >= 1.0f) {
            return 1.0f;
        }
        return x * x * (3.0f - 2.0f * x);
    }

    public boolean isVisible() { return showAnimation > 0.0f; }
    public int getSelectedColor() { return selectedColor; }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            if ((PALETTE_COLORS[i] & 0x00FFFFFF) == (color & 0x00FFFFFF)) {
                selectedIndex = i;
                return;
            }
        }
        selectedIndex = -1;
    }

    public boolean isPointInside(double mouseX, double mouseY) {
        if (showAnimation <= 0.0f) {
            return false;
        }
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double maxDistance = radius + squareSize + 16;
        return distance <= maxDistance;
    }

    public static int getPaletteSize() { return PALETTE_COLORS.length; }
    public static int getColorByIndex(int index) {
        if (index >= 0 && index < PALETTE_COLORS.length) {
            return PALETTE_COLORS[index];
        }
        return 0xFFFFFFFF;
    }
}
