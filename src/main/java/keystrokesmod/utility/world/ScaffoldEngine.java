package keystrokesmod.utility.world;

import keystrokesmod.utility.BlockUtils;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * Shared scaffold placement helpers extracted from Scaffold module.
 */
public final class ScaffoldEngine {
    private ScaffoldEngine() {
    }

    /** Returns false when expand target is not replaceable (Polar would cancel). */
    public static boolean canPolarExpandPlace(BlockPos placePos, EnumFacing facing) {
        return BlockUtils.replaceable(placePos.offset(facing));
    }
}
