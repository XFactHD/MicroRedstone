package io.github.xfacthd.microredstone.common.redstone;

import io.github.xfacthd.microredstone.common.compat.exmachina.ExMachinaCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class WireSupport {
    public static void updateNeighbor(Level level, BlockPos pos, BlockState state, BlockPos adjPos, Direction side, RedstoneType type) {
        Direction adjSide = side.getOpposite();
        RedstoneLevelAdapter adapter = getAdapter(level, adjPos, adjSide, type);
        if (adapter != null) {
            adapter.handleNeighborUpdate(pos, adjSide);
            return;
        }

        switch (type) {
            case SINGLE -> level.neighborChanged(adjPos, state.getBlock(), null);
            case BUNDLED -> ExMachinaCompat.updateNeighbor(level, pos);
        }
    }

    public static short getInput(Level level, BlockPos pos, BlockPos adjPos, Direction side, RedstoneType type) {
        Direction adjSide = side.getOpposite();
        RedstoneLevelAdapter adapter = getAdapter(level, adjPos, adjSide, RedstoneType.SINGLE);
        if (adapter != null) {
            return (short) (adapter.getRedstoneOutput(adjSide) > 0 ? 1 : 0);
        }
        if (type == RedstoneType.SINGLE) {
            return (short) (level.hasSignal(adjPos, side) ? 1 : 0);
        }
        return -1;
    }

    private static @Nullable RedstoneLevelAdapter getAdapter(Level level, BlockPos pos, Direction side, RedstoneType type) {
        if (level.getBlockEntity(pos) instanceof RedstoneLevelAdapter adapter && adapter.getRedstoneType(side) == type) {
            return adapter;
        }
        return null;
    }

    private WireSupport() { }
}
