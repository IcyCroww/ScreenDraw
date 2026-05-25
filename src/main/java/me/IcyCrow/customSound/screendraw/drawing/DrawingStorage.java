package me.IcyCrow.customSound.screendraw.drawing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class DrawingStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE_NAME = "last-drawing.json";
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private DrawingStorage() {
    }

    public static Path save(List<Stroke> strokes, BrushSettings brushSettings, int width, int height) throws IOException {
        Path saveDirectory = getSaveDirectory();
        Files.createDirectories(saveDirectory);

        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("width", width);
        root.addProperty("height", height);
        root.add("brush", writeBrush(brushSettings));
        root.add("colorSlots", writeColorSlots(brushSettings));
        root.add("strokes", writeStrokes(strokes));

        Path savePath = saveDirectory.resolve(SAVE_FILE_NAME);
        try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        return savePath;
    }

    public static SavedDrawing load() throws IOException {
        Path savePath = getSaveDirectory().resolve(SAVE_FILE_NAME);
        if (!Files.exists(savePath)) {
            return null;
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonObject brush = getObject(root, "brush");
        int[] colorSlots = readColorSlots(root.getAsJsonArray("colorSlots"));

        return new SavedDrawing(
                readStrokes(root.getAsJsonArray("strokes")),
                getInt(root, "width", 0),
                getInt(root, "height", 0),
                getInt(brush, "color", 0xFFFFFFFF),
                getFloat(brush, "lineWidth", 2.0f),
                getFloat(brush, "opacity", 1.0f),
                getBoolean(brush, "smoothingEnabled", true),
                readToolMode(brush),
                colorSlots,
                savePath
        );
    }

    public static Path exportPng(List<Stroke> strokes, int width, int height) throws IOException {
        Path exportDirectory = getSaveDirectory().resolve("exports");
        Files.createDirectories(exportDirectory);

        int imageWidth = Math.max(width, 1);
        int imageHeight = Math.max(height, 1);
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Stroke stroke : strokes) {
                renderStroke(graphics, stroke);
            }
        } finally {
            graphics.dispose();
        }

        String fileName = "screendraw-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".png";
        Path exportPath = exportDirectory.resolve(fileName);
        ImageIO.write(image, "png", exportPath.toFile());
        return exportPath;
    }

    private static void renderStroke(Graphics2D graphics, Stroke stroke) {
        for (DrawPoint point : stroke.getPointsView()) {
            int size = Math.max(1, Math.round(point.size()));
            int x = point.x() - size / 2;
            int y = point.y() - size / 2;
            graphics.setColor(new Color(point.color(), true));
            graphics.fillOval(x, y, size, size);
        }
    }

    private static JsonObject writeBrush(BrushSettings brushSettings) {
        JsonObject brush = new JsonObject();
        brush.addProperty("lineWidth", brushSettings.getLineWidth());
        brush.addProperty("color", brushSettings.getColor());
        brush.addProperty("opacity", brushSettings.getOpacity());
        brush.addProperty("smoothingEnabled", brushSettings.isSmoothingEnabled());
        brush.addProperty("toolMode", brushSettings.getToolMode().name());
        return brush;
    }

    private static JsonArray writeColorSlots(BrushSettings brushSettings) {
        JsonArray slots = new JsonArray();
        for (int color : brushSettings.getColorSlots()) {
            slots.add(color);
        }
        return slots;
    }

    private static JsonArray writeStrokes(List<Stroke> strokes) {
        JsonArray strokeArray = new JsonArray();
        for (Stroke stroke : strokes) {
            JsonObject strokeObject = new JsonObject();
            JsonArray points = new JsonArray();
            for (DrawPoint point : stroke.getPointsView()) {
                JsonObject pointObject = new JsonObject();
                pointObject.addProperty("x", point.x());
                pointObject.addProperty("y", point.y());
                pointObject.addProperty("color", point.color());
                pointObject.addProperty("size", point.size());
                points.add(pointObject);
            }
            strokeObject.add("points", points);
            strokeArray.add(strokeObject);
        }
        return strokeArray;
    }

    private static List<Stroke> readStrokes(JsonArray strokeArray) {
        List<Stroke> strokes = new ArrayList<>();
        if (strokeArray == null) {
            return strokes;
        }

        for (JsonElement strokeElement : strokeArray) {
            JsonObject strokeObject = strokeElement.getAsJsonObject();
            JsonArray pointsArray = strokeObject.getAsJsonArray("points");
            List<DrawPoint> points = new ArrayList<>();
            if (pointsArray != null) {
                for (JsonElement pointElement : pointsArray) {
                    JsonObject pointObject = pointElement.getAsJsonObject();
                    points.add(new DrawPoint(
                            getInt(pointObject, "x", 0),
                            getInt(pointObject, "y", 0),
                            getInt(pointObject, "color", 0xFFFFFFFF),
                            getFloat(pointObject, "size", 2.0f)
                    ));
                }
            }
            if (!points.isEmpty()) {
                strokes.add(Stroke.completed(points));
            }
        }
        return strokes;
    }

    private static int[] readColorSlots(JsonArray slotsArray) {
        if (slotsArray == null) {
            return new int[0];
        }
        int[] slots = new int[slotsArray.size()];
        for (int i = 0; i < slotsArray.size(); i++) {
            slots[i] = slotsArray.get(i).getAsInt();
        }
        return slots;
    }

    private static ToolMode readToolMode(JsonObject brush) {
        String value = getString(brush, "toolMode", ToolMode.BRUSH.name());
        try {
            return ToolMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return ToolMode.BRUSH;
        }
    }

    private static Path getSaveDirectory() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("screendraw");
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(key);
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static float getFloat(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        return object != null && object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) ? object.get(key).getAsString() : fallback;
    }

    public record SavedDrawing(
            List<Stroke> strokes,
            int width,
            int height,
            int color,
            float lineWidth,
            float opacity,
            boolean smoothingEnabled,
            ToolMode toolMode,
            int[] colorSlots,
            Path path
    ) {
    }
}

