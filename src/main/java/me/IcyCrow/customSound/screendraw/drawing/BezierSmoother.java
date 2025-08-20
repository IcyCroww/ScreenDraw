package me.IcyCrow.customSound.screendraw.drawing;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для сглаживания штрихов с помощью кривых Безье
 */
public class BezierSmoother {
    private static final int DEFAULT_SEGMENTS = 30;

    /**
     * Применяет сглаживание Безье к штриху
     */
    public static Stroke smoothStroke(List<Point> controlPoints, BrushSettings brushSettings) {
        if (controlPoints.size() < 3) {
            // Конвертируем Point в DrawPoint для коротких штрихов
            Stroke stroke = new Stroke();
            for (Point point : controlPoints) {
                stroke.addPoint(new DrawPoint((int) point.x(), (int) point.y(),
                        brushSettings.getColor(), brushSettings.getLineWidth()));
            }
            return stroke;
        }

        List<Point> smoothedPoints = createBezierCurve(controlPoints);
        Stroke smoothedStroke = new Stroke();

        if (!smoothedPoints.isEmpty()) {
            // Добавляем первую точку
            Point firstPoint = smoothedPoints.getFirst();
            DrawPoint lastDrawPoint = new DrawPoint((int) firstPoint.x(), (int) firstPoint.y(),
                    brushSettings.getColor(), brushSettings.getLineWidth());
            smoothedStroke.addPoint(lastDrawPoint);

            // Интерполируем между точками кривой
            for (int i = 1; i < smoothedPoints.size(); i++) {
                Point currentPoint = smoothedPoints.get(i);
                int newX = (int) currentPoint.x();
                int newY = (int) currentPoint.y();

                List<DrawPoint> interpolatedPoints = Stroke.interpolatePoints(
                        lastDrawPoint.x(), lastDrawPoint.y(), newX, newY,
                        brushSettings.getColor(), brushSettings.getLineWidth()
                );

                smoothedStroke.addPoints(interpolatedPoints);
                lastDrawPoint = new DrawPoint(newX, newY, brushSettings.getColor(), brushSettings.getLineWidth());
            }
        }

        return smoothedStroke;
    }

    /**
     * Создает кривую Безье из контрольных точек
     */
    private static List<Point> createBezierCurve(List<Point> controlPoints) {
        List<Point> result = new ArrayList<>();

        if (controlPoints.size() < 3) {
            return new ArrayList<>(controlPoints);
        }

        // Группируем точки по тройкам для создания квадратичных кривых Безье
        for (int i = 0; i < controlPoints.size() - 2; i += 2) {
            Point p0 = controlPoints.get(i);
            Point p1 = controlPoints.get(Math.min(i + 1, controlPoints.size() - 1));
            Point p2 = controlPoints.get(Math.min(i + 2, controlPoints.size() - 1));

            List<Point> curveSegment = generateQuadraticBezier(p0, p1, p2);
            result.addAll(curveSegment);
        }

        return result;
    }

    /**
     * Генерирует квадратичную кривую Безье
     */
    private static List<Point> generateQuadraticBezier(Point p0, Point p1, Point p2) {
        List<Point> points = new ArrayList<>();

        for (int i = 0; i <= BezierSmoother.DEFAULT_SEGMENTS; i++) {
            float t = (float) i / BezierSmoother.DEFAULT_SEGMENTS;

            // Квадратичная формула Безье: B(t) = (1-t)²P₀ + 2(1-t)tP₁ + t²P₂
            float x = (1 - t) * (1 - t) * p0.x() + 2 * (1 - t) * t * p1.x() + t * t * p2.x();
            float y = (1 - t) * (1 - t) * p0.y() + 2 * (1 - t) * t * p1.y() + t * t * p2.y();

            points.add(new Point(x, y));
        }

        return points;
    }
}