package com.example.examplemod.gui;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * GUI handler that creates the clothing container (server) and GUI (client).
 */
public class ModGuiHandler implements IGuiHandler {

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == ExampleMod.GUI_CLOTHING_ID) {
            IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
            if (inv != null) {
                return new ClothingContainer(player.inventory, inv);
            }
        }
        return null;
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == ExampleMod.GUI_CLOTHING_ID) {
            IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
            if (inv != null) {
                return new ClothingGuiContainer(new ClothingContainer(player.inventory, inv));
            }
        }
        return null;
    }
}

