package keystrokesmod.module.impl.movement.fly;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.movement.Fly;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.SubMode;
import keystrokesmod.utility.MoveUtil;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class GrimAC2Fly extends SubMode<Fly> {
    private final SliderSetting verticalSpeed;
    private int tick;

    public GrimAC2Fly(String name, @NotNull Fly parent) {
        super(name, parent);
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical speed", 0.08, 0.01, 0.2, 0.01));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreMotion(@NotNull PreMotionEvent event) {
        tick++;
        mc.thePlayer.motionY = verticalSpeed.getInput();
        MoveUtil.strafe(0.21);
        if (tick % 4 == 0) {
            event.setOnGround(true);
        } else {
            event.setOnGround(false);
        }
    }

    @Override
    public void onDisable() throws Throwable {
        tick = 0;
    }
}
