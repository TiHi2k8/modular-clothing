package com.example.examplemod.capability;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Storage handler for the clothing capability.
 * Delegates serialization to the IClothingInventory implementation.
 */
public class ClothingCapabilityStorage implements Capability.IStorage<IClothingInventory> {

    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IClothingInventory> capability, IClothingInventory instance, EnumFacing side) {
        return instance.serializeNBT();
    }

    @Override
    public void readNBT(Capability<IClothingInventory> capability, IClothingInventory instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            instance.deserializeNBT((NBTTagCompound) nbt);
        }
    }
}

