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
            // Check if item is valid armor for the slot (or generally valid armor)
            // We use the slot's target vanilla slot for checking validity and model retrieval
            ClothingInventorySlot slotType = ClothingInventorySlot.fromIndex(i % 8);
            EntityEquipmentSlot vanillaSlot = slotType.getVanillaSlot();

            // if (!item.isValidArmor(stack, vanillaSlot, player)) continue;

            // Select default model
            ModelBiped defaultModel = vanillaSlot == EntityEquipmentSlot.LEGS ? this.modelLeggings : this.modelArmor;

            // Get model with hook
            ModelBiped model = item.getArmorModel(player, stack, vanillaSlot, defaultModel);

            // If mod returns null, use default
            if (model == null) {
                model = defaultModel;
            }

            java.util.Map<net.minecraft.client.model.ModelRenderer, Boolean> backupState = null;

            try {
                // Sync attributes — this copies showModel flags from the main player model,
                // so it must come before our custom visibility setup
                model.setModelAttributes(this.renderer.getMainModel());
                model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

                // Hide all standard biped parts
                model.bipedHead.showModel = false;
                model.bipedHeadwear.showModel = false;
                model.bipedBody.showModel = false;
                model.bipedRightArm.showModel = false;
                model.bipedLeftArm.showModel = false;
                model.bipedRightLeg.showModel = false;
                model.bipedLeftLeg.showModel = false;

                // Show only the standard biped part matching this slot
                switch (slotType) {
                    case HEAD:
                        model.bipedHead.showModel = true;
                        model.bipedHeadwear.showModel = true;
                        break;
                    case CHEST:
                        model.bipedBody.showModel = true;
                        model.bipedRightArm.showModel = true;
                        model.bipedLeftArm.showModel = true;
                        break;
                    case RIGHT_ARM:
                        model.bipedRightArm.showModel = true;
                        break;
                    case LEFT_ARM:
                        model.bipedLeftArm.showModel = true;
                        break;
                    case RIGHT_LEG:
                        model.bipedRightLeg.showModel = true;
                        break;
                    case LEFT_LEG:
                        model.bipedLeftLeg.showModel = true;
                        break;
                    case RIGHT_FOOT:
                        model.bipedRightLeg.showModel = true;
                        break;
                    case LEFT_FOOT:
                        model.bipedLeftLeg.showModel = true;
                        break;
                }

                // DynamX/OBJ custom model support: run AFTER standard biped setup so it is
                // the final word on custom field visibility (setModelAttributes would have
                // overwritten an earlier DynamXHelper call for standard fields)
                backupState = DynamXHelper.updateDynamXModel(model, slotType);

                // Render
                // Note: type is null for base texture
                String texturePath = item.getArmorTexture(stack, player, vanillaSlot, null);
                if (texturePath == null) {
                    // Fallback to standard texture path if null
                     if (item instanceof ItemArmor) {
                        ItemArmor armorItem = (ItemArmor) item;
                        texturePath = String.format("%s:textures/models/armor/%s_layer_%d.png",
                            getModId(item), armorItem.getArmorMaterial().getName(), (vanillaSlot == EntityEquipmentSlot.LEGS ? 2 : 1));
                    }
                }

                if (texturePath != null) {
                    this.renderer.bindTexture(new ResourceLocation(texturePath));
                    int layerIndex = i / 8;
                    int slotIndex  = i % 8;

                    // Task 3: apply per-slot custom transform (scale + XYZ offsets)
                    float[] transform = inventory.getSlotTransform(layerIndex, slotIndex);
                    float customScale = transform[0];
                    float ox = transform[1];
                    float oy = transform[2];
                    float oz = transform[3];

                    GlStateManager.pushMatrix();
                    if (ox != 0.0f || oy != 0.0f || oz != 0.0f) {
                        GlStateManager.translate(ox, oy, oz);
                    }
                    if (customScale != 1.0f) {
                        GlStateManager.scale(customScale, customScale, customScale);
                    }
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    // For DynamX ModelObjArmor: hide the ArmorRenderer parts that don't belong
                    // to this slot (they check showModel in their own render() override), then
                    // call model.render() normally so the scene-graph / Matrix4f path runs with
                    // the live animation already set by setModelAttributes(mainModel).
                    boolean handledByDynamX = DynamXHelper.renderDynamXArmorPart(
                            model, slotType, scale,
                            player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                    if (!handledByDynamX) {
                        model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                    }
                    GlStateManager.popMatrix();
                }
            } catch (Exception e) {
                // Log exception safely
                System.err.println("Error rendering clothing layer: " + e.getMessage());
            } finally {
                // Restore model state to avoid affecting other renderings of the same item
                DynamXHelper.restoreDynamXModel(backupState);
            }
        }
    }

    // Helper to get mod id
    private String getModId(Item item) {
        ResourceLocation reg = item.getRegistryName();
        return reg != null ? reg.getResourceDomain() : "minecraft";
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
