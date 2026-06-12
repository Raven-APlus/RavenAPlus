package keystrokesmod.module.impl.combat.aimassist;

import akka.japi.Pair;
import keystrokesmod.event.RotationEvent;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.other.RotationHandler;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.aim.AimSimulator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

public class GrimAimAssist extends SubMode<AimAssist> {
    private final SliderSetting fov;
    private final SliderSetting range;
    private final SliderSetting speed;
    private final ButtonSetting clickAim;
    private final ButtonSetting throughBlock;
    private final AimSimulator aimSimulator = new AimSimulator();

    public GrimAimAssist(String name, AimAssist parent) {
        super(name, parent);
        this.registerSetting(fov = new SliderSetting("FOV", 90, 30, 360, 5));
        this.registerSetting(range = new SliderSetting("Range", 4, 1, 6, 0.1));
        this.registerSetting(speed = new SliderSetting("Speed", 4, 1, 10, 0.5));
        this.registerSetting(clickAim = new ButtonSetting("Click aim", true));
        this.registerSetting(throughBlock = new ButtonSetting("Through block", false));
    }

    @SubscribeEvent
    public void onRotation(RotationEvent event) {
        if (clickAim.isToggled() && !Utils.isLeftClicking()) {
            return;
        }
        EntityPlayer target = getTarget();
        if (target == null) {
            return;
        }
        Pair<Float, Float> rot = aimSimulator.getRotation(target);
        Double gcd = AimSimulator.getGCD();
        event.setYaw(AimSimulator.rotMove(rot.first(), event.getYaw(), speed.getInput(), gcd));
        event.setPitch(AimSimulator.rotMove(rot.second(), event.getPitch(), speed.getInput(), gcd));
        event.setMoveFix(RotationHandler.MoveFix.Silent);
    }

    private @Nullable EntityPlayer getTarget() {
        EntityPlayer best = null;
        double bestFov = Double.MAX_VALUE;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.deathTime > 0) continue;
            if (AntiBot.isBot(player) || Utils.isFriended(player)) continue;
            if (mc.thePlayer.getDistanceToEntity(player) > range.getInput()) continue;
            if (fov.getInput() != 360 && !Utils.inFov((int) fov.getInput(), player)) continue;
            if (!throughBlock.isToggled() && RotationUtils.rayCast(mc.thePlayer.getDistanceToEntity(player), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch) != null) {
                continue;
            }
            double curFov = Math.abs(Utils.getFov(player.posX, player.posZ));
            if (curFov < bestFov) {
                bestFov = curFov;
                best = player;
            }
        }
        return best;
    }
}
