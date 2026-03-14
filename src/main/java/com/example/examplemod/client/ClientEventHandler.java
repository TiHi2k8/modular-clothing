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

    // Reflection fields for GuiContainer
    private static Field guiLeftField;
    private static Field guiTopField;

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
        Slot slot = getSlotAtMouse(gui);

        if (slot != null && isVanillaArmorSlot(slot)) {
            event.setCanceled(true);
            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketOpenClothingGUI());
        }
    }

    private static Slot getSlotAtMouse(GuiContainer gui) {
        try {
            if (guiLeftField == null) {
                try {
                    guiLeftField = GuiContainer.class.getDeclaredField("guiLeft");
                    guiLeftField.setAccessible(true);
                    guiTopField = GuiContainer.class.getDeclaredField("guiTop");
                    guiTopField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    guiLeftField = GuiContainer.class.getDeclaredField("field_147003_i");
                    guiLeftField.setAccessible(true);
                    guiTopField = GuiContainer.class.getDeclaredField("field_147009_r");
                    guiTopField.setAccessible(true);
                }
            }

            int guiLeft = guiLeftField.getInt(gui);
            int guiTop = guiTopField.getInt(gui);

            Minecraft mc = Minecraft.getMinecraft();
            int mouseX = org.lwjgl.input.Mouse.getEventX() * gui.width / mc.displayWidth;
            int mouseY = gui.height - org.lwjgl.input.Mouse.getEventY() * gui.height / mc.displayHeight - 1;

            mouseX -= guiLeft;
            mouseY -= guiTop;

            for (Slot slot : gui.inventorySlots.inventorySlots) {
                if (mouseX >= slot.xPos && mouseX < slot.xPos + 16 &&
                    mouseY >= slot.yPos && mouseY < slot.yPos + 16) {
                    return slot;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean isVanillaArmorSlot(Slot slot) {
        if (slot == null) return false;
        // In ContainerPlayer, slots 5, 6, 7, 8 are armor slots (or 36,37,38,39 depending on inventoryPlayer mapping?)
        // ContainerPlayer adds slots:
        // 0-4: Crafting (5 slots)
        // 5-8: Armor (4 slots)
        // 9-35: Inventory (27)
        // 36-44: Hotbar (9)
        // So indices 5, 6, 7, 8 are armor.
        // Also check simplified class name for mod compatibility
        return (slot.getSlotIndex() >= 36 && slot.getSlotIndex() <= 39 && slot.getClass().getSimpleName().contains("Armor")) // InventoryPlayer armor slots are 36-39
            || (slot.slotNumber >= 5 && slot.slotNumber <= 8); // ContainerPlayer slot numbers
    }
}
