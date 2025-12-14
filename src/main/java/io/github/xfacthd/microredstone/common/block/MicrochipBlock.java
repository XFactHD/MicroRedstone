package io.github.xfacthd.microredstone.common.block;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.data.PropertyHolder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class MicrochipBlock extends PlateBlock
{
    public MicrochipBlock(Properties properties)
    {
        super(properties.strength(1.5F, 6F));
        registerDefaultState(defaultBlockState().setValue(PropertyHolder.HAS_CIRCUIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(PropertyHolder.ROTATION, PropertyHolder.HAS_CIRCUIT);
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if (level.getBlockEntity(pos) instanceof MicrochipBlockEntity be)
        {
            if (!level.isClientSide())
            {
                player.openMenu(be);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
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
            return Utils.createBlockEntityTicker(actualType, MRContent.BLOCK_ENTITY_MICROCHIP, MicrochipBlockEntity::tick);
        }
        return null;
    }
}
