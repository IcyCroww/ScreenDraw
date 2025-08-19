package me.IcyCrow.customSound.screendraw;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class DrawingScreen extends Screen {

    // Список точек для рисования
    private List<DrawPoint> drawPoints = new ArrayList<>();

    // История для Ctrl+Z и Ctrl+Y
    private Stack<List<DrawPoint>> undoHistory = new Stack<>();
    private Stack<List<DrawPoint>> redoHistory = new Stack<>();
    private static final int MAX_HISTORY = 50;

    // Состояние мыши
    private boolean isDrawing = false;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    // Для кривых Безье - контрольные точки текущего штриха
    private List<Point> currentStroke = new ArrayList<>();
    private boolean smoothingEnabled = true;

    // Класс для хранения точки рисования
    private static class DrawPoint {
        public final int x, y;
        public final int color;

        public DrawPoint(int x, int y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    // Простой класс точки для кривых Безье
    private static class Point {
        public final float x, y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public DrawingScreen() {
        super(Text.of("Drawing Screen"));
    }

    @Override
    protected void init() {
        // Очищаем точки при инициализации экрана
        saveToHistory();
        drawPoints.clear();
        currentStroke.clear();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Проверяем левую кнопку мыши (button == 0)
        if (button == 0) {
            // Сохраняем состояние для отмены
            saveToHistory();

            isDrawing = true;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;

            // Начинаем новый штрих
            currentStroke.clear();
            currentStroke.add(new Point((float) mouseX, (float) mouseY));

            // Добавляем первую точку
            drawPoints.add(new DrawPoint((int) mouseX, (int) mouseY, 0xFFFFFFFF)); // Белый цвет
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Останавливаем рисование при отпускании левой кнопки
        if (button == 0) {
            isDrawing = false;

            // Применяем сглаживание Безье к завершенному штриху
            if (smoothingEnabled && currentStroke.size() > 2) {
                applyCurveSmoothing();
            }

            currentStroke.clear();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // Рисуем только если зажата левая кнопка мыши
        if (isDrawing) {
            int currentX = (int) mouseX;
            int currentY = (int) mouseY;

            // Добавляем точку к текущему штриху
            currentStroke.add(new Point((float) mouseX, (float) mouseY));

            // Добавляем интерполяцию между точками для плавной линии
            if (lastMouseX != -1 && lastMouseY != -1) {
                interpolatePoints(lastMouseX, lastMouseY, currentX, currentY);
            }

            lastMouseX = currentX;
            lastMouseY = currentY;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    // Метод для интерполяции точек между двумя позициями мыши
    private void interpolatePoints(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);

        if (steps > 0) {
            for (int i = 0; i <= steps; i++) {
                int x = x1 + (x2 - x1) * i / steps;
                int y = y1 + (y2 - y1) * i / steps;
                drawPoints.add(new DrawPoint(x, y, 0xFFFFFFFF)); // Белый цвет
            }
        }
    }

    // Применение сглаживания кривыми Безье
    private void applyCurveSmoothing() {
        if (currentStroke.size() < 3) return;

        // Удаляем точки текущего штриха из общего списка
        int pointsToRemove = 0;
        for (int i = 0; i < currentStroke.size() - 1; i++) {
            Point p1 = currentStroke.get(i);
            Point p2 = currentStroke.get(i + 1);
            int steps = (int) Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y));
            pointsToRemove += steps + 1;
        }

        // Удаляем последние добавленные точки
        for (int i = 0; i < pointsToRemove && !drawPoints.isEmpty(); i++) {
            drawPoints.remove(drawPoints.size() - 1);
        }

        // Создаем сглаженную кривую
        List<Point> smoothedPoints = createBezierCurve(currentStroke);

        // Добавляем сглаженные точки
        for (Point point : smoothedPoints) {
            drawPoints.add(new DrawPoint((int) point.x, (int) point.y, 0xFFFFFFFF));
        }
    }

    // Создание кривой Безье из набора точек
    private List<Point> createBezierCurve(List<Point> controlPoints) {
        List<Point> result = new ArrayList<>();

        if (controlPoints.size() < 3) {
            return new ArrayList<>(controlPoints);
        }

        // Группируем точки по тройкам для создания квадратичных кривых Безье
        for (int i = 0; i < controlPoints.size() - 2; i += 2) {
            Point p0 = controlPoints.get(i);
            Point p1 = controlPoints.get(Math.min(i + 1, controlPoints.size() - 1));
            Point p2 = controlPoints.get(Math.min(i + 2, controlPoints.size() - 1));

            // Генерируем точки квадратичной кривой Безье
            List<Point> curveSegment = generateQuadraticBezier(p0, p1, p2, 20);
            result.addAll(curveSegment);
        }

        return result;
    }

    // Генерация квадратичной кривой Безье
    private List<Point> generateQuadraticBezier(Point p0, Point p1, Point p2, int segments) {
        List<Point> points = new ArrayList<>();

        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;

            // Квадратичная формула Безье: B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
            float x = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * p1.x + t * t * p2.x;
            float y = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * p1.y + t * t * p2.y;

            points.add(new Point(x, y));
        }

        return points;
    }

    // Сохранение текущего состояния в историю
    private void saveToHistory() {
        // Создаем копию текущего состояния
        List<DrawPoint> stateCopy = new ArrayList<>(drawPoints);
        undoHistory.push(stateCopy);

        // Ограничиваем размер истории
        if (undoHistory.size() > MAX_HISTORY) {
            undoHistory.remove(0);
        }

        // Очищаем redo при новом действии
        redoHistory.clear();
    }

    // Отмена действия (Ctrl+Z)
    private void undo() {
        if (!undoHistory.isEmpty()) {
            // Сохраняем текущее состояние для redo
            redoHistory.push(new ArrayList<>(drawPoints));

            // Восстанавливаем предыдущее состояние
            drawPoints = new ArrayList<>(undoHistory.pop());
        }
    }

    // Повтор действия (Ctrl+Y)
    private void redo() {
        if (!redoHistory.isEmpty()) {
            // Сохраняем текущее состояние для undo
            undoHistory.push(new ArrayList<>(drawPoints));

            // Восстанавливаем состояние из redo
            drawPoints = new ArrayList<>(redoHistory.pop());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим фон (опционально)
        // this.renderBackground(context);

        // Рисуем все точки
        for (DrawPoint point : drawPoints) {
            // Рисуем пиксель как маленький прямоугольник 2x2
            context.fill(point.x - 1, point.y - 1, point.x + 1, point.y + 1, point.color);
        }

        // Рендерим остальные элементы UI
        super.render(context, mouseX, mouseY, delta);

        // Показываем инструкции
        context.drawTextWithShadow(
                this.textRenderer,
                "Зажми ЛКМ для рисования",
                10, 10,
                0xFFFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "ESC - выход, C - очистить",
                10, 25,
                0xFFFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "Ctrl+Z - отмена, Ctrl+Y - повтор",
                10, 40,
                0xFFFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                "S - " + (smoothingEnabled ? "выкл" : "вкл") + " сглаживание",
                10, 55,
                0xFFFFFFFF
        );

        // Показываем размер истории
        context.drawTextWithShadow(
                this.textRenderer,
                "История: " + undoHistory.size() + "/" + MAX_HISTORY,
                10, 70,
                0xFFAAAAAA
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Проверяем модификаторы
        boolean ctrlPressed = (modifiers & 2) != 0; // GLFW_MOD_CONTROL = 2

        if (ctrlPressed) {
            if (keyCode == 90) { // Ctrl+Z
                undo();
                return true;
            } else if (keyCode == 89) { // Ctrl+Y
                redo();
                return true;
            }
        }

        // Дополнительные клавиши для управления
        if (keyCode == 67) { // C - очистить экран
            saveToHistory();
            drawPoints.clear();
            return true;
        }

        if (keyCode == 83) { // S - переключить сглаживание
            smoothingEnabled = !smoothingEnabled;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // Не ставим игру на паузу
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }
}
