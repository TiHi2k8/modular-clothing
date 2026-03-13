package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketChangeClothingLayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class ClothingGui extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ExampleMod.MODID, "textures/gui/clothing_gui.png");
    private float oldMouseX;
    private float oldMouseY;

    public ClothingGui(ClothingContainer container) {
        super(container);
    }

    @Override
    public void initGui() {
        super.initGui();
        // Add buttons
        int i = this.guiLeft;
        int j = this.guiTop;

        // [-] Button
        this.buttonList.add(new GuiButton(1, i + 80, j + 80, 20, 20, "-"));
        // [+] Button
        this.buttonList.add(new GuiButton(2, i + 140, j + 80, 20, 20, "+"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        ClothingContainer container = (ClothingContainer) this.inventorySlots;
        int currentLayer = container.getCurrentLayer();

        if (button.id == 1) { // Prev
            if (currentLayer > 0) {
                ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer - 1));
                container.setCurrentLayer(currentLayer - 1);
            }
        } else if (button.id == 2) { // Next
            // Allow going to next layer (creates if needed on server)
            ClothingNetworkHandler.INSTANCE.sendToServer(new PacketChangeClothingLayer(currentLayer + 1));
            container.setCurrentLayer(currentLayer + 1);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.oldMouseX = (float)mouseX;
        this.oldMouseY = (float)mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
         ClothingContainer container = (ClothingContainer) this.inventorySlots;
         // Draw layer index
         String layerText = "Layer " + (container.getCurrentLayer() + 1);
         int width = this.fontRenderer.getStringWidth(layerText);
         this.fontRenderer.drawString(layerText, 120 - width / 2, 85, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);

        GuiInventory.drawEntityOnScreen(i + 51, j + 75, 30, (float)(i + 51) - this.oldMouseX, (float)(j + 75 - 50) - this.oldMouseY, this.mc.player);
    }
}
