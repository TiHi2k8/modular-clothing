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

import java.util.Arrays;

import java.io.IOException;

/**
 * Sub-screen opened by right-clicking a clothing slot in ClothingGui.
 * Lets the player set per-slot Scale (uniform or per-axis X/Y/Z) and XYZ position offsets.
 * The player preview updates live as values are typed.
 * "Apply" sends PacketUpdateClothingTransform; "Cancel" / Escape restores the original values.
 * "Reset" resets all fields to defaults (scale=1, offsets=0).
 * The toggle button next to "Scale" switches between uniform scale and per-axis X/Y/Z scale.
 */
@SideOnly(Side.CLIENT)
public class GuiClothingTransform extends GuiScreen {

    private static final int BTN_APPLY        = 0;
    private static final int BTN_CANCEL       = 1;
    private static final int BTN_RESET        = 2;
    private static final int BTN_SCALE_TOGGLE = 3;
    private static final int BTN_PRESETS      = 4;

    private final ClothingGui parent;
    private final int slotIndex;
    private final int layer;

    // Text fields — scaleY/Z are null in uniform mode
    private GuiTextField fieldScaleX;
    private GuiTextField fieldScaleY;
    private GuiTextField fieldScaleZ;
    private GuiTextField fieldOffX;
    private GuiTextField fieldOffY;
    private GuiTextField fieldOffZ;

    private String errorMessage = "";

    // Per-axis mode toggle — static so it persists across GUI opens (better UX)
    private static boolean perAxisMode = false;

    // Whether initGui has run at least once (for preserving field values on reinit)
    private boolean initialized = false;

    // Cached field texts — survive initGui() reinit when toggling modes
    private String cScaleX = "1.0000", cScaleY = "1.0000", cScaleZ = "1.0000";
    private String cOffX   = "0.0000", cOffY   = "0.0000", cOffZ   = "0.0000";

    /** Saved on open so Cancel / Escape can undo any live-preview changes. */
    private float[] originalTransform;

    public GuiClothingTransform(ClothingGui parent, int slotIndex, int layer) {
        this.parent    = parent;
        this.slotIndex = slotIndex;
        this.layer     = layer;
    }

    @Override
    public void initGui() {
        // Save current field values before wiping (only if already initialized)
        if (initialized) {
            saveFieldValues();
        }

        // First open: load from capability and save original
        if (!initialized) {
            float[] t = readCurrentTransform();
            originalTransform = t.clone();
            cScaleX = String.format("%.4f", t[0]);
            cScaleY = String.format("%.4f", t[1]);
            cScaleZ = String.format("%.4f", t[2]);
            cOffX   = String.format("%.4f", t[3]);
            cOffY   = String.format("%.4f", t[4]);
            cOffZ   = String.format("%.4f", t[5]);
            initialized = true;
        }

        super.initGui();
        buildLayout();
    }

    private void saveFieldValues() {
        if (fieldScaleX != null) cScaleX = fieldScaleX.getText();
        if (fieldScaleY != null) cScaleY = fieldScaleY.getText();
        if (fieldScaleZ != null) cScaleZ = fieldScaleZ.getText();
        if (fieldOffX   != null) cOffX   = fieldOffX.getText();
        if (fieldOffY   != null) cOffY   = fieldOffY.getText();
        if (fieldOffZ   != null) cOffZ   = fieldOffZ.getText();
    }

    /**
     * Builds buttons and text fields for the current mode.
     * Called from initGui() on first open and from the toggle handler on mode switch.
     * Does NOT call super.initGui() — avoids the ConcurrentModificationException that
     * would occur if the buttonList is cleared while Forge's action-performed loop is
     * still iterating it.
     */
    private void buildLayout() {
        int cx     = this.width / 2;
        int startY = this.height / 2 - 70;
        int fx     = cx + 20; // field center-X (fields are 90px wide, centered here)

        // Toggle button above the scale field
        this.buttonList.add(new GuiButton(BTN_SCALE_TOGGLE,
                fx - 45, startY - 17, 90, 14,
                perAxisMode ? "Scale: All" : "Scale: XYZ"));

        if (!perAxisMode) {
            // Uniform scale: one scale field + three offset fields
            fieldScaleX = makeField(0, fx, startY,      cScaleX);
            fieldScaleY = null;
            fieldScaleZ = null;
            fieldOffX   = makeField(3, fx, startY + 30, cOffX);
            fieldOffY   = makeField(4, fx, startY + 60, cOffY);
            fieldOffZ   = makeField(5, fx, startY + 90, cOffZ);
            fieldScaleX.setFocused(true);

            int by = startY + 120;
            this.buttonList.add(new GuiButton(BTN_APPLY,   fx - 45, by,      42, 20, "Apply"));
            this.buttonList.add(new GuiButton(BTN_RESET,   fx +  2, by,      42, 20, "Reset"));
            this.buttonList.add(new GuiButton(BTN_CANCEL,  fx + 49, by,      42, 20, "Cancel"));
            this.buttonList.add(new GuiButton(BTN_PRESETS, fx - 45, by + 24, 134, 20, "Presets..."));
        } else {
            // Per-axis scale: three scale fields + three offset fields
            fieldScaleX = makeField(0, fx, startY,       cScaleX);
            fieldScaleY = makeField(1, fx, startY + 22,  cScaleY);
            fieldScaleZ = makeField(2, fx, startY + 44,  cScaleZ);
            fieldOffX   = makeField(3, fx, startY + 70,  cOffX);
            fieldOffY   = makeField(4, fx, startY + 92,  cOffY);
            fieldOffZ   = makeField(5, fx, startY + 114, cOffZ);
            fieldScaleX.setFocused(true);

            int by = startY + 138;
            this.buttonList.add(new GuiButton(BTN_APPLY,   fx - 45, by,      42, 20, "Apply"));
            this.buttonList.add(new GuiButton(BTN_RESET,   fx +  2, by,      42, 20, "Reset"));
            this.buttonList.add(new GuiButton(BTN_CANCEL,  fx + 49, by,      42, 20, "Cancel"));
            this.buttonList.add(new GuiButton(BTN_PRESETS, fx - 45, by + 24, 134, 20, "Presets..."));
        }
    }

    private GuiTextField makeField(int id, int cx, int y, String text) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, cx - 45, y, 90, 20);
        field.setMaxStringLength(16);
        field.setText(text);
        return field;
    }

    private float[] readCurrentTransform() {
        IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (inv != null) return inv.getSlotTransform(layer, slotIndex);
        return new float[]{1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
    }

    /** Parse fields and push into local capability for live preview. Silently ignores bad input. */
    private void tryApplyLivePreview() {
        try {
            float sx = Float.parseFloat(fieldScaleX.getText().trim());
            float sy = (perAxisMode && fieldScaleY != null) ? Float.parseFloat(fieldScaleY.getText().trim()) : sx;
            float sz = (perAxisMode && fieldScaleZ != null) ? Float.parseFloat(fieldScaleZ.getText().trim()) : sx;
            float ox = Float.parseFloat(fieldOffX.getText().trim());
            float oy = Float.parseFloat(fieldOffY.getText().trim());
            float oz = Float.parseFloat(fieldOffZ.getText().trim());
            IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            if (inv != null) inv.setSlotTransform(layer, slotIndex, sx, sy, sz, ox, oy, oz);
        } catch (NumberFormatException ignored) {}
    }

    /** Write the saved original transform back to the local capability (Cancel / Escape). */
    private void restoreOriginalTransform() {
        if (originalTransform == null) return;
        IClothingInventory inv = this.mc.player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (inv != null) {
            inv.setSlotTransform(layer, slotIndex,
                    originalTransform[0], originalTransform[1], originalTransform[2],
                    originalTransform[3], originalTransform[4], originalTransform[5]);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_PRESETS) {
            saveFieldValues();
            try {
                float sx = Float.parseFloat(cScaleX.trim());
                float sy = perAxisMode ? Float.parseFloat(cScaleY.trim()) : sx;
                float sz = perAxisMode ? Float.parseFloat(cScaleZ.trim()) : sx;
                float ox = Float.parseFloat(cOffX.trim());
                float oy = Float.parseFloat(cOffY.trim());
                float oz = Float.parseFloat(cOffZ.trim());
                this.mc.displayGuiScreen(new GuiClothingPresets(this, perAxisMode, sx, sy, sz, ox, oy, oz));
            } catch (NumberFormatException e) {
                errorMessage = "Fix values before opening presets";
            }
            return;
        }

        if (button.id == BTN_SCALE_TOGGLE) {
            saveFieldValues();
            perAxisMode = !perAxisMode;
            if (!perAxisMode) {
                // Switching to uniform: sync Y/Z to X so no unexpected jump
                cScaleY = cScaleX;
                cScaleZ = cScaleX;
            }
            // Rebuild buttons/fields without calling super.initGui() to avoid
            // ConcurrentModificationException from clearing buttonList mid-iteration.
            this.buttonList.clear();
            buildLayout();
            return;
        }

        if (button.id == BTN_RESET) {
            fieldScaleX.setText("1.0000");
            if (fieldScaleY != null) fieldScaleY.setText("1.0000");
            if (fieldScaleZ != null) fieldScaleZ.setText("1.0000");
            fieldOffX.setText("0.0000");
            fieldOffY.setText("0.0000");
            fieldOffZ.setText("0.0000");
            return;
        }

        if (button.id == BTN_APPLY) {
            try {
                float sx = Float.parseFloat(fieldScaleX.getText().trim());
                float sy = (perAxisMode && fieldScaleY != null) ? Float.parseFloat(fieldScaleY.getText().trim()) : sx;
                float sz = (perAxisMode && fieldScaleZ != null) ? Float.parseFloat(fieldScaleZ.getText().trim()) : sx;
                float ox = Float.parseFloat(fieldOffX.getText().trim());
                float oy = Float.parseFloat(fieldOffY.getText().trim());
                float oz = Float.parseFloat(fieldOffZ.getText().trim());
                errorMessage = "";
                ClothingNetworkHandler.INSTANCE.sendToServer(
                        new PacketUpdateClothingTransform(layer, slotIndex, sx, sy, sz, ox, oy, oz));
                this.mc.displayGuiScreen(parent);
            } catch (NumberFormatException e) {
                errorMessage = "Invalid number — use decimal format (e.g. 1.0)";
            }
            return;
        }

        // BTN_CANCEL
        restoreOriginalTransform();
        this.mc.displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        tryApplyLivePreview();

        this.drawDefaultBackground();

        int cx     = this.width / 2;
        int startY = this.height / 2 - 70;

        // Player preview on the left side
        int previewX = cx - 120;
        int previewY = this.height / 2 + 20;
        GuiInventory.drawEntityOnScreen(previewX, previewY, 40,
                (float) previewX - mouseX,
                (float) (previewY - 50) - mouseY,
                this.mc.player);

        // Panel title
        String title = "Transform  (Slot " + (slotIndex + 1) + ", Layer " + (layer + 1) + ")";
        this.fontRenderer.drawStringWithShadow(title,
                cx - this.fontRenderer.getStringWidth(title) / 2, startY - 30, 0xFFFFFF);

        // Labels
        if (!perAxisMode) {
            this.fontRenderer.drawStringWithShadow("Scale:",    cx - 30, startY +  5, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("X Offset:", cx - 30, startY + 35, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Y Offset:", cx - 30, startY + 65, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Z Offset:", cx - 30, startY + 95, 0xAAAAAA);
        } else {
            this.fontRenderer.drawStringWithShadow("Scale X:",  cx - 30, startY +  5, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Scale Y:",  cx - 30, startY + 27, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Scale Z:",  cx - 30, startY + 49, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("X Offset:", cx - 30, startY + 75, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Y Offset:", cx - 30, startY + 97, 0xAAAAAA);
            this.fontRenderer.drawStringWithShadow("Z Offset:", cx - 30, startY + 119, 0xAAAAAA);
        }

        // Error message
        if (!errorMessage.isEmpty()) {
            this.fontRenderer.drawStringWithShadow(errorMessage,
                    cx - this.fontRenderer.getStringWidth(errorMessage) / 2,
                    startY + (perAxisMode ? 162 : 148), 0xFF5555);
        }

        // Draw active fields
        fieldScaleX.drawTextBox();
        if (perAxisMode && fieldScaleY != null) fieldScaleY.drawTextBox();
        if (perAxisMode && fieldScaleZ != null) fieldScaleZ.drawTextBox();
        fieldOffX.drawTextBox();
        fieldOffY.drawTextBox();
        fieldOffZ.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // Escape — restore and go back
            restoreOriginalTransform();
            this.mc.displayGuiScreen(parent);
            return;
        }
        fieldScaleX.textboxKeyTyped(typedChar, keyCode);
        if (perAxisMode && fieldScaleY != null) fieldScaleY.textboxKeyTyped(typedChar, keyCode);
        if (perAxisMode && fieldScaleZ != null) fieldScaleZ.textboxKeyTyped(typedChar, keyCode);
        fieldOffX.textboxKeyTyped(typedChar, keyCode);
        fieldOffY.textboxKeyTyped(typedChar, keyCode);
        fieldOffZ.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        fieldScaleX.mouseClicked(mouseX, mouseY, mouseButton);
        if (perAxisMode && fieldScaleY != null) fieldScaleY.mouseClicked(mouseX, mouseY, mouseButton);
        if (perAxisMode && fieldScaleZ != null) fieldScaleZ.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffX.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffY.mouseClicked(mouseX, mouseY, mouseButton);
        fieldOffZ.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Called by GuiClothingPresets when the player clicks "Load" on a preset.
     * Fills all fields and switches to per-axis mode if the scale values differ.
     */
    public void applyPreset(boolean perAxis, float sx, float sy, float sz, float ox, float oy, float oz) {
        perAxisMode = perAxis;
        cScaleX = String.format("%.4f", sx);
        cScaleY = String.format("%.4f", sy);
        cScaleZ = String.format("%.4f", sz);
        cOffX   = String.format("%.4f", ox);
        cOffY   = String.format("%.4f", oy);
        cOffZ   = String.format("%.4f", oz);
        this.buttonList.clear();
        buildLayout();
        tryApplyLivePreview();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
