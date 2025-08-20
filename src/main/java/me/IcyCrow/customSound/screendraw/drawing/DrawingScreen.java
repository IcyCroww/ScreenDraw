package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Главный экран для рисования
 * Следует принципу Single Responsibility - только управление UI и делегирование действий
 */
public class DrawingScreen extends Screen {

    private final DrawingCanvas canvas;

    public DrawingScreen() {
        super(Text.of("Drawing Screen"));
        // Создаем canvas с размерами по умолчанию, обновим в init()
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

        MatrixStack matrixStack = context.getMatrices();

        // Рендерим колорпикер если он видимый
        if (canvas.getColorPicker().isVisible()) {
            canvas.getColorPicker().render(context);
        }

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
        context.drawTextWithShadow(this.textRenderer, "Alt - колорпикер", 10, 70, 0xFFFFFFFF);

        // Статистика
        context.drawTextWithShadow(this.textRenderer,
                "История: " + history.getUndoHistorySize() + "/" + history.getMaxHistorySize(), 10, 90, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer,
                "Штрихи: " + canvas.getStrokeCount() + ", Точки: " + canvas.getTotalPointCount(), 10, 105, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer,
                "Колесо мыши - размер кисти: " + String.format("%.1f", brush.getLineWidth()), 10, 120, 0xFFAAAAAA);

        // Размер кисти в правом верхнем углу
        String brushSize = "Кисть: " + String.format("%.1f", brush.getLineWidth());
        int textWidth = this.textRenderer.getWidth(brushSize);
        context.drawTextWithShadow(this.textRenderer, brushSize,
                this.width - textWidth - 10, 10, 0xFFFFFF00);

        // Текущий цвет кисти в правом верхнем углу
        String colorInfo = "Цвет: " + String.format("#%08X", brush.getColor());
        int colorTextWidth = this.textRenderer.getWidth(colorInfo);
        context.drawTextWithShadow(this.textRenderer, colorInfo,
                this.width - colorTextWidth - 10, 25, brush.getColor());

        // Индикатор цвета (маленький квадратик)
        int colorSquareSize = 16;
        int colorSquareX = this.width - colorTextWidth - colorSquareSize - 15;
        int colorSquareY = 25;

        context.fill(colorSquareX - 1, colorSquareY - 1,
                colorSquareX + colorSquareSize + 1, colorSquareY + colorSquareSize + 1,
                0xFF000000); // Черная рамка
        context.fill(colorSquareX, colorSquareY,
                colorSquareX + colorSquareSize, colorSquareY + colorSquareSize,
                brush.getColor()); // Цвет кисти
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean altPressed = (modifiers & 4) != 0;   // GLFW_MOD_ALT = 4
        boolean ctrlPressed = (modifiers & 2) != 0;  // GLFW_MOD_CONTROL = 2

        // Показываем палитру при нажатии Alt
        if (altPressed && !canvas.getColorPicker().isVisible()) {
            canvas.getColorPicker().show();
            return true;
        }

        // Обработка Ctrl+Z и Ctrl+Y
        if (ctrlPressed) {
            if (keyCode == 90) { // Z
                canvas.undo();
                return true;
            } else if (keyCode == 89) { // Y
                canvas.redo();
                return true;
            }
        }

        // Дополнительные клавиши
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
        // Скрываем палитру при отпускании любой клавиши Alt
        if (keyCode == 342 || keyCode == 346) { // Left Alt или Right Alt
            if (canvas.getColorPicker().isVisible()) {
                canvas.getColorPicker().hide();
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
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