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

                // Check for Merged Modes and skip rendering secondary slots if active
                boolean layerChestArmsMode = inventory.getChestArmsMode(layerIndex);
                if (layerChestArmsMode && (slotType == ClothingInventorySlot.RIGHT_ARM || slotType == ClothingInventorySlot.LEFT_ARM)) {
                    continue; // Skip separate arms if Chest is handling them
                }

                boolean layerPantsLegsMode = inventory.getPantsLegsMode(layerIndex);
                if (layerPantsLegsMode && (slotType == ClothingInventorySlot.LEFT_LEG)) {
                    continue; // Skip separate left leg if Right Leg (Pants) is handling both
                }

                boolean layerShoesFeetMode = inventory.getShoesFeetMode(layerIndex);
                if (layerShoesFeetMode && (slotType == ClothingInventorySlot.LEFT_FOOT)) {
                    continue; // Skip separate left foot if Right Foot (Shoes) is handling both
                }

                boolean chestArmsMode = (slotType == ClothingInventorySlot.CHEST)
                        && layerChestArmsMode;
                boolean pantsLegsMode = (slotType == ClothingInventorySlot.RIGHT_LEG || slotType == ClothingInventorySlot.LEFT_LEG)
                        && layerPantsLegsMode;
                boolean shoesFeetMode = (slotType == ClothingInventorySlot.RIGHT_FOOT || slotType == ClothingInventorySlot.LEFT_FOOT)
                        && layerShoesFeetMode;

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
                        // "Pants" slot, located at Screen Right (or Center)
                        if (pantsLegsMode) {
                            model.bipedBody.showModel = true; // Body covered by pants
                            model.bipedLeftLeg.showModel = true; // Screen Right Leg (Player Left)
                            model.bipedRightLeg.showModel = true; // Screen Left Leg (Player Right)
                        } else {
                             // Separate: Screen Right Slot -> renders Screen Right Leg (Player Left)
                            model.bipedLeftLeg.showModel  = true;
                        }
                        break;
                    case LEFT_LEG:
                        // Separate: Screen Left Slot -> renders Screen Left Leg (Player Right)
                         if (!pantsLegsMode) {
                            model.bipedRightLeg.showModel = true;
                         }
                        break;
                    case RIGHT_FOOT:
                        // "Shoes" slot, Screen Right
                        if (shoesFeetMode) {
                             model.bipedLeftLeg.showModel = true; // Screen Right Foot
                             model.bipedRightLeg.showModel = true; // Screen Left Foot
                        } else {
                            // Separate: Right only -> Screen Right Foot
                            model.bipedLeftLeg.showModel = true;
                        }
                        break;
                    case LEFT_FOOT:
                        // Separate: Left only -> Screen Left Foot
                        if (!shoesFeetMode) {
                            model.bipedRightLeg.showModel = true;
                        }
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

                    // Fix for shoes/pants clipping when sneaking
                    if (player.isSneaking() && (
                            slotType == ClothingInventorySlot.RIGHT_LEG ||
                            slotType == ClothingInventorySlot.LEFT_LEG ||
                            slotType == ClothingInventorySlot.RIGHT_FOOT ||
                            slotType == ClothingInventorySlot.LEFT_FOOT)) {
                        float[] modified = transform.clone();
                        modified[5] += 1.0f; // Move backwards by 1.0f
                        transform = modified;
                    }

                    if (isDynamX) {
                        // Use the main player model as the reference for pivots, because it has the correct
                        // sneaking/riding offsets applied for the current frame.
                        ModelBiped pivotReference = (ModelBiped) this.renderer.getMainModel();

                        boolean handledByDynamX = DynamXHelper.renderDynamXArmorPart(
                                model, pivotReference, slotType,
                                chestArmsMode, pantsLegsMode, shoesFeetMode,
                                scale, transform,
                                player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                        
                        if (!handledByDynamX) {
                             // Fallback for failed DynamX render
                            float scaleX = transform[0];
                            float scaleY = transform[1];
                            float scaleZ = transform[2];
                            float ox     = transform[3];
                            float oy     = transform[4];
                            float oz     = transform[5];

                            net.minecraft.client.model.ModelRenderer pivotPart =
                                    getModelPartForSlot(pivotReference, slotType, pantsLegsMode, shoesFeetMode);

                            float tx = ox;
                            float ty = oy;
                            float tz = oz;
                            if (pivotPart != null && (ox != 0.0f || oy != 0.0f || oz != 0.0f)) {
                                float[] rotatedOffset = rotateVectorByPartAngles(
                                        ox, oy, oz,
                                        pivotPart.rotateAngleX,
                                        pivotPart.rotateAngleY,
                                        pivotPart.rotateAngleZ);
                                tx = rotatedOffset[0];
                                ty = rotatedOffset[1];
                                tz = rotatedOffset[2];
                            }

                            GlStateManager.pushMatrix();
                            if (tx != 0.0f || ty != 0.0f || tz != 0.0f) {
                                GlStateManager.translate(tx, ty, tz);
                            }
                            if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
                                float[] pivot = getPivotForSlot(pivotReference, slotType, chestArmsMode, pantsLegsMode, shoesFeetMode);
                                GlStateManager.translate(pivot[0], pivot[1], pivot[2]);
                                GlStateManager.scale(scaleX, scaleY, scaleZ);
                                GlStateManager.translate(-pivot[0], -pivot[1], -pivot[2]);
                            }
                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                            model.render(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                            GlStateManager.popMatrix();
                        }
                    } else {
                        // Standard rendering: apply transform per-part locally in the hierarchy
                        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, player);

                        renderPartOverride(model.bipedHead, transform, scale);
                        renderPartOverride(model.bipedHeadwear, transform, scale);
                        renderPartOverride(model.bipedBody, transform, scale);
                        renderPartOverride(model.bipedLeftArm, transform, scale);
                        renderPartOverride(model.bipedRightArm, transform, scale);
                        renderPartOverride(model.bipedLeftLeg, transform, scale);
                        renderPartOverride(model.bipedRightLeg, transform, scale);
                    }
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

    private net.minecraft.client.model.ModelRenderer getModelPartForSlot(ModelBiped model,
                                                                          ClothingInventorySlot slotType,
                                                                          boolean pantsLegsMode,
                                                                          boolean shoesFeetMode) {
        switch (slotType) {
            case HEAD:
                return model.bipedHead;
            case CHEST:
                return model.bipedBody;
            case RIGHT_ARM:
                return model.bipedLeftArm;
            case LEFT_ARM:
                return model.bipedRightArm;
            case RIGHT_LEG:
                return pantsLegsMode ? model.bipedBody : model.bipedLeftLeg;
            case LEFT_LEG:
                return model.bipedRightLeg;
            case RIGHT_FOOT:
                return shoesFeetMode ? model.bipedBody : model.bipedLeftLeg;
            case LEFT_FOOT:
                return model.bipedRightLeg;
            default:
                return model.bipedBody;
        }
    }

    private float[] rotateVectorByPartAngles(float x, float y, float z,
                                             float angleX, float angleY, float angleZ) {
        // Match ModelRenderer transformation order: Rz * Ry * Rx * v
        // This means we apply X rotation first (closest to vector), then Y, then Z.
        float[] v = new float[]{x, y, z};

        // 1. Rotate around X axis
        if (angleX != 0.0f) {
            float cos = (float) Math.cos(angleX);
            float sin = (float) Math.sin(angleX);
            float ny = v[1] * cos - v[2] * sin;
            float nz = v[1] * sin + v[2] * cos;
            v[1] = ny;
            v[2] = nz;
        }

        // 2. Rotate around Y axis
        if (angleY != 0.0f) {
            float cos = (float) Math.cos(angleY);
            float sin = (float) Math.sin(angleY);
            float nx = v[0] * cos + v[2] * sin;
            float nz = -v[0] * sin + v[2] * cos;
            v[0] = nx;
            v[2] = nz;
        }

        // 3. Rotate around Z axis
        if (angleZ != 0.0f) {
            float cos = (float) Math.cos(angleZ);
            float sin = (float) Math.sin(angleZ);
            float nx = v[0] * cos - v[1] * sin;
            float ny = v[0] * sin + v[1] * cos;
            v[0] = nx;
            v[1] = ny;
        }

        return v;
    }

    private float[] getPivotForSlot(ModelBiped model,
                                    ClothingInventorySlot slotType,
                                    boolean chestArmsMode,
                                    boolean pantsLegsMode,
                                    boolean shoesFeetMode) {
        net.minecraft.client.model.ModelRenderer part =
                getModelPartForSlot(model, slotType, pantsLegsMode, shoesFeetMode);

        return new float[]{
                part.rotationPointX / 16.0f,
                part.rotationPointY / 16.0f,
                part.rotationPointZ / 16.0f
        };
    }


    private void renderPartOverride(net.minecraft.client.model.ModelRenderer part, float[] transform, float scale) {
        if (!part.showModel || part.isHidden) return;

        float scaleX = transform[0];
        float scaleY = transform[1];
        float scaleZ = transform[2];
        float ox     = transform[3];
        float oy     = transform[4];
        float oz     = transform[5];

        GlStateManager.pushMatrix();

        // 1. Position at the part's pivot (standard ModelRenderer behavior)
        if (part.rotationPointX != 0.0F || part.rotationPointY != 0.0F || part.rotationPointZ != 0.0F) {
            GlStateManager.translate(part.rotationPointX * scale, part.rotationPointY * scale, part.rotationPointZ * scale);
        }

        // 2. Apply Model Rotation (standard ModelRenderer behavior)
        if (part.rotateAngleZ != 0.0F) GlStateManager.rotate(part.rotateAngleZ * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
        if (part.rotateAngleY != 0.0F) GlStateManager.rotate(part.rotateAngleY * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
        if (part.rotateAngleX != 0.0F) GlStateManager.rotate(part.rotateAngleX * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);

        // 3. User Transform - "Relative to the limb pivot and rotation"
        // Apply Offset
        if (ox != 0.0f || oy != 0.0f || oz != 0.0f) {
            GlStateManager.translate(ox, oy, oz);
        }
        // Apply Scale
        if (scaleX != 1.0f || scaleY != 1.0f || scaleZ != 1.0f) {
            GlStateManager.scale(scaleX, scaleY, scaleZ);
        }

        // 4. Render the part content
        // Temporarily zero transforms so render(scale) only draws the boxes/list without re-transforming.
        float savedX = part.rotationPointX;
        float savedY = part.rotationPointY;
        float savedZ = part.rotationPointZ;
        float savedRotX = part.rotateAngleX;
        float savedRotY = part.rotateAngleY;
        float savedRotZ = part.rotateAngleZ;

        part.rotationPointX = 0;
        part.rotationPointY = 0;
        part.rotationPointZ = 0;
        part.rotateAngleX = 0;
        part.rotateAngleY = 0;
        part.rotateAngleZ = 0;
        
        // IMPORTANT: ModelRenderer.render() normally compiles commands to translate to rotationPoint,
        // rotate, THEN compile the display list.
        // We have zeroed rotationPoint and rotateAngle, so render() will essentially translate(0,0,0)
        // and call the display list.
        // The display list contains the box geometry compiled relative to the pivot.
        
        part.render(scale);

        // Restore
        part.rotationPointX = savedX;
        part.rotationPointY = savedY;
        part.rotationPointZ = savedZ;
        part.rotateAngleX = savedRotX;
        part.rotateAngleY = savedRotY;
        part.rotateAngleZ = savedRotZ;

        GlStateManager.popMatrix();
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
