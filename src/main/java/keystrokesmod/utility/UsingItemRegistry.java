package keystrokesmod.utility;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.movement.NoSlow;

/**
 * Centralizes fake using-item / noslow slowdown logic for mixins and packet guards.
 */
public final class UsingItemRegistry {
    private UsingItemRegistry() {
    }

    public static boolean isAutoBlocking() {
        final KillAura killAura = ModuleManager.killAura;
        return killAura != null
                && killAura.isEnabled()
                && Utils.holdingSword()
                && killAura.isClientBlocking()
                && (killAura.autoBlockMode.getInput() != 0
                || killAura.rmbDown
                || !killAura.manualBlock.isToggled());
    }

    public static boolean isUsingItemModified(boolean vanillaUsingItem) {
        return vanillaUsingItem || isAutoBlocking();
    }

    public static float getForwardSlowed(boolean autoBlocking) {
        if (autoBlocking && ModuleManager.killAura != null) {
            return (float) ModuleManager.killAura.slowdown.getInput();
        }
        return NoSlow.getForwardSlowed();
    }

    public static float getStrafeSlowed(boolean autoBlocking) {
        if (autoBlocking && ModuleManager.killAura != null) {
            return (float) ModuleManager.killAura.slowdown.getInput();
        }
        return NoSlow.getStrafeSlowed();
    }

    public static boolean shouldStopSprint(boolean vanillaUsingItem) {
        final boolean autoBlocking = isAutoBlocking();
        return vanillaUsingItem
                && (ModuleManager.noSlow != null && ModuleManager.noSlow.isEnabled() && NoSlow.getForwardSlowed() <= 0.8)
                || (autoBlocking && ModuleManager.killAura != null && ModuleManager.killAura.slowdown.getInput() <= 0.8);
    }
}
