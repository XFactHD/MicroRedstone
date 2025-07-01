package io.github.xfacthd.microredstone.common.block;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.data.PropertyHolder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public final class MicrochipBlock extends PlateBlock
{
    public MicrochipBlock(Properties properties)
    {
        super(properties.strength(1.5F, 6F));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(PropertyHolder.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        BlockState state = super.getStateForPlacement(ctx);
        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction orientation = Utils.getDirFromCross(ctx.getClickLocation(), ctx.getClickedFace());
        Rotation rotation = Utils.getRotationFromFacingOrientation(facing, orientation);
        return state.setValue(PropertyHolder.ROTATION, rotation);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new MicrochipBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actualType)
    {
        if (!level.isClientSide())
        {
            return BaseEntityBlock.createTickerHelper(actualType, MRContent.BLOCK_ENTITY_MICROCHIP.value(), MicrochipBlockEntity::tick);
        }
        return null;
    }
}
