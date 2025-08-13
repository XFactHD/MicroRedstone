package io.github.xfacthd.microredstone.common.compat.morered;

import io.github.xfacthd.microredstone.common.compat.CompatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

// TODO: implement MoreRed support
public final class MoreRedCompat
{
    private static boolean loaded = false;

    public static void init(IEventBus modBus)
    {
        if (ModList.get().isLoaded("morered"))
        {
            try
            {
                GuardedAccess.init(modBus);
                loaded = true;
            }
            catch (Throwable t)
            {
                CompatHandler.LOGGER.error("Failed to initialize MoreRed compat", t);
            }
        }
    }

    public static boolean isBundledCable(Level level, BlockPos pos, Direction side)
    {
        return false;
    }

    public static void updateNeighbor(Level level, BlockPos pos, BlockPos adjPos, Direction side)
    {

    }

    public static int getBundledPower(Level level, BlockPos pos, Direction side)
    {
        return 0;
    }

    private static final class GuardedAccess
    {
        static void init(IEventBus modBus)
        {

        }

        private GuardedAccess() {}
    }

    private MoreRedCompat() { }
}
