package com.example.examplemod.render;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders equipped clothing/armor on specific body limbs.
 * Handles both vanilla ItemArmor and DynamX items uniformly.
 */
@SideOnly(Side.CLIENT)
public class LayerClothing implements LayerRenderer<EntityPlayer> {

    private static final Logger LOGGER = LogManager.getLogger("modular-clothing");
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    private final net.minecraft.client.renderer.entity.RenderPlayer renderer;

    public LayerClothing(net.minecraft.client.renderer.entity.RenderPlayer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityPlayer player, float limbSwing, float limbSwingAmount,
                               float partialTicks, float ageInTicks, float netHeadYaw,
                               float headPitch, float scale) {
        IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
        if (inv == null) return;

        // Render each clothing slot
        for (int slot = 0; slot < IClothingInventory.SLOT_COUNT; slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            try {
                renderClothingSlot(player, stack, slot, limbSwing, limbSwingAmount,
                        partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            } catch (Exception e) {
                LOGGER.debug("Error rendering clothing slot {}: {}", slot, e.getMessage());
            }
        }
    }

    /**
     * Renders clothing/armor with selective body part rendering.
     */
    private void renderClothingSlot(EntityPlayer player, ItemStack stack, int clothingSlot,
                                    float limbSwing, float limbSwingAmount, float partialTicks,
                                    float ageInTicks, float netHeadYaw, float headPitch, float scale) {

        // Get model - works for both ItemArmor and DynamX
        ModelBiped armorModel = getClothingModel(player, stack);
        if (armorModel == null) return;

        // Get texture - works for both ItemArmor and DynamX
        ResourceLocation texture = getClothingTexture(stack);
        if (texture == null) return;

        // Setup animation from player model
        ModelBiped playerModel = renderer.getMainModel();
        armorModel.setModelAttributes(playerModel);
        armorModel.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);

        // Disable ALL body parts
        disableAllParts(armorModel);

        // Enable only required part for this slot
        enablePartForSlot(armorModel, clothingSlot);

        // Bind texture and render
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        armorModel.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();

        // Restore all parts
        enableAllParts(armorModel);
    }

    /**
     * Disable all body parts of the model.
     */
    private void disableAllParts(ModelBiped model) {
        model.bipedHead.showModel = false;
        model.bipedHeadwear.showModel = false;
        model.bipedBody.showModel = false;
        model.bipedRightArm.showModel = false;
        model.bipedLeftArm.showModel = false;
        model.bipedRightLeg.showModel = false;
        model.bipedLeftLeg.showModel = false;
    }

    /**
     * Enable all body parts of the model.
     */
    private void enableAllParts(ModelBiped model) {
        model.bipedHead.showModel = true;
        model.bipedHeadwear.showModel = true;
        model.bipedBody.showModel = true;
        model.bipedRightArm.showModel = true;
        model.bipedLeftArm.showModel = true;
        model.bipedRightLeg.showModel = true;
        model.bipedLeftLeg.showModel = true;
    }

    /**
     * Enable only the part required for the specific clothing slot.
     */
    private void enablePartForSlot(ModelBiped model, int clothingSlot) {
        switch (clothingSlot) {
            case IClothingInventory.SLOT_HEAD:
                model.bipedHead.showModel = true;
                model.bipedHeadwear.showModel = true;
                break;
            case IClothingInventory.SLOT_CHEST:
                model.bipedBody.showModel = true;
                break;
            case IClothingInventory.SLOT_RIGHT_ARM:
                model.bipedRightArm.showModel = true;
                break;
            case IClothingInventory.SLOT_LEFT_ARM:
                model.bipedLeftArm.showModel = true;
                break;
            case IClothingInventory.SLOT_RIGHT_LEG:
            case IClothingInventory.SLOT_RIGHT_FOOT:
                model.bipedRightLeg.showModel = true;
                break;
            case IClothingInventory.SLOT_LEFT_LEG:
            case IClothingInventory.SLOT_LEFT_FOOT:
                model.bipedLeftLeg.showModel = true;
                break;
        }
    }

    /**
     * Get clothing model - handles both ItemArmor and DynamX items.
     */
    private ModelBiped getClothingModel(EntityPlayer player, ItemStack stack) {
        // If it's ItemArmor, use standard method
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            return getItemArmorModel(player, stack, armor.armorType);
        }

        // For DynamX and other items, try reflection
        return tryReflectionModel(player, stack);
    }

    /**
     * Get model from standard ItemArmor.
     */
    private ModelBiped getItemArmorModel(EntityPlayer player, ItemStack stack, EntityEquipmentSlot slot) {
        try {
            ItemArmor armor = (ItemArmor) stack.getItem();
            net.minecraft.client.model.ModelBase model = armor.getArmorModel(player, stack, slot, null);
            if (model instanceof ModelBiped) {
                return (ModelBiped) model;
            }
            // Fallback
            boolean isLegs = (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET);
            return new ModelBiped(isLegs ? 0.5F : 1.0F);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Try to get model from DynamX or other items via reflection.
     */
    private ModelBiped tryReflectionModel(EntityPlayer player, ItemStack stack) {
        try {
            // Try various method names that DynamX or other mods might use
            String[] methods = {"getModel", "getObjModel", "getArmorModel", "getModelObj"};

            for (String methodName : methods) {
                try {
                    // Try (EntityPlayer, ItemStack)
                    java.lang.reflect.Method m = stack.getItem().getClass()
                            .getMethod(methodName, EntityPlayer.class, ItemStack.class);
                    Object result = m.invoke(stack.getItem(), player, stack);
                    if (result instanceof ModelBiped) {
                        return (ModelBiped) result;
                    }
                } catch (NoSuchMethodException ignored) {}

                try {
                    // Try (ItemStack)
                    java.lang.reflect.Method m = stack.getItem().getClass()
                            .getMethod(methodName, ItemStack.class);
                    Object result = m.invoke(stack.getItem(), stack);
                    if (result instanceof ModelBiped) {
                        return (ModelBiped) result;
                    }
                } catch (NoSuchMethodException ignored) {}

                try {
                    // Try ()
                    java.lang.reflect.Method m = stack.getItem().getClass().getMethod(methodName);
                    Object result = m.invoke(stack.getItem());
                    if (result instanceof ModelBiped) {
                        return (ModelBiped) result;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            // Silent fail
        }

        // Fallback - return basic ModelBiped
        return new ModelBiped(0.5F);
    }

    /**
     * Get clothing texture - handles both ItemArmor and DynamX items.
     */
    private ResourceLocation getClothingTexture(ItemStack stack) {
        // If it's ItemArmor, use standard method
        if (stack.getItem() instanceof ItemArmor) {
            return getItemArmorTexture(stack);
        }

        // For DynamX and other items, try reflection
        return tryReflectionTexture(stack);
    }

    /**
     * Get texture from standard ItemArmor.
     */
    private ResourceLocation getItemArmorTexture(ItemStack stack) {
        try {
            ItemArmor armor = (ItemArmor) stack.getItem();
            EntityEquipmentSlot slot = armor.armorType;
            String texturePath = armor.getArmorTexture(stack, null, slot, null);

            if (texturePath == null || texturePath.trim().isEmpty()) {
                String material = armor.getArmorMaterial().getName();
                if (material == null || material.trim().isEmpty()) {
                    return null;
                }
                String domain = "minecraft";
                int idx = material.indexOf(':');
                if (idx != -1) {
                    domain = material.substring(0, idx);
                    material = material.substring(idx + 1);
                }
                String layer = (slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET)
                        ? "layer_2" : "layer_1";
                texturePath = String.format("%s:textures/models/armor/%s_%s.png", domain, material, layer);
            }

            return TEXTURE_CACHE.computeIfAbsent(texturePath, ResourceLocation::new);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Try to get texture from DynamX or other items via reflection.
     */
    private ResourceLocation tryReflectionTexture(ItemStack stack) {
        try {
            String[] methods = {"getTexture", "getArmorTexture", "getTextureLocation"};

            for (String methodName : methods) {
                try {
                    // Try (ItemStack)
                    java.lang.reflect.Method m = stack.getItem().getClass()
                            .getMethod(methodName, ItemStack.class);
                    Object result = m.invoke(stack.getItem(), stack);
                    if (result instanceof String) {
                        return TEXTURE_CACHE.computeIfAbsent((String) result, ResourceLocation::new);
                    }
                    if (result instanceof ResourceLocation) {
                        return (ResourceLocation) result;
                    }
                } catch (NoSuchMethodException ignored) {}

                try {
                    // Try ()
                    java.lang.reflect.Method m = stack.getItem().getClass().getMethod(methodName);
                    Object result = m.invoke(stack.getItem());
                    if (result instanceof String) {
                        return TEXTURE_CACHE.computeIfAbsent((String) result, ResourceLocation::new);
                    }
                    if (result instanceof ResourceLocation) {
                        return (ResourceLocation) result;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            // Silent fail
        }

        // Fallback - return default diamond armor texture
        return new ResourceLocation("minecraft:textures/models/armor/diamond_layer_1.png");
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}

