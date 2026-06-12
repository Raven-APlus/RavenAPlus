package keystrokesmod.module.impl.movement.noslow;

import keystrokesmod.Raven;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.other.SlotHandler;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * BlockController-based noslow for GrimAC — avoids patched slot-cycle spam.
 */
public class GrimAC2NoSlow extends INoSlow {
    private int swordTick;

    public GrimAC2NoSlow(String name, @NotNull NoSlow parent) {
        super(name, parent);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        ItemStack itemStack = SlotHandler.getHeldItem();
        if (!mc.thePlayer.isUsingItem() || itemStack == null) {
            swordTick = 0;
            return;
        }

        if (NoSlow.sword.isToggled() && itemStack.getItem() instanceof ItemSword) {
            if (++swordTick % 3 == 0 && Raven.blockController.canSendBlock()) {
                if (Raven.blockController.isServerBlocking()) {
                    Raven.blockController.stopBlock();
                } else {
                    Raven.blockController.startBlockPacket();
                }
            }
        } else if (NoSlow.bow.isToggled() && itemStack.getItem() instanceof ItemBow) {
            if (swordTick++ % 4 == 0 && Raven.blockController.canSendBlock() && !Raven.blockController.isServerBlocking()) {
                Raven.blockController.startBlockPacket();
            }
        }
    }

    @Override
    public void onDisable() throws Throwable {
        swordTick = 0;
        if (Raven.blockController.isServerBlocking()) {
            Raven.blockController.reset();
        }
    }

    @Override
    public float getSlowdown() {
        return 1;
    }
}
