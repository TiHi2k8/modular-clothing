package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.oldMouseX = (float)mouseX;
        this.oldMouseY = (float)mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
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

