package com.example.examplemod.client;

import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketOpenClothingGUI;
import com.example.examplemod.render.ClothingRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Map;

public class ClientEventHandler {
    private boolean layersInitialized = false;

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (!layersInitialized) {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            for (RenderPlayer render : skinMap.values()) {
                render.addLayer(new ClothingRenderLayer(render));
            }
            layersInitialized = true;
        }
    }

    /**
     * Right-clicking on any vanilla armor slot in the player inventory opens the
     * clothing mod GUI (same as pressing K).
     */
    @SubscribeEvent
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiInventory)) return;
        if (org.lwjgl.input.Mouse.getEventButton() != 1) return;     // right-click only
        if (!org.lwjgl.input.Mouse.getEventButtonState()) return;      // button pressed, not released

        GuiContainer gui = (GuiContainer) event.getGui();

        // Primary: try hovered-slot reflection
        Slot hovered = getHoveredSlot(gui);
        boolean isArmor;
        if (hovered != null) {
            isArmor = isVanillaArmorSlot(hovered);
        } else {
            // Fallback: check mouse pixel position against the fixed armor-slot areas in GuiInventory
            isArmor = isMouseOverArmorSlot(gui);
        }
        if (!isArmor) return;

        event.setCanceled(true);
        ClothingNetworkHandler.INSTANCE.sendToServer(new PacketOpenClothingGUI());
    }

    // Cache the reflected field to avoid repeated lookups
    private static Field hoveredSlotField = null;

    private static Slot getHoveredSlot(GuiContainer container) {
        try {
            if (hoveredSlotField == null) {
                try {
                    // MCP deobfuscated name (dev environment)
                    hoveredSlotField = GuiContainer.class.getDeclaredField("hoveredSlot");
                } catch (NoSuchFieldException e) {
                    // SRG name for production environment
                    hoveredSlotField = GuiContainer.class.getDeclaredField("field_147006_n");
                }
                hoveredSlotField.setAccessible(true);
            }
            return (Slot) hoveredSlotField.get(container);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isVanillaArmorSlot(Slot slot) {
        if (slot == null) return false;
        // Works in dev environment with MCP mappings
        if (slot.getClass().getSimpleName().equals("SlotArmor")) return true;
        // Fallback: armor slots in vanilla GuiInventory (ContainerPlayer) are at x=8, y=8/26/44/62
        return slot.xPos == 8 && (slot.yPos == 8 || slot.yPos == 26 || slot.yPos == 44 || slot.yPos == 62);
    }

    /**
     * Pixel-level fallback: GuiInventory is always 176x166; the four vanilla armor slots
     * sit at guiLeft+8, guiTop+8/26/44/62, each 16px wide and tall.
     */
    private static boolean isMouseOverArmorSlot(GuiContainer gui) {
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = org.lwjgl.input.Mouse.getX() * gui.width / mc.displayWidth;
        int mouseY = gui.height - org.lwjgl.input.Mouse.getY() * gui.height / mc.displayHeight - 1;
        int guiLeft = (gui.width  - 176) / 2;
        int guiTop  = (gui.height - 166) / 2;
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;
        return relX >= 8 && relX < 24
                && ((relY >= 8  && relY < 24)
                 || (relY >= 26 && relY < 42)
                 || (relY >= 44 && relY < 60)
                 || (relY >= 62 && relY < 78));
    }
}
