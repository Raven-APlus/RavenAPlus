package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.mixins.impl.entity.EntityPlayerAccessor;
import keystrokesmod.module.impl.other.SlotHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;

import static keystrokesmod.Raven.mc;
import static net.minecraft.util.EnumFacing.DOWN;

public class BlockController {
    public enum BlockMethod {
        PACKET,
        VANILLA
    }

    private boolean clientBlocking;
    private boolean serverBlocking;
    private boolean deferAttack;

    public boolean isClientBlocking() {
        return clientBlocking;
    }

    public boolean isServerBlocking() {
        return serverBlocking;
    }

    public boolean shouldDeferAttack() {
        return deferAttack || Raven.badPacketsHandler.C07;
    }

    public void clearDeferAttack() {
        deferAttack = false;
    }

    public boolean canSendBlock() {
        return !Raven.badPacketsHandler.C07;
    }

    public void syncClientVisual(boolean state) {
        if (state) {
            ((EntityPlayerAccessor) mc.thePlayer).setItemInUseCount(72000);
            clientBlocking = true;
        } else {
            Reflection.setBlocking(false);
            clientBlocking = false;
        }
    }

    public void startBlock(BlockMethod method) {
        if (!Utils.holdingSword() || serverBlocking || !canSendBlock()) {
            return;
        }

        if (method == BlockMethod.VANILLA) {
            int key = mc.gameSettings.keyBindUseItem.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
            Utils.setMouseButtonState(1, true);
            serverBlocking = true;
            clientBlocking = true;
        } else {
            PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(SlotHandler.getHeldItem()));
            syncClientVisual(true);
            serverBlocking = true;
        }
    }

    public void startBlockPacket() {
        startBlock(BlockMethod.PACKET);
    }

    public void stopBlock() {
        if (!Utils.holdingSword()) {
            reset();
            return;
        }
        if (serverBlocking) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
            serverBlocking = false;
            deferAttack = true;
        }
        stopClientBlock();
    }

    public void stopClientBlock() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        Utils.setMouseButtonState(1, false);
        if (mc.thePlayer.isUsingItem()) {
            mc.thePlayer.stopUsingItem();
        }
        Reflection.setBlocking(false);
        clientBlocking = false;
    }

    public void reset() {
        if (serverBlocking && Utils.holdingSword()) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
        }
        serverBlocking = false;
        deferAttack = false;
        stopClientBlock();
    }

    /**
     * Unblocks on server if needed so an attack can be sent. Returns true when attack may proceed.
     */
    public boolean prepareForAttack() {
        if (serverBlocking) {
            stopBlock();
            return false;
        }
        if (shouldDeferAttack()) {
            clearDeferAttack();
            return false;
        }
        return true;
    }
}
