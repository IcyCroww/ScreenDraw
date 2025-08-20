package me.IcyCrow.customSound.screendraw;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Главный экран для рисования
 * Следует принципу Single Responsibility - только управление UI и делегирование действий
 */
public class DrawingScreen extends Screen {

    private final DrawingCanvas canvas;

    public DrawingScreen() {
        super(Text.of("Drawing Screen"));
        this.canvas = new DrawingCanvas();
    }

    @Override
    protected void init() {
        canvas.getHistory().saveState(canvas.getStrokes());
        canvas.clear();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            canvas.startStroke((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { // Левая кнопка мыши
            canvas.endStroke();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (canvas.isDrawing()) {
            canvas.continueStroke((int) mouseX, (int) mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            // Изменяем размер кисти колесиком мыши
            return canvas.adjustBrushSize((float) verticalAmount * 0.5f);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим штрихи
        StrokeRenderer.renderStrokes(canvas.getStrokes(), canvas.getCurrentStroke(), context.getMatrices());

        // Рендерим остальные элементы UI
        super.render(context, mouseX, mouseY, delta);

        // Рендерим UI информацию
        renderUI(context);
    }

    /**
     * Рендерит пользовательский интерфейс
     */
    private void renderUI(DrawContext context) {
        BrushSettings brush = canvas.getBrushSettings();
        DrawingHistory history = canvas.getHistory();

        // Инструкции
        context.drawTextWithShadow(this.textRenderer, "Зажми ЛКМ для рисования", 10, 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "ESC - выход, C - очистить", 10, 25, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Ctrl+Z - отмена, Ctrl+Y - повтор", 10, 40, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                "S - " + (brush.isSmoothingEnabled() ? "выкл" : "вкл") + " сглаживание", 10, 55, 0xFFFFFFFF);

        // Статистика
        context.drawTextWithShadow(this.textRenderer,
                "История: " + history.getUndoHistorySize() + "/" + history.getMaxHistorySize(), 10, 70, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer,
                "Штрихи: " + canvas.getStrokeCount() + ", Точки: " + canvas.getTotalPointCount(), 10, 85, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer,
                "Колесо мыши - размер кисти: " + String.format("%.1f", brush.getLineWidth()), 10, 100, 0xFFAAAAAA);

        // Размер кисти в правом верхнем углу
        String brushSize = "Кисть: " + String.format("%.1f", brush.getLineWidth());
        int textWidth = this.textRenderer.getWidth(brushSize);
        context.drawTextWithShadow(this.textRenderer, brushSize,
                this.width - textWidth - 10, 10, 0xFFFFFF00);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ctrlPressed = (modifiers & 2) != 0; // GLFW_MOD_CONTROL = 2

        if (ctrlPressed) {
            if (keyCode == 90) { // Ctrl+Z
                canvas.undo();
                return true;
            } else if (keyCode == 89) { // Ctrl+Y
                canvas.redo();
                return true;
            }
        }

        // Дополнительные клавиши
        return switch (keyCode) {
            case 67 -> {
                canvas.clear();
                yield true;
            }
            case 83 -> {
                canvas.toggleSmoothing();
                yield true;
            }
            default -> super.keyPressed(keyCode, scanCode, modifiers);
        };

    }

    @Override
    public boolean shouldPause() {
        return false; // Не ставим игру на паузу
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Пустая реализация для прозрачного фона
    }
}