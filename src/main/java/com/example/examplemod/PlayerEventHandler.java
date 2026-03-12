package com.example.examplemod;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.ClothingInventory;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.SyncHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handles all player-related events for the clothing system:
 * - Attaching capabilities to players
 * - Copying inventory on death/clone
 * - Syncing data on login, respawn, dimension change
 * - Tracking players for multiplayer sync
 */
public class PlayerEventHandler {

    private static final ResourceLocation CLOTHING_CAP_ID = new ResourceLocation(ExampleMod.MODID, "clothing_inventory");

    /**
     * Attach the clothing inventory capability to all players.
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(CLOTHING_CAP_ID, new ClothingCapabilityProvider());
        }
    }

    /**
     * Copy clothing inventory when player is cloned (death, end portal return).
     * Respects keepInventory gamerule.
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        IClothingInventory oldInv = event.getOriginal().getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
        IClothingInventory newInv = event.getEntityPlayer().getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);

        if (oldInv != null && newInv != null) {
            // Always copy on dimension change (not death), or if keepInventory is on
            if (!event.isWasDeath() || event.getEntityPlayer().world.getGameRules().getBoolean("keepInventory")) {
                newInv.deserializeNBT(oldInv.serializeNBT());
            }
            // If death and no keepInventory, inventory is lost (items should be dropped)
            // Dropping items on death is handled in onPlayerDeath if needed
        }
    }

    /**
     * Sync clothing inventory to the player when they log in.
     * Also sync all other online players' clothing to the newcomer.
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            // Sync this player's clothing to themselves
            SyncHelper.syncToTrackingPlayers(player);

            // Sync all other players' clothing to this newcomer
            for (EntityPlayer other : player.world.playerEntities) {
                if (other != player && other instanceof EntityPlayerMP) {
                    SyncHelper.syncToPlayer(player, other);
                }
            }
        }
    }

    /**
     * Re-sync after respawn (new entity created).
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            // Delay sync by 1 tick to ensure the player entity is fully initialized
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            player.getServerWorld().addScheduledTask(() -> {
                SyncHelper.syncToTrackingPlayers(player);
            });
        }
    }

    /**
     * Re-sync after dimension change (new entity in new world).
     */
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            player.getServerWorld().addScheduledTask(() -> {
                SyncHelper.syncToTrackingPlayers(player);
            });
        }
    }

    /**
     * Periodically sync nearby players' clothing (handles edge cases like
     * players entering tracking range). Runs every 100 ticks (~5 seconds).
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof EntityPlayerMP)) return;

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.ticksExisted % 100 == 0) {
            SyncHelper.syncToTrackingPlayers(player);
        }
    }
}

