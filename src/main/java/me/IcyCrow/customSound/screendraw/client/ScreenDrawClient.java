package me.IcyCrow.customSound.screendraw.client;

import me.IcyCrow.customSound.screendraw.DrawingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ScreenDrawClient implements ClientModInitializer {

    // Создаем кейбинд для открытия экрана рисования
    private static KeyBinding openDrawingScreenKey;

    @Override
    public void onInitializeClient() {
        // Регистрируем кейбинд (по умолчанию клавиша P)
        openDrawingScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.screendraw.open_drawing", // Ключ локализации
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P, // Клавиша P (можешь изменить на любую другую)
                "category.screendraw.general" // Категория в настройках управления
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient minecraftClient) {
        // Проверяем, была ли нажата клавиша для открытия экрана рисования
        while (openDrawingScreenKey.wasPressed()) {
            // Открываем экран рисования только если игрок в игре
            if (minecraftClient.player != null) {
                minecraftClient.setScreen(new DrawingScreen());
            }
        }
    }
}