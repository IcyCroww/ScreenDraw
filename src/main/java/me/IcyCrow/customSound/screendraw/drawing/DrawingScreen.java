package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;


public class DrawingScreen extends Screen {

    private final DrawingCanvas canvas;

    public DrawingScreen() {
        super(Text.of("Drawing Screen"));
        this.canvas = new DrawingCanvas(width, height);
    }

    @Override
    protected void init() {
        // Обновляем canvas с актуальными размерами экрана
        canvas.updateColorPickerPosition(width, height);
        canvas.getHistory().saveState(canvas.getStrokes());
        canvas.clear();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (canvas.getColorPicker().handleMouseClick(mouseX, mouseY)) {
                canvas.getBrushSettings().setColor(canvas.getColorPicker().getSelectedColor());
                return true;
            }

            canvas.startStroke((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            canvas.endStroke();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        canvas.getColorPicker().handleMouseMove(mouseX, mouseY);

        if (canvas.isDrawing()) {
            canvas.continueStroke((int) mouseX, (int) mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            return canvas.adjustBrushSize((float) verticalAmount * 0.5f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        StrokeRenderer.renderStrokes(canvas.getStrokes(), canvas.getCurrentStroke(), context.getMatrices());

        canvas.getColorPicker().tick();

        canvas.getColorPicker().render(context);

        super.render(context, mouseX, mouseY, delta);


        renderUI(context);
    }

    private void renderUI(DrawContext context) {
        BrushSettings brush = canvas.getBrushSettings();
        DrawingHistory history = canvas.getHistory();

        context.drawTextWithShadow(this.textRenderer,Text.translatable("gui.screendraw.button_draw"), 10, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,Text.translatable("gui.screendraw.button_ESC_C"), 10, 25, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,Text.translatable("gui.screendraw.undo_redo"), 10, 40, 0xFFFFFFFF);
        context.drawTextWithShadow( //"S - " + (brush.isSmoothingEnabled() ? "выкл" : "вкл") + " сглаживание"
                this.textRenderer,
                Text.translatable(
                        "gui.screendraw.smoothing",
                        Text.translatable(brush.isSmoothingEnabled() ? "gui.screendraw.off" : "gui.screendraw.on")
                ),
                10, 55, 0xFFFFFFFF
        );

        context.drawTextWithShadow(this.textRenderer,Text.translatable("gui.screendraw.alt"), 10, 70, 0xFFFFFFFF);

        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable(
                        "gui.screendraw.brush_size",
                        String.format("%.1f", brush.getLineWidth())
                ),
                10, 85, 0xFFAAAAAA
        );


        Text brushSizeText = Text.translatable(
                "gui.screendraw.brush_size",
                String.format("%.1f", brush.getLineWidth())
        );
        int textWidth = this.textRenderer.getWidth(brushSizeText);
        context.drawTextWithShadow(
                this.textRenderer,
                brushSizeText,
                this.width - textWidth - 10,
                10,
                0xFFFFFF00
        );

        Text colorInfoText = Text.translatable(
                "gui.screendraw.brush_color",
                String.format("#%08X", brush.getColor())
        );
        int colorTextWidth = this.textRenderer.getWidth(colorInfoText);
        context.drawTextWithShadow(
                this.textRenderer,
                colorInfoText,
                this.width - colorTextWidth - 10,
                25,
                brush.getColor()
        );


        renderAnimatedColorIndicator(context, brush.getColor(),
                this.width - colorTextWidth - 25, 25, 10);
    }


    private void renderAnimatedColorIndicator(DrawContext context, int color, int x, int y, int size) {
        MatrixStack matrixStack = context.getMatrices();
        matrixStack.push();

        matrixStack.translate(x, y, 0);

        context.fill(-1, -1, size + 1, size + 1, 0xFF000000);

        context.fill(0, 0, size, size, color);

        matrixStack.pop();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean altPressed = (modifiers & 4) != 0;
        boolean ctrlPressed = (modifiers & 2) != 0;

        if (altPressed && !canvas.getColorPicker().isVisible()) {
            canvas.getColorPicker().show();
            return true;
        }

        if (ctrlPressed) {
            if (keyCode == 90) { // Z
                canvas.undo();
                return true;
            } else if (keyCode == 89) { // Y
                canvas.redo();
                return true;
            }
        }

        return switch (keyCode) {
            case 67 -> { // C
                canvas.clear();
                yield true;
            }
            case 83 -> { // S
                canvas.toggleSmoothing();
                yield true;
            }
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 342 || keyCode == 346) {
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
}