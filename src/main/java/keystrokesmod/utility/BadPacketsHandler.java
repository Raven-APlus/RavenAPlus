package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class BadPacketsHandler { // ensures you don't get banned
    public boolean C08;
    public boolean C07;
    private boolean C02;
    public boolean C09;
    public boolean delayAttack;
    public boolean delay;
    public int playerSlot = -1;
    public int serverSlot = -1;

    public boolean canSendBlock() {
        return !C07 && !Raven.blockController.shouldDeferAttack();
    }

    public boolean canSendAttack() {
        return !C07 && !delayAttack && !Raven.blockController.shouldDeferAttack();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(@NotNull SendPacketEvent e) {
        if (e.isCanceled()) {
            return;
        }
        if (PacketValidation.shouldCancelOutgoing(e.getPacket())) {
            e.setCanceled(true);
            return;
        }
        if (e.getPacket() instanceof C02PacketUseEntity) {
            if (!canSendAttack()) {
                PacketScheduler.queue(e.getPacket(), PacketScheduler.Priority.HIGH);
                e.setCanceled(true);
                return;
            }
            C02 = true;
        }
        else if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (!canSendBlock()) {
                PacketScheduler.queue(e.getPacket(), PacketScheduler.Priority.NORMAL);
                e.setCanceled(true);
                return;
            }
            C08 = true;
        }
        else if (e.getPacket() instanceof C07PacketPlayerDigging) {
            C07 = true;
        }
        else if (e.getPacket() instanceof C09PacketHeldItemChange) {
            if (((C09PacketHeldItemChange) e.getPacket()).getSlotId() == playerSlot && ((C09PacketHeldItemChange) e.getPacket()).getSlotId() == serverSlot) {
                e.setCanceled(true);
                return;
            }
            C09 = true;
            serverSlot = playerSlot = ((C09PacketHeldItemChange) e.getPacket()).getSlotId();
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S09PacketHeldItemChange) {
            S09PacketHeldItemChange packet = (S09PacketHeldItemChange) e.getPacket();
            if (packet.getHeldItemHotbarIndex() >= 0 && packet.getHeldItemHotbarIndex() < InventoryPlayer.getHotbarSize()) {
                serverSlot = packet.getHeldItemHotbarIndex();
            }
        }
        else if (e.getPacket() instanceof S0CPacketSpawnPlayer && Minecraft.getMinecraft().thePlayer != null) {
            if (((S0CPacketSpawnPlayer) e.getPacket()).getEntityID() != Minecraft.getMinecraft().thePlayer.getEntityId()) {
                return;
            }
            this.playerSlot = -1;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostUpdate(PostUpdateEvent e) {
        if (delay) {
            delayAttack = false;
            delay = false;
        }
        if (C08 || C09) {
            delay = true;
            delayAttack = true;
        }
        C08 = C07 = C02 = C09 = false;
        Raven.blockController.clearDeferAttack();
    }
}
