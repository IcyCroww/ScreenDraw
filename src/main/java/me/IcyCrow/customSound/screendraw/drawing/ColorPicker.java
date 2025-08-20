package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.DrawContext;

/**
 * HUE колесо-колорпикер
 */
public class ColorPicker {
    private int centerX;
    private int centerY;
    private final int squareSize;
    private final int radius;

    private boolean visible;
    private int selectedColor;
    private int selectedIndex;

    private static final int[] PALETTE_COLORS = {
            0xFF000000, 0xFF7F7F7F, 0xFFC3C3C3, 0xFF880015,
            0xFFED1C24, 0xFFB97A57, 0xFFFF7F27, 0xFFB5E61D,
            0xFFFFC90E, 0xFFFFF200, 0xFFEFE4B0, 0xFF22B14C,
            0xFF3F48CC, 0xFF00A2E8, 0xFFA349A4, 0xFF7092BE,
            0xFF99D9EA, 0xFFC8BFE7, 0xFFFFAEC9
    };

    public ColorPicker(int centerX, int centerY) {
        this(centerX, centerY, 24, 120);
    }

    public ColorPicker(int centerX, int centerY, int squareSize, int radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.squareSize = squareSize;
        this.radius = radius;

        this.visible = false;
        this.selectedColor = 0xFFFFFFFF; // белый по умолчанию
        this.selectedIndex = -1;
    }

    // Позволяет обновить позицию палитры
    public void setPosition(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public void toggle() { visible = !visible; }
    public void show() { visible = true; }
    public void hide() { visible = false; }

    public boolean handleMouseClick(double mouseX, double mouseY) {
        if (!visible) return false;

        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            double angle = 2 * Math.PI * i / PALETTE_COLORS.length;
            double cx = centerX + Math.cos(angle) * radius;
            double cy = centerY + Math.sin(angle) * radius;
            int x = (int) Math.round(cx - squareSize / 2.0);
            int y = (int) Math.round(cy - squareSize / 2.0);

            if (mouseX >= x && mouseX <= x + squareSize &&
                    mouseY >= y && mouseY <= y + squareSize) {
                selectedColor = PALETTE_COLORS[i];
                selectedIndex = i;
                return true;
            }
        }
        return false;
    }

    public void render(DrawContext context) {
        if (!visible) return;

        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            double angle = 2 * Math.PI * i / PALETTE_COLORS.length;
            double cx = centerX + Math.cos(angle) * radius;
            double cy = centerY + Math.sin(angle) * radius;
            int x = (int) Math.round(cx - squareSize / 2.0);
            int y = (int) Math.round(cy - squareSize / 2.0);
            int color = PALETTE_COLORS[i];

            if (i == selectedIndex) {
                int pad = 2;
                context.drawBorder(x - pad, y - pad, squareSize + pad * 2, squareSize + pad * 2, 0xFFFFFFFF);
            }

            fillRect(context, x, y, squareSize, squareSize, color);
            context.drawBorder(x, y, squareSize, squareSize, 0xFF000000);
        }
    }

    private static void fillRect(DrawContext ctx, int x, int y, int w, int h, int argb) {
        ctx.fill(x, y, x + w, y + h, argb);
    }

    public boolean isVisible() { return visible; }
    public int getSelectedColor() { return selectedColor; }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
        for (int i = 0; i < PALETTE_COLORS.length; i++) {
            if (PALETTE_COLORS[i] == color) {
                selectedIndex = i;
                return;
            }
        }
        selectedIndex = -1;
    }

    public boolean isPointInside(double mouseX, double mouseY) {
        if (!visible) return false;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double maxDistance = radius + squareSize + 16;
        return distance <= maxDistance;
    }

    public static int getPaletteSize() { return PALETTE_COLORS.length; }
    public static int getColorByIndex(int index) {
        if (index >= 0 && index < PALETTE_COLORS.length) return PALETTE_COLORS[index];
        return 0xFFFFFFFF;
    }
}