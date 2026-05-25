package me.IcyCrow.customSound.screendraw.drawing;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class StrokeRenderer {

    public static void renderStrokes(List<Stroke> strokes, Stroke currentStroke, GuiGraphicsExtractor context) {
        if (strokes.isEmpty() && (currentStroke == null || currentStroke.isEmpty())) {
            return;
        }

        for (Stroke stroke : strokes) {
            if (!stroke.isEmpty()) {
                renderStroke(context, stroke.getPointsView());
            }
        }

        if (currentStroke != null && !currentStroke.isEmpty()) {
            renderStroke(context, currentStroke.getPointsView());
        }
    }

    private static void renderStroke(GuiGraphicsExtractor context, List<DrawPoint> points) {
        for (DrawPoint point : points) {
            int alpha = (point.color() >>> 24) & 0xFF;
            if (alpha == 0) {
                continue;
            }

            float halfSize = point.size() / 2.0f;
            int x1 = Math.round(point.x() - halfSize);
            int y1 = Math.round(point.y() - halfSize);
            int x2 = Math.round(point.x() + halfSize);
            int y2 = Math.round(point.y() + halfSize);

            context.fill(x1, y1, x2, y2, point.color());
        }
    }
}
