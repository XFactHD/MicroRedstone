package io.github.xfacthd.microredstone.common.redstone;

import io.github.xfacthd.microredstone.common.compat.morered.MoreRedCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class BundledWireSupport
{
    public static void updateNeighbor(Level level, BlockPos pos, BlockPos adjPos, Direction side)
    {
        Direction adjSide = side.getOpposite();
        RedstoneLevelAdapter adapter = getBundledAdapter(level, pos, adjSide);
        if (adapter != null)
        {
            adapter.handleNeighborUpdate(pos, adjSide);
        }
        else if (MoreRedCompat.isBundledCable(level, adjPos, adjSide))
        {
            MoreRedCompat.updateNeighbor(level, adjPos, pos, adjSide);
        }
    }

    public static short getBundledInput(Level level, BlockPos pos, BlockPos adjPos, Direction side)
    {
        Direction adjSide = side.getOpposite();
        RedstoneLevelAdapter adapter = getBundledAdapter(level, adjPos, adjSide);
        if (adapter != null)
        {
            return (short) adapter.getRedstoneOutput(adjSide);
        }
        else if (MoreRedCompat.isBundledCable(level, adjPos, adjSide))
        {
            return (short) MoreRedCompat.getBundledPower(level, adjPos, adjSide);
        }
        return 0;
    }

    @Nullable
    private static RedstoneLevelAdapter getBundledAdapter(Level level, BlockPos pos, Direction side)
    {
        if (level.getBlockEntity(pos) instanceof RedstoneLevelAdapter adapter)
        {
            if (adapter.getRedstoneType(side) == RedstoneType.BUNDLED)
            {
                return adapter;
            }
        }
        return null;
    }

    private BundledWireSupport() { }
}
