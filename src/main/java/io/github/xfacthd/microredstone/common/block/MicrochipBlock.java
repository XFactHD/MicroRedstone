package io.github.xfacthd.microredstone.common.block;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.data.PropertyHolder;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.Nullable;

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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        // TODO: replace with UI interaction
        if (stack.is(MRContent.ITEM_INTEGRATED_CIRCUIT) && level.getBlockEntity(pos) instanceof MicrochipBlockEntity be)
        {
            if (!level.isClientSide())
            {
                StoredCircuit circuit = stack.getOrDefault(MRContent.DC_TYPE_CIRCUIT, StoredCircuit.EMPTY);
                be.setCircuit(circuit.name(), circuit.toCircuit());
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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
