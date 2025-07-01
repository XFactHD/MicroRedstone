package io.github.xfacthd.microredstone.common.redstone;

import io.github.xfacthd.microredstone.common.circuit.ExternalInterfaceAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface RedstoneLevelAdapter extends ExternalInterfaceAdapter
{
    RedstoneType getRedstoneType(Direction facing, Direction side);

    int getRedstoneOutput(Direction side);

    void handleNeighborUpdate(BlockPos adjPos, Direction side);
}
