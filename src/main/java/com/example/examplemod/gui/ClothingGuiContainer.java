package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side GUI for the modular clothing inventory.
 * Displays 6 body-part slots arranged in a human body layout
 * above the standard player inventory.
 */
@SideOnly(Side.CLIENT)
public class ClothingGuiContainer extends GuiContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(ExampleMod.MODID, "textures/gui/clothing_gui.png");

    public ClothingGuiContainer(Container container) {
        super(container);
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        // Draw title
        String title = "Modular Clothing";
        this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, -12, 0x404040);

        // Draw slot labels
        this.fontRenderer.drawString("H", 84, 12, 0x808080);   // Head hint
        this.fontRenderer.drawString("LA", 50, 34, 0x808080);   // Left Arm
        this.fontRenderer.drawString("C", 84, 34, 0x808080);    // Chest
        this.fontRenderer.drawString("RA", 112, 34, 0x808080);  // Right Arm
        this.fontRenderer.drawString("LL", 56, 56, 0x808080);   // Left Leg
        this.fontRenderer.drawString("RL", 88, 56, 0x808080);   // Right Leg
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }
}

