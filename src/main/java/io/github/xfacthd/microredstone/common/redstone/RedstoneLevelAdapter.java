package io.github.xfacthd.microredstone.common.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface RedstoneLevelAdapter {
    RedstoneType getRedstoneType(Direction side);

    int getRedstoneOutput(Direction side);

    void handleNeighborUpdate(BlockPos adjPos, Direction side);
}
