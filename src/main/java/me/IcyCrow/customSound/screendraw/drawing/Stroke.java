package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.List;

public class Stroke {
    private final List<DrawPoint> points;
    private boolean completed;

    public Stroke() {
        this.points = new ArrayList<>();
        this.completed = false;
    }

    public Stroke(List<DrawPoint> points) {
        this.points = new ArrayList<>(points);
        this.completed = true;
    }

    public void addPoint(DrawPoint point) {
        if (!completed) {
            points.add(point);
        }
    }

    public void addPoints(List<DrawPoint> newPoints) {
        if (!completed) {
            points.addAll(newPoints);
        }
    }

    public void complete() {
        completed = true;
    }

    public void clear() {
        if (!completed) {
            points.clear();
        }
    }

    public void replacePoints(List<DrawPoint> newPoints) {
        if (!completed) {
            points.clear();
            points.addAll(newPoints);
        }
    }

    public static List<DrawPoint> interpolatePoints(int x1, int y1, int x2, int y2, int color, float size) {
        List<DrawPoint> interpolated = new ArrayList<>();

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(Math.max(dx, dy), 1);

        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            interpolated.add(new DrawPoint(x, y, color, size));
        }

        return interpolated;
    }

    public List<DrawPoint> getPoints() { return new ArrayList<>(points); }
    public int getPointCount() { return points.size(); }
    public boolean isCompleted() { return completed; }
    public boolean isEmpty() { return points.isEmpty(); }

    public DrawPoint getLastPoint() {
        return points.isEmpty() ? null : points.get(points.size() - 1);
    }

    public DrawPoint getFirstPoint() {
        return points.isEmpty() ? null : points.get(0);
    }

    public Stroke copy() {
        Stroke copy = new Stroke();
        copy.points.addAll(this.points);
        copy.completed = this.completed;
        return copy;
    }
}