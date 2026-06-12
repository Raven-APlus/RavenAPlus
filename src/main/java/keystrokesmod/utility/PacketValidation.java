package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.exploit.AntiFalseFlag;
import keystrokesmod.module.impl.exploit.antifalseflag.HypixelAntiFalseFlag;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;

/**
 * Pre-send validation hook used by {@link BadPacketsHandler} before packets reach the server.
 */
public final class PacketValidation {
    private PacketValidation() {
    }

    public static boolean shouldCancelOutgoing(Packet<?> packet) {
        if (!Utils.nullCheck()) {
            return false;
        }
        AntiFalseFlag antiFalseFlag = ModuleManager.antiFalseFlag;
        if (antiFalseFlag == null || !antiFalseFlag.isEnabled()) {
            return false;
        }
        SubMode<?> subMode = antiFalseFlag.mode.getSelected();
        if (!(subMode instanceof HypixelAntiFalseFlag)) {
            return false;
        }
        HypixelAntiFalseFlag hypixel = (HypixelAntiFalseFlag) subMode;

        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK && hypixel.shouldCancelReach(useEntity)) {
                return true;
            }
            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK
                    && Raven.blockController.isServerBlocking()
                    && Raven.badPacketsHandler.C07) {
                return true;
            }
        }

        if (packet instanceof C08PacketPlayerBlockPlacement && hypixel.shouldCancelNoSlow()) {
            return true;
        }

        return false;
    }
}
