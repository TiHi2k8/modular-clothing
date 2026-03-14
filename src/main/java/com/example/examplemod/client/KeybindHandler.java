package com.example.examplemod.client;

import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketOpenClothingGUI;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeybindHandler {
    public static KeyBinding openGuiKey;

    public static void init() {
        openGuiKey = new KeyBinding("key.examplemod.openclothing", Keyboard.KEY_K, "key.categories.examplemod");
        ClientRegistry.registerKeyBinding(openGuiKey);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGuiKey.isPressed()) {
            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketOpenClothingGUI());
        }
    }
}

