package me.IcyCrow.customSound.screendraw.drawing;

public class BrushSettings {
    private float lineWidth;
    private int color;
    private float opacity;
    private boolean smoothingEnabled;
    private ToolMode toolMode;
    private final int[] colorSlots;

    private static final float MIN_LINE_WIDTH = 0.5f;
    private static final float MAX_LINE_WIDTH = 20.0f;
    private static final float MIN_OPACITY = 0.1f;
    private static final float MAX_OPACITY = 1.0f;
    private static final float DEFAULT_LINE_WIDTH = 2.0f;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final int[] DEFAULT_COLOR_SLOTS = {
            0xFFFFFFFF, 0xFFED1C24, 0xFF00A2E8, 0xFF22B14C, 0xFFFFC90E
    };

    public BrushSettings() {
        this.lineWidth = DEFAULT_LINE_WIDTH;
        this.color = DEFAULT_COLOR;
        this.opacity = MAX_OPACITY;
        this.smoothingEnabled = true;
        this.toolMode = ToolMode.BRUSH;
        this.colorSlots = DEFAULT_COLOR_SLOTS.clone();
    }

    public BrushSettings(float lineWidth, int color, boolean smoothingEnabled) {
        this();
        setLineWidth(lineWidth);
        setColor(color);
        this.smoothingEnabled = smoothingEnabled;
    }

    public boolean adjustSize(float delta) {
        float oldLineWidth = lineWidth;
        setLineWidth(lineWidth + delta);
        return lineWidth != oldLineWidth;
    }

    public boolean adjustOpacity(float delta) {
        float oldOpacity = opacity;
        setOpacity(opacity + delta);
        return opacity != oldOpacity;
    }

    public void toggleSmoothing() {
        smoothingEnabled = !smoothingEnabled;
    }

    public void toggleToolMode() {
        toolMode = toolMode == ToolMode.BRUSH ? ToolMode.ERASER : ToolMode.BRUSH;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public int getColor() {
        return color;
    }

    public int getEffectiveColor() {
        int alpha = Math.round(opacity * 255.0f) & 0xFF;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean isSmoothingEnabled() {
        return smoothingEnabled;
    }

    public ToolMode getToolMode() {
        return toolMode;
    }

    public boolean isEraserSelected() {
        return toolMode == ToolMode.ERASER;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, lineWidth));
    }

    public void setColor(int color) {
        this.color = normalizeOpaque(color);
        int alpha = (color >>> 24) & 0xFF;
        if (alpha != 0xFF && alpha != 0) {
            setOpacity(alpha / 255.0f);
        }
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, opacity));
    }

    public void setSmoothingEnabled(boolean smoothingEnabled) {
        this.smoothingEnabled = smoothingEnabled;
    }

    public void setToolMode(ToolMode toolMode) {
        this.toolMode = toolMode == null ? ToolMode.BRUSH : toolMode;
    }

    public int getColorSlot(int index) {
        if (index < 0 || index >= colorSlots.length) {
            return DEFAULT_COLOR;
        }
        return colorSlots[index];
    }

    public void saveColorSlot(int index) {
        if (index >= 0 && index < colorSlots.length) {
            colorSlots[index] = getEffectiveColor();
        }
    }

    public void loadColorSlot(int index) {
        if (index >= 0 && index < colorSlots.length) {
            setColor(colorSlots[index]);
        }
    }

    public int[] getColorSlots() {
        return colorSlots.clone();
    }

    public void setColorSlots(int[] slots) {
        if (slots == null) {
            return;
        }
        int limit = Math.min(slots.length, colorSlots.length);
        for (int i = 0; i < limit; i++) {
            colorSlots[i] = slots[i];
        }
    }

    public int getColorSlotCount() {
        return colorSlots.length;
    }

    public static float getMinLineWidth() {
        return MIN_LINE_WIDTH;
    }

    public static float getMaxLineWidth() {
        return MAX_LINE_WIDTH;
    }

    public static float getMinOpacity() {
        return MIN_OPACITY;
    }

    public static float getMaxOpacity() {
        return MAX_OPACITY;
    }

    private static int normalizeOpaque(int color) {
        return 0xFF000000 | (color & 0x00FFFFFF);
    }
}
