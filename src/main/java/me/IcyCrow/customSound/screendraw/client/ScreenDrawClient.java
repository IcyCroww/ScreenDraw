package me.IcyCrow.customSound.screendraw.client;

import me.IcyCrow.customSound.screendraw.drawing.DrawingScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ScreenDrawClient implements ClientModInitializer {

    private static final KeyMapping.Category SCREEN_DRAW_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("screendraw", "general"));

    private static KeyMapping openDrawingScreenKey;

    @Override
    public void onInitializeClient() {
        openDrawingScreenKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.screendraw.open_drawing",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                SCREEN_DRAW_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft minecraft) {
        while (openDrawingScreenKey.consumeClick()) {
            if (minecraft.player != null) {
                if (minecraft.screen instanceof DrawingScreen) {
                    minecraft.setScreen(null);
                } else {
                    minecraft.setScreen(new DrawingScreen());
                }
            }
        }
    }
}
