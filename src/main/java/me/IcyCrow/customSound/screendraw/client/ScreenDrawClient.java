package me.IcyCrow.customSound.screendraw.client;

import me.IcyCrow.customSound.screendraw.drawing.DrawingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ScreenDrawClient implements ClientModInitializer {

    private static KeyBinding openDrawingScreenKey;

    @Override
    public void onInitializeClient() {
        openDrawingScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.screendraw.open_drawing",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.screendraw.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient minecraftClient) {
        while (openDrawingScreenKey.wasPressed()) {
            if (minecraftClient.player != null) {
                minecraftClient.setScreen(new DrawingScreen());
            }
        }
    }
}