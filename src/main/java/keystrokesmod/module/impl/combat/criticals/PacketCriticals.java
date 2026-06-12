package keystrokesmod.module.impl.combat.criticals;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixins.impl.network.C03PacketPlayerAccessor;
import keystrokesmod.module.impl.combat.Criticals;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class PacketCriticals extends SubMode<Criticals> {
    private final SliderSetting chance;
    private final SliderSetting minFall;

    public PacketCriticals(String name, @NotNull Criticals parent) {
        super(name, parent);
        this.registerSetting(chance = new SliderSetting("Chance", 100, 0, 100, 1, "%"));
        this.registerSetting(minFall = new SliderSetting("Min fall distance", 0.05, 0, 0.5, 0.01));
    }

    @SubscribeEvent
    public void onSendPacket(@NotNull SendPacketEvent event) {
        if (!(event.getPacket() instanceof C03PacketPlayer) || mc.thePlayer.onGround) {
            return;
        }
        if (mc.thePlayer.fallDistance < minFall.getInput()) {
            return;
        }
        if (chance.getInput() != 100 && Math.random() * 100 > chance.getInput()) {
            return;
        }
        if (!Utils.isTargetNearby(4)) {
            return;
        }
        C03PacketPlayerAccessor accessor = (C03PacketPlayerAccessor) event.getPacket();
        accessor.setOnGround(false);
    }
}
