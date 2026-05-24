package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DrawingScreen extends Screen {
    private static final DrawingCanvas SHARED_CANVAS = new DrawingCanvas(0, 0);
    private static final int TOOLBAR_X = 8;
    private static final int TOOLBAR_Y = 8;
    private static final int TOOLBAR_HEIGHT = 18;
    private static final int TOOLBAR_GAP = 3;
    private static final int STATUS_TICKS = 80;

    private final DrawingCanvas canvas;
    private Text statusText = Text.empty();
    private int statusTicks = 0;

    public DrawingScreen() {
        super(Text.of("Drawing Screen"));
        this.canvas = SHARED_CANVAS;
    }

    @Override
    protected void init() {
        canvas.updateColorPickerPosition(width, height);
    }

    @Override
    public void tick() {
        super.tick();
        canvas.getColorPicker().tick();
        if (statusTicks > 0) {
            statusTicks--;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (handleToolbarClick((int) mouseX, (int) mouseY)) {
                return true;
            }

            if (canvas.getColorPicker().handleMouseClick(mouseX, mouseY)) {
                canvas.getBrushSettings().setColor(canvas.getColorPicker().getSelectedColor());
                canvas.setToolMode(ToolMode.BRUSH);
                return true;
            }

            canvas.startStroke((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && canvas.isDrawing()) {
            canvas.continueStroke((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            canvas.endStroke();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        canvas.getColorPicker().handleMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            if (Screen.hasShiftDown()) {
                return canvas.adjustOpacity((float) verticalAmount * 0.05f);
            }
            return canvas.adjustBrushSize((float) verticalAmount * 0.5f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        StrokeRenderer.renderStrokes(canvas.getStrokes(), canvas.getCurrentStroke(), context.getMatrices());
        canvas.getColorPicker().render(context);
        super.render(context, mouseX, mouseY, delta);
        renderToolbar(context);
        renderInfo(context);
        renderColorSlots(context);
        renderBrushPreview(context, mouseX, mouseY);
        renderStatus(context);
    }

    private void renderToolbar(DrawContext context) {
        for (ToolbarButton button : buildToolbarButtons()) {
            boolean active = isToolbarButtonActive(button.id());
            boolean enabled = isToolbarButtonEnabled(button.id());
            int fill = !enabled ? 0x88404040 : active ? 0xCC2F80ED : 0xAA101010;
            int border = active ? 0xFFFFFFFF : 0xAAFFFFFF;
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
            context.drawBorder(button.x(), button.y(), button.width(), button.height(), border);
            int textColor = enabled ? 0xFFFFFFFF : 0xFFAAAAAA;
            int textX = button.x() + (button.width() - textRenderer.getWidth(button.label())) / 2;
            int textY = button.y() + 5;
            context.drawTextWithShadow(textRenderer, button.label(), textX, textY, textColor);
        }
    }

    private void renderInfo(DrawContext context) {
        BrushSettings brush = canvas.getBrushSettings();
        int y = getToolbarBottom() + 8;

        context.drawTextWithShadow(textRenderer, Text.translatable("gui.screendraw.button_draw"), 10, y, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("gui.screendraw.button_ESC_C"), 10, y + 15, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("gui.screendraw.undo_redo"), 10, y + 30, 0xFFFFFFFF);
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable(
                        "gui.screendraw.smoothing",
                        Text.translatable(brush.isSmoothingEnabled() ? "gui.screendraw.off" : "gui.screendraw.on")
                ),
                10,
                y + 45,
                0xFFFFFFFF
        );
        context.drawTextWithShadow(textRenderer, Text.translatable("gui.screendraw.alt"), 10, y + 60, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("gui.screendraw.palette_slots"), 10, y + 75, 0xFFDDDDDD);

        int statsY = this.width < 520 ? y + 95 : y;
        Text brushSizeText = Text.translatable(
                "gui.screendraw.brush_size",
                String.format(Locale.ROOT, "%.1f", brush.getLineWidth())
        );
        drawRightAligned(context, brushSizeText, statsY, 0xFFFFFF00);

        Text opacityText = Text.translatable(
                "gui.screendraw.opacity",
                Math.round(brush.getOpacity() * 100.0f)
        );
        drawRightAligned(context, opacityText, statsY + 15, 0xFFDDDDDD);

        Text toolText = Text.translatable(
                "gui.screendraw.tool",
                Text.translatable(toolKey(brush.getToolMode()))
        );
        drawRightAligned(context, toolText, statsY + 30, brush.isEraserSelected() ? 0xFFFF8888 : 0xFF99DDFF);

        Text colorInfoText = Text.translatable(
                "gui.screendraw.brush_color",
                String.format(Locale.ROOT, "#%08X", brush.getEffectiveColor())
        );
        int colorTextWidth = this.textRenderer.getWidth(colorInfoText);
        context.drawTextWithShadow(this.textRenderer, colorInfoText, this.width - colorTextWidth - 10, statsY + 45, 0xFFFFFFFF);
        renderColorIndicator(context, brush.getEffectiveColor(), this.width - colorTextWidth - 27, statsY + 45, 12);
    }

    private void renderColorSlots(DrawContext context) {
        BrushSettings brush = canvas.getBrushSettings();
        int x = 10;
        int y = getToolbarBottom() + (this.width < 520 ? 164 : 104);
        int size = 14;
        for (int i = 0; i < brush.getColorSlotCount(); i++) {
            int sx = x + i * (size + 5);
            context.fill(sx - 1, y - 1, sx + size + 1, y + size + 1, 0xFF000000);
            context.fill(sx, y, sx + size, y + size, brush.getColorSlot(i));
            if ((brush.getColorSlot(i) & 0x00FFFFFF) == (brush.getColor() & 0x00FFFFFF)) {
                context.drawBorder(sx - 2, y - 2, size + 4, size + 4, 0xFFFFFFFF);
            }
            context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)), sx + 4, y + size + 3, 0xFFFFFFFF);
        }
    }

    private void renderBrushPreview(DrawContext context, int mouseX, int mouseY) {
        if (canvas.getColorPicker().isVisible() || isPointInToolbar(mouseX, mouseY)) {
            return;
        }

        BrushSettings brush = canvas.getBrushSettings();
        int size = Math.max(3, Math.round(brush.getLineWidth()));
        int half = size / 2;
        int x1 = mouseX - half;
        int y1 = mouseY - half;
        int x2 = x1 + size;
        int y2 = y1 + size;

        if (brush.isEraserSelected()) {
            context.fill(x1, y1, x2, y2, 0x22FFFFFF);
            context.drawBorder(x1, y1, size, size, 0xFFFF6666);
            return;
        }

        int previewColor = withMinimumAlpha(brush.getEffectiveColor(), 0x66);
        context.fill(x1, y1, x2, y2, previewColor);
        context.drawBorder(x1, y1, size, size, 0xCCFFFFFF);
    }

    private void renderStatus(DrawContext context) {
        if (statusTicks <= 0) {
            return;
        }
        int width = textRenderer.getWidth(statusText) + 16;
        int x = (this.width - width) / 2;
        int y = this.height - 28;
        context.fill(x, y, x + width, y + 18, 0xCC101010);
        context.drawBorder(x, y, width, 18, 0xAAFFFFFF);
        context.drawTextWithShadow(textRenderer, statusText, x + 8, y + 5, 0xFFFFFFFF);
    }

    private void renderColorIndicator(DrawContext context, int color, int x, int y, int size) {
        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();
        matrixStack.translate(x, y, 0);
        context.fill(-1, -1, size + 1, size + 1, 0xFF000000);
        context.fill(0, 0, size, size, color);
        matrixStack.pop();
    }

    private void drawRightAligned(DrawContext context, Text text, int y, int color) {
        int textWidth = this.textRenderer.getWidth(text);
        context.drawTextWithShadow(this.textRenderer, text, this.width - textWidth - 10, y, color);
    }

    private boolean handleToolbarClick(int mouseX, int mouseY) {
        for (ToolbarButton button : buildToolbarButtons()) {
            if (button.contains(mouseX, mouseY)) {
                if (isToolbarButtonEnabled(button.id())) {
                    handleToolbarButton(button.id());
                }
                return true;
            }
        }
        return false;
    }

    private void handleToolbarButton(String id) {
        switch (id) {
            case "brush" -> canvas.setToolMode(ToolMode.BRUSH);
            case "eraser" -> canvas.setToolMode(ToolMode.ERASER);
            case "undo" -> undo();
            case "redo" -> redo();
            case "clear" -> clear();
            case "save" -> saveDrawing();
            case "load" -> loadDrawing();
            case "export" -> exportDrawing();
            case "smoothing" -> canvas.toggleSmoothing();
            default -> {
            }
        }
    }

    private List<ToolbarButton> buildToolbarButtons() {
        List<ToolbarButton> buttons = new ArrayList<>();
        addToolbarButton(buttons, "brush", Text.translatable("gui.screendraw.toolbar.brush"));
        addToolbarButton(buttons, "eraser", Text.translatable("gui.screendraw.toolbar.eraser"));
        addToolbarButton(buttons, "undo", Text.translatable("gui.screendraw.toolbar.undo"));
        addToolbarButton(buttons, "redo", Text.translatable("gui.screendraw.toolbar.redo"));
        addToolbarButton(buttons, "clear", Text.translatable("gui.screendraw.toolbar.clear"));
        addToolbarButton(buttons, "save", Text.translatable("gui.screendraw.toolbar.save"));
        addToolbarButton(buttons, "load", Text.translatable("gui.screendraw.toolbar.load"));
        addToolbarButton(buttons, "export", Text.translatable("gui.screendraw.toolbar.export"));
        addToolbarButton(buttons, "smoothing", Text.translatable("gui.screendraw.toolbar.smoothing"));
        return buttons;
    }

    private void addToolbarButton(List<ToolbarButton> buttons, String id, Text label) {
        int x = TOOLBAR_X;
        int y = TOOLBAR_Y;
        if (!buttons.isEmpty()) {
            ToolbarButton previous = buttons.getLast();
            x = previous.x() + previous.width() + TOOLBAR_GAP;
            y = previous.y();
            if (x + textRenderer.getWidth(label) + 12 > this.width - TOOLBAR_X) {
                x = TOOLBAR_X;
                y += TOOLBAR_HEIGHT + TOOLBAR_GAP;
            }
        }

        int width = Math.max(36, textRenderer.getWidth(label) + 12);
        buttons.add(new ToolbarButton(id, label, x, y, width, TOOLBAR_HEIGHT));
    }

    private boolean isToolbarButtonActive(String id) {
        BrushSettings brush = canvas.getBrushSettings();
        return switch (id) {
            case "brush" -> brush.getToolMode() == ToolMode.BRUSH;
            case "eraser" -> brush.getToolMode() == ToolMode.ERASER;
            case "smoothing" -> brush.isSmoothingEnabled();
            default -> false;
        };
    }

    private boolean isToolbarButtonEnabled(String id) {
        return switch (id) {
            case "undo" -> canvas.getHistory().canUndo();
            case "redo" -> canvas.getHistory().canRedo();
            case "clear", "save", "export" -> canvas.getStrokeCount() > 0;
            default -> true;
        };
    }

    private int getToolbarBottom() {
        int bottom = TOOLBAR_Y + TOOLBAR_HEIGHT;
        for (ToolbarButton button : buildToolbarButtons()) {
            bottom = Math.max(bottom, button.y() + button.height());
        }
        return bottom;
    }

    private boolean isPointInToolbar(int mouseX, int mouseY) {
        for (ToolbarButton button : buildToolbarButtons()) {
            if (button.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private void undo() {
        if (canvas.undo()) {
            setStatus(Text.translatable("gui.screendraw.status.undo"));
        }
    }

    private void redo() {
        if (canvas.redo()) {
            setStatus(Text.translatable("gui.screendraw.status.redo"));
        }
    }

    private void clear() {
        if (canvas.clear()) {
            setStatus(Text.translatable("gui.screendraw.status.cleared"));
        }
    }

    private void saveDrawing() {
        try {
            Path path = DrawingStorage.save(canvas.getStrokes(), canvas.getBrushSettings(), width, height);
            setStatus(Text.translatable("gui.screendraw.status.saved", path.getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Text.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void loadDrawing() {
        try {
            DrawingStorage.SavedDrawing savedDrawing = DrawingStorage.load();
            if (savedDrawing == null) {
                setStatus(Text.translatable("gui.screendraw.status.load_missing"));
                return;
            }

            canvas.replaceStrokes(savedDrawing.strokes(), true);
            BrushSettings brush = canvas.getBrushSettings();
            brush.setColor(savedDrawing.color());
            brush.setLineWidth(savedDrawing.lineWidth());
            brush.setOpacity(savedDrawing.opacity());
            brush.setSmoothingEnabled(savedDrawing.smoothingEnabled());
            brush.setToolMode(savedDrawing.toolMode());
            brush.setColorSlots(savedDrawing.colorSlots());
            canvas.getColorPicker().setSelectedColor(savedDrawing.color());
            setStatus(Text.translatable("gui.screendraw.status.loaded", savedDrawing.path().getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Text.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void exportDrawing() {
        try {
            Path path = DrawingStorage.exportPng(canvas.getStrokes(), width, height);
            setStatus(Text.translatable("gui.screendraw.status.exported", path.getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Text.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void setStatus(Text text) {
        statusText = text;
        statusTicks = STATUS_TICKS;
    }

    private void handleColorSlotKey(int keyCode, boolean shiftPressed) {
        int slot = keyCode - GLFW.GLFW_KEY_1;
        if (slot < 0 || slot >= canvas.getBrushSettings().getColorSlotCount()) {
            return;
        }

        if (shiftPressed) {
            canvas.getBrushSettings().saveColorSlot(slot);
            setStatus(Text.translatable("gui.screendraw.status.slot_saved", slot + 1));
        } else {
            canvas.getBrushSettings().loadColorSlot(slot);
            canvas.getColorPicker().setSelectedColor(canvas.getBrushSettings().getColor());
            canvas.setToolMode(ToolMode.BRUSH);
            setStatus(Text.translatable("gui.screendraw.status.slot_loaded", slot + 1));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean altPressed = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        boolean ctrlPressed = Screen.hasControlDown() || (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftPressed = Screen.hasShiftDown() || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (isAltKey(keyCode) || (altPressed && !canvas.getColorPicker().isVisible())) {
            canvas.getColorPicker().show();
            return true;
        }

        if (ctrlPressed) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_Z -> {
                    if (shiftPressed) {
                        redo();
                    } else {
                        undo();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_Y -> {
                    redo();
                    return true;
                }
                case GLFW.GLFW_KEY_S -> {
                    saveDrawing();
                    return true;
                }
                case GLFW.GLFW_KEY_L -> {
                    loadDrawing();
                    return true;
                }
                case GLFW.GLFW_KEY_P -> {
                    exportDrawing();
                    return true;
                }
                default -> {
                }
            }
        }

        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_5) {
            handleColorSlotKey(keyCode, shiftPressed);
            return true;
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_C -> {
                clear();
                yield true;
            }
            case GLFW.GLFW_KEY_S -> {
                canvas.toggleSmoothing();
                yield true;
            }
            case GLFW.GLFW_KEY_B -> {
                canvas.setToolMode(ToolMode.BRUSH);
                yield true;
            }
            case GLFW.GLFW_KEY_E -> {
                canvas.setToolMode(ToolMode.ERASER);
                yield true;
            }
            case GLFW.GLFW_KEY_X -> {
                canvas.toggleToolMode();
                yield true;
            }
            case GLFW.GLFW_KEY_EQUAL -> {
                canvas.adjustBrushSize(0.5f);
                yield true;
            }
            case GLFW.GLFW_KEY_MINUS -> {
                canvas.adjustBrushSize(-0.5f);
                yield true;
            }
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (isAltKey(keyCode)) {
            if (canvas.getColorPicker().isVisible()) {
                canvas.getColorPicker().hide();
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private static boolean isAltKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static String toolKey(ToolMode toolMode) {
        return toolMode == ToolMode.ERASER ? "gui.screendraw.tool.eraser" : "gui.screendraw.tool.brush";
    }

    private static int withMinimumAlpha(int color, int minAlpha) {
        int alpha = Math.max((color >>> 24) & 0xFF, minAlpha);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private record ToolbarButton(String id, Text label, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
