package com.example.examplemod;

import com.example.examplemod.network.ModNetworkHandler;
import com.example.examplemod.network.PacketOpenClothingGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

/**
 * Handles keybind registration and input for the clothing GUI.
 * Default key: K
 */
public class KeyInputHandler {

    public static final KeyBinding KEY_OPEN_CLOTHING = new KeyBinding(
            "key.examplemod.openclothing",
            Keyboard.KEY_K,
            "key.categories.examplemod"
    );

    public static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(KEY_OPEN_CLOTHING);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (KEY_OPEN_CLOTHING.isPressed()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null) {
                // Send packet to server to open the clothing GUI
                ModNetworkHandler.INSTANCE.sendToServer(new PacketOpenClothingGUI());
            }
        }
    }
}

