package keystrokesmod.module.impl.player.nofall;

import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixins.impl.network.C03PacketPlayerAccessor;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class GrimACNoFall extends SubMode<NoFall> {
    private final SliderSetting minFallDistance;
    private int groundTicks;

    public GrimACNoFall(String name, @NotNull NoFall parent) {
        super(name, parent);
        this.registerSetting(new DescriptionSetting("Rate-limited ground spoof for GrimAC."));
        this.registerSetting(minFallDistance = new SliderSetting("Minimum fall distance", 2.5, 0.5, 8.0, 0.1));
    }

    @SubscribeEvent
    public void onSendPacket(@NotNull SendPacketEvent event) {
        if (parent.noAction() || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        if (mc.thePlayer.fallDistance < minFallDistance.getInput()) {
            return;
        }
        if (++groundTicks % 3 != 0) {
            return;
        }
        C03PacketPlayerAccessor accessor = (C03PacketPlayerAccessor) event.getPacket();
        accessor.setOnGround(true);
        mc.thePlayer.fallDistance = 0;
    }

    @Override
    public void onDisable() throws Throwable {
        groundTicks = 0;
    }
}
