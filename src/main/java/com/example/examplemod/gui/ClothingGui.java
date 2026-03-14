package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketChangeClothingLayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class ClothingGui extends GuiContainer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ExampleMod.MODID, "textures/gui/clothing_gui.png");

    /** Max layer index (0-based), mirrors the 10-layer cap in ClothingInventory. */
    private static final int MAX_LAYER_INDEX = 9;

    /** Remembered across GUI open/close (client-side only). */
    private static int lastSelectedLayer = 0;

    private float oldMouseX;
    private float oldMouseY;

    public ClothingGui(ClothingContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        int i = this.guiLeft;
        int j = this.guiTop;

        // Buttons placed directly above the player entity preview.
        // Player preview is drawn at (i+51, j+75) with scale 30.
        // We put the two buttons centred on x=51, at y = j+5.
        this.buttonList.add(new GuiButton(1, i + 28, j + 5, 20, 20, "-"));
        this.buttonList.add(new GuiButton(2, i + 52, j + 5, 20, 20, "+"));

        // Task 6: restore the last selected layer when re-opening
        if (lastSelectedLayer > 0) {
            IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            int maxExisting = (inv != null) ? inv.getLayerCount() - 1 : 0;
            int targetLayer = Math.min(lastSelectedLayer, Math.min(maxExisting, MAX_LAYER_INDEX));
            if (targetLayer > 0) {
                ((ClothingContainer) this.inventorySlots).setCurrentLayer(targetLayer);
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(targetLayer));
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        int currentLayer = container.getCurrentLayer();

        if (button.id == 1) { // [-] Previous layer
            if (currentLayer > 0) {
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer - 1));
                container.setCurrentLayer(currentLayer - 1);
            }
        } else if (button.id == 2) { // [+] Next layer — capped at MAX_LAYER_INDEX
            if (currentLayer < MAX_LAYER_INDEX) {
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer + 1));
                container.setCurrentLayer(currentLayer + 1);
            }
        }
    }

    /**
     * Task 3: Intercept right-click on any of the 8 clothing slots (indices 0-7)
     * and open the transform sub-screen for that slot.
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1) { // right-click
            for (int slotIdx = 0; slotIdx < 8; slotIdx++) {
                Slot slot = this.inventorySlots.getSlot(slotIdx);
                if (isMouseOverSlot(slot, mouseX, mouseY) && slot instanceof ClothingInventorySlotHandler) {
                    int capSlot = ((ClothingInventorySlotHandler) slot).getCapabilitySlotIndex();
                    ClothingContainer container = (ClothingContainer) this.inventorySlots;
                    this.mc.displayGuiScreen(new GuiClothingTransform(this, capSlot, container.getCurrentLayer()));
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY) {
        int sx = this.guiLeft + slot.xPos;
        int sy = this.guiTop  + slot.yPos;
        return mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16;
    }

    @Override
    public void onGuiClosed() {
        // Task 6: save the layer for the next time this GUI is opened
        lastSelectedLayer = ((ClothingContainer) this.inventorySlots).getCurrentLayer();
        super.onGuiClosed();
    }

    // -------------------------------------------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.oldMouseX = (float) mouseX;
        this.oldMouseY = (float) mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);

        // Hint drawn below the GUI panel so it never overlaps inventory slots
        String hint = "Right-click slot to adjust transform";
        int hintWidth = this.fontRenderer.getStringWidth(hint);
        this.fontRenderer.drawString(hint, (this.width / 2) - hintWidth / 2, this.guiTop + this.ySize + 4, 0x888888);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        // Layer label drawn just below the navigation buttons (relative coords)
        String layerText = "Layer " + (container.getCurrentLayer() + 1);
        int textWidth = this.fontRenderer.getStringWidth(layerText);
        this.fontRenderer.drawString(layerText, 51 - textWidth / 2, 28, 4210752);

    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        // Player preview centred at x=51, feet at y=75
        GuiInventory.drawEntityOnScreen(i + 51, j + 75, 30,
                (float)(i + 51) - this.oldMouseX,
                (float)(j + 75 - 50) - this.oldMouseY,
                this.mc.player);
    }
}
