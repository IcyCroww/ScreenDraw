package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.List;

public class DrawingCanvas {
    private final List<Stroke> strokes;
    private Stroke currentStroke;
    private final List<Point> currentBezierPoints;
    private final DrawingHistory history;
    private final BrushSettings brushSettings;
    private ColorPicker colorPicker; // больше не final

    private boolean isDrawing;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public DrawingCanvas(int screenWidth, int screenHeight) {
        this.strokes = new ArrayList<>();
        this.currentStroke = null;
        this.currentBezierPoints = new ArrayList<>();
        this.history = new DrawingHistory();
        this.brushSettings = new BrushSettings();
        this.colorPicker = new ColorPicker(screenWidth / 2, screenHeight / 2);
        this.isDrawing = false;
    }

    public void startStroke(int mouseX, int mouseY) {
        if (colorPicker.isVisible() && colorPicker.handleMouseClick(mouseX, mouseY)) {
            brushSettings.setColor(colorPicker.getSelectedColor());
            return;
        }

        if (colorPicker.isVisible() && !colorPicker.isPointInside(mouseX, mouseY)) {
            colorPicker.hide();
            return;
        }

        if (!isDrawing && !colorPicker.isVisible()) {
            history.saveState(strokes);

            isDrawing = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;

            currentStroke = new Stroke();
            currentBezierPoints.clear();
            currentBezierPoints.add(new Point(mouseX, mouseY));

            currentStroke.addPoint(new DrawPoint(mouseX, mouseY,
                    brushSettings.getColor(),
                    brushSettings.getLineWidth()));
        }
    }

    public void continueStroke(int mouseX, int mouseY) {
        if (isDrawing && currentStroke != null && !colorPicker.isVisible()) {
            currentBezierPoints.add(new Point(mouseX, mouseY));

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

    public void endStroke() {
        if (isDrawing && currentStroke != null) {
            isDrawing = false;

            if (brushSettings.isSmoothingEnabled() && currentBezierPoints.size() > 2) {
                Stroke smoothedStroke = BezierSmoother.smoothStroke(currentBezierPoints, brushSettings);
                currentStroke = smoothedStroke;
            }

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

    public void toggleColorPicker() { colorPicker.toggle(); }

    public void updateColorPickerPosition(int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        if (colorPicker == null) {
            colorPicker = new ColorPicker(cx, cy);
        } else {
            colorPicker.setPosition(cx, cy);
        }
    }

    public void undo() {
        List<Stroke> newStrokes = history.undo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
    }

    public void redo() {
        List<Stroke> newStrokes = history.redo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
    }

    public void clear() {
        history.saveState(strokes);
        strokes.clear();
        if (currentStroke != null) {
            currentStroke.clear();
        }
    }

    public boolean adjustBrushSize(float delta) {
        return brushSettings.adjustSize(delta);
    }

    public void toggleSmoothing() {
        brushSettings.toggleSmoothing();
    }

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

    public List<Stroke> getStrokes() { return strokes; }
    public Stroke getCurrentStroke() { return currentStroke; }
    public BrushSettings getBrushSettings() { return brushSettings; }
    public DrawingHistory getHistory() { return history; }
    public ColorPicker getColorPicker() { return colorPicker; }
    public boolean isDrawing() { return isDrawing; }
    public int getStrokeCount() { return strokes.size(); }
}