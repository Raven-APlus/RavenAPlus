package keystrokesmod.module.impl.player.nofall;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SubMode;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class NoGroundNoFall extends SubMode<NoFall> {
    public NoGroundNoFall(String name, @NotNull NoFall parent) {
        super(name, parent);
        this.registerSetting(new DescriptionSetting("Legacy — flagged on modern AC."));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreMotion(@NotNull PreMotionEvent event) {
        if (!parent.noAction())
            event.setOnGround(false);
    }
}
