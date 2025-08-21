package me.IcyCrow.customSound.screendraw.drawing;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.List;

public class StrokeRenderer {

    public static void renderStrokes(List<Stroke> strokes, Stroke currentStroke, MatrixStack matrices) {
        if (strokes.isEmpty() && (currentStroke == null || currentStroke.isEmpty())) {
            return;
        }

        setupRenderState();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Stroke stroke : strokes) {
            if (!stroke.isEmpty()) {
                renderStroke(builder, matrix, stroke.getPoints());
            }
        }

        if (currentStroke != null && !currentStroke.isEmpty()) {
            renderStroke(builder, matrix, currentStroke.getPoints());
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        cleanupRenderState();
    }

    private static void renderStroke(BufferBuilder builder, Matrix4f matrix, List<DrawPoint> points) {
        for (DrawPoint point : points) {
            float x = point.x();
            float y = point.y();
            int color = point.color();
            float pointSize = point.size();

            int alpha = (color >>> 24) & 0xFF;
            int red = (color >>> 16) & 0xFF;
            int green = (color >>> 8) & 0xFF;
            int blue = color & 0xFF;

            float halfSize = pointSize / 2.0f;

            builder.vertex(matrix, x - halfSize, y + halfSize, 0).color(red, green, blue, alpha);
            builder.vertex(matrix, x + halfSize, y + halfSize, 0).color(red, green, blue, alpha);
            builder.vertex(matrix, x + halfSize, y - halfSize, 0).color(red, green, blue, alpha);
            builder.vertex(matrix, x - halfSize, y - halfSize, 0).color(red, green, blue, alpha);
        }
    }

    private static void setupRenderState() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void cleanupRenderState() {
        RenderSystem.disableBlend();
    }
}