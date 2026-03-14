package com.example.examplemod.render;

import com.example.examplemod.capability.ClothingInventorySlot;
import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClothingRenderLayer implements LayerRenderer<AbstractClientPlayer> {
    private final RenderPlayer renderer;
    private final ModelBiped modelLeggings;
    private final ModelBiped modelArmor;

    public ClothingRenderLayer(RenderPlayer renderer) {
        this.renderer = renderer;
        this.modelLeggings = new ModelBiped(0.5F);
        this.modelArmor = new ModelBiped(1.0F);
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        if (inventory == null) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            ClothingInventorySlot slotType = ClothingInventorySlot.fromIndex(i % 8);
            EntityEquipmentSlot vanillaSlot = slotType.getVanillaSlot();

            // Select default model
            ModelBiped defaultModel = vanillaSlot == EntityEquipmentSlot.LEGS ? this.modelLeggings : this.modelArmor;
            ModelBiped model = item.getArmorModel(player, stack, vanillaSlot, defaultModel);
            if (model == null) model = defaultModel;

            java.util.Map<net.minecraft.client.model.ModelRenderer, Boolean> backupState = null;

            try {
                model.setModelAttributes(this.renderer.getMainModel());
                model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

                // Hide all standard biped parts
                model.bipedHead.showModel      = false;
                model.bipedHeadwear.showModel  = false;
                model.bipedBody.showModel      = false;
                model.bipedRightArm.showModel  = false;
                model.bipedLeftArm.showModel   = false;
                model.bipedRightLeg.showModel  = false;
                model.bipedLeftLeg.showModel   = false;

                int layerIndex = i / 8;
                int slotIndex  = i % 8;

                boolean chestArmsMode = (slotType == ClothingInventorySlot.CHEST)
                        && inventory.getChestArmsMode(layerIndex);

                // Show only the standard biped part(s) matching this slot.
                // Mirroring: from the camera's view, bipedLeftArm/Leg appears on screen RIGHT
                // and bipedRightArm/Leg appears on screen LEFT. So RIGHT_* slots use the Left
                // biped field and LEFT_* slots use the Right biped field.
                switch (slotType) {
                    case HEAD:
                        model.bipedHead.showModel     = true;
                        model.bipedHeadwear.showModel = true;
                        break;
                    case CHEST:
                        model.bipedBody.showModel = true;
                        if (chestArmsMode) {
                            model.bipedLeftArm.showModel  = true; // screen right
                            model.bipedRightArm.showModel = true; // screen left
                        }
                        break;
                    case RIGHT_ARM:
                        model.bipedLeftArm.showModel  = true; // screen right = player's left
                        break;
                    case LEFT_ARM:
                        model.bipedRightArm.showModel = true; // screen left = player's right
                        break;
                    case RIGHT_LEG:
                        model.bipedLeftLeg.showModel  = true; // screen right = player's left
                        break;
                    case LEFT_LEG:
                        model.bipedRightLeg.showModel = true; // screen left = player's right
                        break;
                    case RIGHT_FOOT:
                        model.bipedLeftLeg.showModel  = true; // screen right = player's left
                        break;
                    case LEFT_FOOT:
                        model.bipedRightLeg.showModel = true; // screen left = player's right
                        break;
                }

                // DynamX/OBJ custom model support — run after standard biped setup
                backupState = DynamXHelper.updateDynamXModel(model, slotType);

                // Determine texture and render
                boolean isDynamX = DynamXHelper.isDynamXModel(model);

                // DynamX models manage their own textures via the scene graph.
                // For all other models, we need to look up and bind a texture.
                String texturePath = null;
                if (!isDynamX) {
                    texturePath = item.getArmorTexture(stack, player, vanillaSlot, null);
                    if (texturePath == null && item instanceof ItemArmor) {
                        ItemArmor armorItem = (ItemArmor) item;
                        String matName = armorItem.getArmorMaterial().getName();
                        if (!matName.isEmpty()) {
                            texturePath = String.format("%s:textures/models/armor/%s_layer_%d.png",
                                    getModId(item), matName, (vanillaSlot == EntityEquipmentSlot.LEGS ? 2 : 1));
                        }
                    }
                }

                if (texturePath != null || isDynamX) {
                    if (texturePath != null) {
                        this.renderer.bindTexture(new ResourceLocation(texturePath));
                    }

                    // Apply per-slot transform: float[6] = {scaleX, scaleY, scaleZ, offsetX, offsetY, offsetZ}
                    float[] transform = inventory.getSlotTransform(layerIndex, slotIndex);
                    float scaleX = transform[0];
                    float scaleY = transform[1];
                    float scaleZ = transform[2];
                    float ox     = transform[3];
                    float oy     = transform[4];
                    float oz     = transform[5];

                    GlStateManager.pushMatrix();
                    if (ox != 0.0f || oy != 0.0f || oz != 0.0f) {
                        GlStateManager.translate(ox, oy, oz);
                    }
                    if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
                        GlStateManager.scale(scaleX, scaleY, scaleZ);
                    }
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    boolean handledByDynamX = DynamXHelper.renderDynamXArmorPart(
                            model, slotType, chestArmsMode, scale,
                            player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                    if (!handledByDynamX) {
                        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                    }
                    GlStateManager.popMatrix();
                }
            } catch (Exception e) {
                System.err.println("Error rendering clothing layer: " + e.getMessage());
            } finally {
                DynamXHelper.restoreDynamXModel(backupState);
            }
        }
    }

    private String getModId(Item item) {
        ResourceLocation reg = item.getRegistryName();
        return reg != null ? reg.getResourceDomain() : "minecraft";
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
