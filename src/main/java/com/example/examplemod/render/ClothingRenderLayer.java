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
            ClothingInventorySlot slotType = ClothingInventorySlot.fromIndex(i);
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

            try {
                // Sync attributes
                model.setModelAttributes(this.renderer.getMainModel());
                model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

                // Hide all parts first
                model.bipedHead.showModel = false;
                model.bipedHeadwear.showModel = false;
                model.bipedBody.showModel = false;
                model.bipedRightArm.showModel = false;
                model.bipedLeftArm.showModel = false;
                model.bipedRightLeg.showModel = false;
                model.bipedLeftLeg.showModel = false;

                // Enable specific part based on clothing slot
                switch (slotType) {
                    case HEAD:
                        model.bipedHead.showModel = true;
                        model.bipedHeadwear.showModel = true;
                        break;
                    case CHEST:
                        model.bipedBody.showModel = true;
                        break;
                    case RIGHT_ARM:
                        model.bipedRightArm.showModel = true;
                        // For arms, if model has arms, render them.
                        break;
                    case LEFT_ARM:
                        model.bipedLeftArm.showModel = true;
                        break;
                    case RIGHT_LEG:
                        model.bipedRightLeg.showModel = true;
                        break;
                    case LEFT_LEG:
                        // Usually leg armor covers both legs, but we want 1.
                        model.bipedLeftLeg.showModel = true;
                        break;
                    case RIGHT_FOOT:
                        // Feet usually use boots model which is modelArmor (1.0F) or similar?
                        // Actually feet are usually layer 1 (boots).
                        // If model is leggings, legs are used. If model is armor (boots), feet are part of legs?
                        // ModelBiped doesn't have feet. It has legs.
                        // So for feet slot, we rely on texture transparency or model shape.
                        // We enable leg part.
                        model.bipedRightLeg.showModel = true;
                        break;
                    case LEFT_FOOT:
                        model.bipedLeftLeg.showModel = true;
                        break;
                }

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
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                }
            } catch (Exception e) {
                // Log exception safely
                System.err.println("Error rendering clothing layer: " + e.getMessage());
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
