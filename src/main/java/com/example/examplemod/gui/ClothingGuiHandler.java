package com.example.examplemod.gui;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class ClothingGuiHandler implements IGuiHandler {
    public static final int GUI_ID = 20;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            return new ClothingContainer(player.inventory, inventory, player);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID) {
            IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
            return new ClothingGui(new ClothingContainer(player.inventory, inventory, player));
        }
        return null;
    }
}

