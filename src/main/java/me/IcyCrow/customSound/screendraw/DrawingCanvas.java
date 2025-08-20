package me.IcyCrow.customSound.screendraw;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс представляющий холст для рисования
 */
public class DrawingCanvas {
    private final List<Stroke> strokes;
    private Stroke currentStroke;
    private final List<Point> currentBezierPoints;
    private final DrawingHistory history;
    private final BrushSettings brushSettings;

    // Состояние рисования
    private boolean isDrawing;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public DrawingCanvas() {
        this.strokes = new ArrayList<>();
        this.currentStroke = null;
        this.currentBezierPoints = new ArrayList<>();
        this.history = new DrawingHistory();
        this.brushSettings = new BrushSettings();
        this.isDrawing = false;
    }

    /**
     * Начинает новый штрих
     */
    public void startStroke(int mouseX, int mouseY) {
        if (!isDrawing) {
            history.saveState(strokes);

            isDrawing = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            currentStroke = new Stroke();
            currentBezierPoints.clear();
            currentBezierPoints.add(new Point(mouseX, mouseY));

            // Добавляем первую точку
            currentStroke.addPoint(new DrawPoint(mouseX, mouseY,
                    brushSettings.getColor(),
                    brushSettings.getLineWidth()));
        }
    }

    /**
     * Продолжает текущий штрих
     */
    public void continueStroke(int mouseX, int mouseY) {
        if (isDrawing && currentStroke != null) {
            currentBezierPoints.add(new Point(mouseX, mouseY));

            // Добавляем интерполированные точки между позициями мыши
            if (lastMouseX != -1 && lastMouseY != -1) {
                List<DrawPoint> interpolatedPoints = Stroke.interpolatePoints(
                        lastMouseX, lastMouseY, mouseX, mouseY,
                        brushSettings.getColor(), brushSettings.getLineWidth()
                );
                currentStroke.addPoints(interpolatedPoints);
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    /**
     * Завершает текущий штрих
     */
    public void endStroke() {
        if (isDrawing && currentStroke != null) {
            isDrawing = false;

            // Применяем сглаживание если включено
            if (brushSettings.isSmoothingEnabled() && currentBezierPoints.size() > 2) {
                currentStroke = BezierSmoother.smoothStroke(currentBezierPoints, brushSettings);
            }

            // Сохраняем завершенный штрих
            if (!currentStroke.isEmpty()) {
                currentStroke.complete();
                strokes.add(currentStroke);
            }

            currentStroke = null;
            currentBezierPoints.clear();
            lastMouseX = -1;
            lastMouseY = -1;
        }
    }

    /**
     * Отменяет последнее действие
     */
    public void undo() {
        List<Stroke> newStrokes = history.undo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
    }

    /**
     * Повторяет отмененное действие
     */
    public void redo() {
        List<Stroke> newStrokes = history.redo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
    }

    /**
     * Очищает холст
     */
    public void clear() {
        history.saveState(strokes);
        strokes.clear();
        if (currentStroke != null) {
            currentStroke.clear();
        }
    }

    /**
     * Изменяет размер кисти
     */
    public boolean adjustBrushSize(float delta) {
        return brushSettings.adjustSize(delta);
    }

    /**
     * Переключает сглаживание
     */
    public void toggleSmoothing() {
        brushSettings.toggleSmoothing();
    }

    /**
     * Подсчитывает общее количество точек
     */
    public int getTotalPointCount() {
        int total = 0;
        for (Stroke stroke : strokes) {
            total += stroke.getPointCount();
        }
        if (currentStroke != null) {
            total += currentStroke.getPointCount();
        }
        return total;
    }

    // Getters
    public List<Stroke> getStrokes() { return strokes; }
    public Stroke getCurrentStroke() { return currentStroke; }
    public BrushSettings getBrushSettings() { return brushSettings; }
    public DrawingHistory getHistory() { return history; }
    public boolean isDrawing() { return isDrawing; }
    public int getStrokeCount() { return strokes.size(); }
}