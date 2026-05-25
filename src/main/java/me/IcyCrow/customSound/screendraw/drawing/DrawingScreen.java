package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
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
    private Component statusText = Component.empty();
    private int statusTicks = 0;

    public DrawingScreen() {
        super(Component.literal("Drawing Screen"));
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
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
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
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && canvas.isDrawing()) {
            canvas.continueStroke((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            canvas.endStroke();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        canvas.getColorPicker().handleMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            if (isShiftPressed()) {
                return canvas.adjustOpacity((float) verticalAmount * 0.05f);
            }
            return canvas.adjustBrushSize((float) verticalAmount * 0.5f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        StrokeRenderer.renderStrokes(canvas.getStrokes(), canvas.getCurrentStroke(), context);
        canvas.getColorPicker().render(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
        renderToolbar(context);
        renderInfo(context);
        renderColorSlots(context);
        renderBrushPreview(context, mouseX, mouseY);
        renderStatus(context);
    }

    private void renderToolbar(GuiGraphicsExtractor context) {
        for (ToolbarButton button : buildToolbarButtons()) {
            boolean active = isToolbarButtonActive(button.id());
            boolean enabled = isToolbarButtonEnabled(button.id());
            int fill = !enabled ? 0x88404040 : active ? 0xCC2F80ED : 0xAA101010;
            int border = active ? 0xFFFFFFFF : 0xAAFFFFFF;
            context.fill(button.x(), button.y(), button.x() + button.width(), button.y() + button.height(), fill);
            context.outline(button.x(), button.y(), button.width(), button.height(), border);
            int textColor = enabled ? 0xFFFFFFFF : 0xFFAAAAAA;
            int textX = button.x() + (button.width() - font.width(button.label())) / 2;
            int textY = button.y() + 5;
            context.text(font, button.label(), textX, textY, textColor);
        }
    }

    private void renderInfo(GuiGraphicsExtractor context) {
        BrushSettings brush = canvas.getBrushSettings();
        int y = getToolbarBottom() + 8;

        context.text(font, Component.translatable("gui.screendraw.button_draw"), 10, y, 0xFFFFFFFF);
        context.text(font, Component.translatable("gui.screendraw.button_ESC_C"), 10, y + 15, 0xFFFFFFFF);
        context.text(font, Component.translatable("gui.screendraw.undo_redo"), 10, y + 30, 0xFFFFFFFF);
        context.text(
                font,
                Component.translatable(
                        "gui.screendraw.smoothing",
                        Component.translatable(brush.isSmoothingEnabled() ? "gui.screendraw.off" : "gui.screendraw.on")
                ),
                10,
                y + 45,
                0xFFFFFFFF
        );
        context.text(font, Component.translatable("gui.screendraw.alt"), 10, y + 60, 0xFFFFFFFF);
        context.text(font, Component.translatable("gui.screendraw.palette_slots"), 10, y + 75, 0xFFDDDDDD);

        int statsY = this.width < 520 ? y + 95 : y;
        Component brushSizeText = Component.translatable(
                "gui.screendraw.brush_size",
                String.format(Locale.ROOT, "%.1f", brush.getLineWidth())
        );
        drawRightAligned(context, brushSizeText, statsY, 0xFFFFFF00);

        Component opacityText = Component.translatable(
                "gui.screendraw.opacity",
                Math.round(brush.getOpacity() * 100.0f)
        );
        drawRightAligned(context, opacityText, statsY + 15, 0xFFDDDDDD);

        Component toolText = Component.translatable(
                "gui.screendraw.tool",
                Component.translatable(toolKey(brush.getToolMode()))
        );
        drawRightAligned(context, toolText, statsY + 30, brush.isEraserSelected() ? 0xFFFF8888 : 0xFF99DDFF);

        Component colorInfoText = Component.translatable(
                "gui.screendraw.brush_color",
                String.format(Locale.ROOT, "#%08X", brush.getEffectiveColor())
        );
        int colorTextWidth = this.font.width(colorInfoText);
        context.text(this.font, colorInfoText, this.width - colorTextWidth - 10, statsY + 45, 0xFFFFFFFF);
        renderColorIndicator(context, brush.getEffectiveColor(), this.width - colorTextWidth - 27, statsY + 45, 12);
    }

    private void renderColorSlots(GuiGraphicsExtractor context) {
        BrushSettings brush = canvas.getBrushSettings();
        int x = 10;
        int y = getToolbarBottom() + (this.width < 520 ? 164 : 104);
        int size = 14;
        for (int i = 0; i < brush.getColorSlotCount(); i++) {
            int sx = x + i * (size + 5);
            context.fill(sx - 1, y - 1, sx + size + 1, y + size + 1, 0xFF000000);
            context.fill(sx, y, sx + size, y + size, brush.getColorSlot(i));
            if ((brush.getColorSlot(i) & 0x00FFFFFF) == (brush.getColor() & 0x00FFFFFF)) {
                context.outline(sx - 2, y - 2, size + 4, size + 4, 0xFFFFFFFF);
            }
            context.text(font, Component.literal(String.valueOf(i + 1)), sx + 4, y + size + 3, 0xFFFFFFFF);
        }
    }

    private void renderBrushPreview(GuiGraphicsExtractor context, int mouseX, int mouseY) {
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
            context.outline(x1, y1, size, size, 0xFFFF6666);
            return;
        }

        int previewColor = withMinimumAlpha(brush.getEffectiveColor(), 0x66);
        context.fill(x1, y1, x2, y2, previewColor);
        context.outline(x1, y1, size, size, 0xCCFFFFFF);
    }

    private void renderStatus(GuiGraphicsExtractor context) {
        if (statusTicks <= 0) {
            return;
        }
        int width = font.width(statusText) + 16;
        int x = (this.width - width) / 2;
        int y = this.height - 28;
        context.fill(x, y, x + width, y + 18, 0xCC101010);
        context.outline(x, y, width, 18, 0xAAFFFFFF);
        context.text(font, statusText, x + 8, y + 5, 0xFFFFFFFF);
    }

    private void renderColorIndicator(GuiGraphicsExtractor context, int color, int x, int y, int size) {
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        context.fill(x, y, x + size, y + size, color);
    }

    private void drawRightAligned(GuiGraphicsExtractor context, Component text, int y, int color) {
        int textWidth = this.font.width(text);
        context.text(this.font, text, this.width - textWidth - 10, y, color);
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
        addToolbarButton(buttons, "brush", Component.translatable("gui.screendraw.toolbar.brush"));
        addToolbarButton(buttons, "eraser", Component.translatable("gui.screendraw.toolbar.eraser"));
        addToolbarButton(buttons, "undo", Component.translatable("gui.screendraw.toolbar.undo"));
        addToolbarButton(buttons, "redo", Component.translatable("gui.screendraw.toolbar.redo"));
        addToolbarButton(buttons, "clear", Component.translatable("gui.screendraw.toolbar.clear"));
        addToolbarButton(buttons, "save", Component.translatable("gui.screendraw.toolbar.save"));
        addToolbarButton(buttons, "load", Component.translatable("gui.screendraw.toolbar.load"));
        addToolbarButton(buttons, "export", Component.translatable("gui.screendraw.toolbar.export"));
        addToolbarButton(buttons, "smoothing", Component.translatable("gui.screendraw.toolbar.smoothing"));
        return buttons;
    }

    private void addToolbarButton(List<ToolbarButton> buttons, String id, Component label) {
        int x = TOOLBAR_X;
        int y = TOOLBAR_Y;
        if (!buttons.isEmpty()) {
            ToolbarButton previous = buttons.getLast();
            x = previous.x() + previous.width() + TOOLBAR_GAP;
            y = previous.y();
            if (x + font.width(label) + 12 > this.width - TOOLBAR_X) {
                x = TOOLBAR_X;
                y += TOOLBAR_HEIGHT + TOOLBAR_GAP;
            }
        }

        int width = Math.max(36, font.width(label) + 12);
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
            setStatus(Component.translatable("gui.screendraw.status.undo"));
        }
    }

    private void redo() {
        if (canvas.redo()) {
            setStatus(Component.translatable("gui.screendraw.status.redo"));
        }
    }

    private void clear() {
        if (canvas.clear()) {
            setStatus(Component.translatable("gui.screendraw.status.cleared"));
        }
    }

    private void saveDrawing() {
        try {
            Path path = DrawingStorage.save(canvas.getStrokes(), canvas.getBrushSettings(), width, height);
            setStatus(Component.translatable("gui.screendraw.status.saved", path.getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Component.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void loadDrawing() {
        try {
            DrawingStorage.SavedDrawing savedDrawing = DrawingStorage.load();
            if (savedDrawing == null) {
                setStatus(Component.translatable("gui.screendraw.status.load_missing"));
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
            setStatus(Component.translatable("gui.screendraw.status.loaded", savedDrawing.path().getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Component.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void exportDrawing() {
        try {
            Path path = DrawingStorage.exportPng(canvas.getStrokes(), width, height);
            setStatus(Component.translatable("gui.screendraw.status.exported", path.getFileName().toString()));
        } catch (IOException exception) {
            setStatus(Component.translatable("gui.screendraw.status.error", exception.getMessage()));
        }
    }

    private void setStatus(Component text) {
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
            setStatus(Component.translatable("gui.screendraw.status.slot_saved", slot + 1));
        } else {
            canvas.getBrushSettings().loadColorSlot(slot);
            canvas.getColorPicker().setSelectedColor(canvas.getBrushSettings().getColor());
            canvas.setToolMode(ToolMode.BRUSH);
            setStatus(Component.translatable("gui.screendraw.status.slot_loaded", slot + 1));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        int modifiers = input.modifiers();
        boolean altPressed = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        boolean ctrlPressed = isControlPressed() || (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftPressed = isShiftPressed() || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

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
            default -> super.keyPressed(input);
        };
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        int keyCode = input.key();
        if (isAltKey(keyCode)) {
            if (canvas.getColorPicker().isVisible()) {
                canvas.getColorPicker().hide();
                return true;
            }
        }
        return super.keyReleased(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    private static boolean isAltKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private boolean isShiftPressed() {
        return isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private boolean isControlPressed() {
        return isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    private boolean isKeyPressed(int keyCode) {
        return minecraft != null && InputConstants.isKeyDown(minecraft.getWindow(), keyCode);
    }

    private static String toolKey(ToolMode toolMode) {
        return toolMode == ToolMode.ERASER ? "gui.screendraw.tool.eraser" : "gui.screendraw.tool.brush";
    }

    private static int withMinimumAlpha(int color, int minAlpha) {
        int alpha = Math.max((color >>> 24) & 0xFF, minAlpha);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private record ToolbarButton(String id, Component label, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}

