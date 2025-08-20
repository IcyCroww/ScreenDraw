package me.IcyCrow.customSound.screendraw;

/**
 * Класс для управления настройками кисти
 */
public class BrushSettings {
    private float lineWidth;
    private int color;
    private boolean smoothingEnabled;

    private static final float MIN_LINE_WIDTH = 0.5f;
    private static final float MAX_LINE_WIDTH = 20.0f;
    private static final float DEFAULT_LINE_WIDTH = 2.0f;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF; // Белый цвет

    public BrushSettings() {
        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.color = DEFAULT_COLOR;
        this.smoothingEnabled = true;
    }

    public BrushSettings(float lineWidth, int color, boolean smoothingEnabled) {
        this.lineWidth = Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, lineWidth));
        this.color = color;
        this.smoothingEnabled = smoothingEnabled;
    }

    // Изменение размера кисти
    public boolean adjustSize(float delta) {
        float oldLineWidth = lineWidth;
        lineWidth += delta;
        lineWidth = Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, lineWidth));
        return lineWidth != oldLineWidth;
    }

    public void toggleSmoothing() {
        smoothingEnabled = !smoothingEnabled;
    }

    // Getters
    public float getLineWidth() { return lineWidth; }
    public int getColor() { return color; }
    public boolean isSmoothingEnabled() { return smoothingEnabled; }

    // Setters
    public void setLineWidth(float lineWidth) {
        this.lineWidth = Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, lineWidth));
    }

    public void setColor(int color) { this.color = color; }
    public void setSmoothingEnabled(boolean smoothingEnabled) { this.smoothingEnabled = smoothingEnabled; }

    // Constants getters
    public static float getMinLineWidth() { return MIN_LINE_WIDTH; }
    public static float getMaxLineWidth() { return MAX_LINE_WIDTH; }
}