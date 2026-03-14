package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketUpdateClothingTransform;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

/**
 * Sub-screen opened by right-clicking a clothing slot in ClothingGui.
 * Lets the player set per-slot Scale and XYZ position offsets.
 * The player preview updates live as values are typed.
 * "Apply" sends PacketUpdateClothingTransform; "Cancel" / Escape restores the original values.
 */
@SideOnly(Side.CLIENT)
public class GuiClothingTransform extends GuiScreen {

    private static final int BTN_APPLY  = 0;
    private static final int BTN_CANCEL = 1;

    private final ClothingGui parent;
    private final int slotIndex;
    private final int layer;

    private GuiTextField fieldScale;
    private GuiTextField fieldOffsetX;
    private GuiTextField fieldOffsetY;
    private GuiTextField fieldOffsetZ;

    private String errorMessage = "";

    /** Saved on open so Cancel / Escape can undo any live-preview changes. */
    private float[] originalTransform;

    public GuiClothingTransform(ClothingGui parent, int slotIndex, int layer) {
        this.parent    = parent;
        this.slotIndex = slotIndex;
        this.layer     = layer;
    }

    @Override
    public void initGui() {
        float[] transform = readCurrentTransform();
        originalTransform = transform.clone();

        int cx = this.width / 2;
        int startY = this.height / 2 - 70;

        // Shift fields right to leave room for player preview on the left
        int fx = cx + 20;
        fieldScale   = makeField(0, fx, startY,       String.format("%.4f", transform[0]));
        fieldOffsetX = makeField(1, fx, startY + 30,  String.format("%.4f", transform[1]));
        fieldOffsetY = makeField(2, fx, startY + 60,  String.format("%.4f", transform[2]));
        fieldOffsetZ = makeField(3, fx, startY + 90,  String.format("%.4f", transform[3]));

        fieldScale.setFocused(true);

        this.buttonList.add(new GuiButton(BTN_APPLY,  fx - 35, startY + 120, 50, 20, "Apply"));
        this.buttonList.add(new GuiButton(BTN_CANCEL, fx + 25, startY + 120, 50, 20, "Cancel"));
    }

    private GuiTextField makeField(int id, int cx, int y, String defaultText) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, cx - 40, y, 90, 20);
        field.setMaxStringLength(16);
        field.setText(defaultText);
        return field;
    }

    private float[] readCurrentTransform() {
        IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (inv != null) {
            return inv.getSlotTransform(layer, slotIndex);
        }
        return new float[]{1.0f, 0.0f, 0.0f, 0.0f};
    }

    /**
     * Parse current field values and push them into the local capability so the
     * player preview reflects the current input without pressing Apply.
     * Silently ignores unparseable text (the preview just stays at the last valid state).
     */
    private void tryApplyLivePreview() {
        try {
            float scale = Float.parseFloat(fieldScale.getText().trim());
            float ox    = Float.parseFloat(fieldOffsetX.getText().trim());
            float oy    = Float.parseFloat(fieldOffsetY.getText().trim());
            float oz    = Float.parseFloat(fieldOffsetZ.getText().trim());
            IClothingInventory localInv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            if (localInv != null) {
                localInv.setSlotTransform(layer, slotIndex, scale, ox, oy, oz);
            }
        } catch (NumberFormatException ignored) {
            // Leave the preview at its last valid state
        }
    }

    /** Write the saved original transform back to the local capability (Cancel / Escape). */
    private void restoreOriginalTransform() {
        if (originalTransform == null) return;
        IClothingInventory localInv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (localInv != null) {
            localInv.setSlotTransform(layer, slotIndex,
                    originalTransform[0], originalTransform[1],
                    originalTransform[2], originalTransform[3]);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_APPLY) {
            try {
                float scale = Float.parseFloat(fieldScale.getText().trim());
                float ox    = Float.parseFloat(fieldOffsetX.getText().trim());
                float oy    = Float.parseFloat(fieldOffsetY.getText().trim());
                float oz    = Float.parseFloat(fieldOffsetZ.getText().trim());
                errorMessage = "";
                // Local capability already updated by live preview; just sync to server
                ClothingNetworkHandler.INSTANCE.sendToServer(
                        new PacketUpdateClothingTransform(layer, slotIndex, scale, ox, oy, oz));
                this.mc.displayGuiScreen(parent);
                return;
            } catch (NumberFormatException e) {
                errorMessage = "Invalid number — use decimal format (e.g. 1.0)";
                return;
            }
        }
        // Cancel — undo live-preview changes and go back
        restoreOriginalTransform();
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Push live-preview transform into local capability every frame
        tryApplyLivePreview();

        this.drawDefaultBackground();

        int cx = this.width / 2;
        int startY = this.height / 2 - 70;

        // Player preview on the left side
        int previewX = cx - 120;
        int previewY = this.height / 2 + 20;
        GuiInventory.drawEntityOnScreen(previewX, previewY, 40,
                (float) previewX - mouseX,
                (float) (previewY - 50) - mouseY,
                this.mc.player);

        // Panel title
        String title = "Clothing Transform  (Slot " + (slotIndex + 1) + ", Layer " + (layer + 1) + ")";
        this.fontRenderer.drawStringWithShadow(title, cx - this.fontRenderer.getStringWidth(title) / 2, startY - 22, 0xFFFFFF);

        // Labels (shifted right to make room for preview)
        this.fontRenderer.drawStringWithShadow("Scale:",    cx - 30, startY +  5, 0xAAAAAA);
        this.fontRenderer.drawStringWithShadow("X Offset:", cx - 30, startY + 35, 0xAAAAAA);
        this.fontRenderer.drawStringWithShadow("Y Offset:", cx - 30, startY + 65, 0xAAAAAA);
        this.fontRenderer.drawStringWithShadow("Z Offset:", cx - 30, startY + 95, 0xAAAAAA);

        // Error
        if (!errorMessage.isEmpty()) {
            this.fontRenderer.drawStringWithShadow(errorMessage, cx - this.fontRenderer.getStringWidth(errorMessage) / 2, startY + 148, 0xFF5555);
        }

        fieldScale.drawTextBox();
        fieldOffsetX.drawTextBox();
        fieldOffsetY.drawTextBox();
        fieldOffsetZ.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // Escape — restore and go back
            restoreOriginalTransform();
            this.mc.displayGuiScreen(parent);
            return;
        }
        fieldScale.textboxKeyTyped(typedChar, keyCode);
        fieldOffsetX.textboxKeyTyped(typedChar, keyCode);
        fieldOffsetY.textboxKeyTyped(typedChar, keyCode);
        fieldOffsetZ.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldScale.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffsetX.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffsetY.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffsetZ.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
