package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DrawingCanvas {
    private final List<Stroke> strokes;
    private Stroke currentStroke;
    private final List<Point> currentBezierPoints;
    private final DrawingHistory history;
    private final BrushSettings brushSettings;
    private ColorPicker colorPicker;

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

        if (isDrawing || colorPicker.isVisible()) {
            return;
        }

        history.saveState(strokes);
        isDrawing = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (brushSettings.isEraserSelected()) {
            currentStroke = null;
            eraseAt(mouseX, mouseY);
            return;
        }

        currentStroke = new Stroke();
        currentBezierPoints.clear();
        currentBezierPoints.add(new Point(mouseX, mouseY));

        currentStroke.addPoint(new DrawPoint(
                mouseX,
                mouseY,
                brushSettings.getEffectiveColor(),
                brushSettings.getLineWidth()
        ));
    }

    public void continueStroke(int mouseX, int mouseY) {
        if (!isDrawing || colorPicker.isVisible()) {
            return;
        }

        if (brushSettings.isEraserSelected()) {
            eraseBetween(lastMouseX, lastMouseY, mouseX, mouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return;
        }

        if (currentStroke != null) {
            currentBezierPoints.add(new Point(mouseX, mouseY));

            if (lastMouseX != -1 && lastMouseY != -1) {
                List<DrawPoint> interpolatedPoints = Stroke.interpolatePoints(
                        lastMouseX, lastMouseY, mouseX, mouseY,
                        brushSettings.getEffectiveColor(), brushSettings.getLineWidth()
                );
                currentStroke.addPoints(interpolatedPoints);
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    public void endStroke() {
        if (!isDrawing) {
            return;
        }

        isDrawing = false;

        if (currentStroke != null) {
            if (brushSettings.isSmoothingEnabled() && currentBezierPoints.size() > 2) {
                currentStroke = BezierSmoother.smoothStroke(currentBezierPoints, brushSettings);
            }

            if (!currentStroke.isEmpty()) {
                currentStroke.complete();
                strokes.add(currentStroke);
            }
        }

        currentStroke = null;
        currentBezierPoints.clear();
        lastMouseX = -1;
        lastMouseY = -1;
    }

    public void toggleColorPicker() {
        colorPicker.toggle();
    }

    public void updateColorPickerPosition(int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        if (colorPicker == null) {
            colorPicker = new ColorPicker(cx, cy);
        } else {
            colorPicker.setPosition(cx, cy);
        }
    }

    public boolean undo() {
        if (!history.canUndo()) {
            return false;
        }
        List<Stroke> newStrokes = history.undo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
        return true;
    }

    public boolean redo() {
        if (!history.canRedo()) {
            return false;
        }
        List<Stroke> newStrokes = history.redo(strokes);
        strokes.clear();
        strokes.addAll(newStrokes);
        return true;
    }

    public boolean clear() {
        if (strokes.isEmpty() && (currentStroke == null || currentStroke.isEmpty())) {
            return false;
        }
        history.saveState(strokes);
        strokes.clear();
        if (currentStroke != null) {
            currentStroke.clear();
        }
        return true;
    }

    public void replaceStrokes(List<Stroke> newStrokes, boolean saveHistory) {
        if (saveHistory) {
            history.saveState(strokes);
        }
        strokes.clear();
        for (Stroke stroke : newStrokes) {
            strokes.add(stroke.copy());
        }
        currentStroke = null;
        currentBezierPoints.clear();
        isDrawing = false;
        lastMouseX = -1;
        lastMouseY = -1;
    }

    public boolean adjustBrushSize(float delta) {
        return brushSettings.adjustSize(delta);
    }

    public boolean adjustOpacity(float delta) {
        return brushSettings.adjustOpacity(delta);
    }

    public void toggleSmoothing() {
        brushSettings.toggleSmoothing();
    }

    public void setToolMode(ToolMode toolMode) {
        brushSettings.setToolMode(toolMode);
    }

    public void toggleToolMode() {
        brushSettings.toggleToolMode();
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

    private void eraseBetween(int x1, int y1, int x2, int y2) {
        List<DrawPoint> points = Stroke.interpolatePoints(
                x1, y1, x2, y2, 0xFFFFFFFF, Math.max(1.0f, brushSettings.getLineWidth())
        );
        for (DrawPoint point : points) {
            eraseAt(point.x(), point.y());
        }
    }

    private void eraseAt(int mouseX, int mouseY) {
        float radius = Math.max(2.0f, brushSettings.getLineWidth());
        Iterator<Stroke> iterator = strokes.iterator();
        while (iterator.hasNext()) {
            Stroke stroke = iterator.next();
            stroke.erasePointsNear(mouseX, mouseY, radius);
            if (stroke.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public List<Stroke> getStrokes() { return strokes; }
    public Stroke getCurrentStroke() { return currentStroke; }
    public BrushSettings getBrushSettings() { return brushSettings; }
    public DrawingHistory getHistory() { return history; }
    public ColorPicker getColorPicker() { return colorPicker; }
    public boolean isDrawing() { return isDrawing; }
    public int getStrokeCount() { return strokes.size(); }
}
