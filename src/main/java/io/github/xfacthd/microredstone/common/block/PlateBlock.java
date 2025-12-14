package io.github.xfacthd.microredstone.common.block;

import io.github.xfacthd.microredstone.common.redstone.RedstoneLevelAdapter;
import io.github.xfacthd.microredstone.common.redstone.RedstoneType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class PlateBlock extends Block implements EntityBlock
{
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final VoxelShape[] SHAPES = makeShapes(2D);
    // Make the collision shape slightly higher to avoid playing step sound and particles of the block below
    private static final VoxelShape[] COLLISION_SHAPES = makeShapes(3.3D);

    protected PlateBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        return defaultBlockState().setValue(BlockStateProperties.FACING, ctx.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return SHAPES[getFacing(state).ordinal()];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return COLLISION_SHAPES[getFacing(state).ordinal()];
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction dir)
    {
        Direction facing = getFacing(state);
        if (dir != null && dir.getAxis() != facing.getAxis() && level.getBlockEntity(pos) instanceof RedstoneLevelAdapter be)
        {
            Direction side = dir.getOpposite(); // The given direction is from the wire's view
            return be.getRedstoneType(side) == RedstoneType.SINGLE;
        }
        return false;
    }

    @Override
    protected boolean isSignalSource(BlockState state)
    {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir)
    {
        return getSignal(state, level, pos, dir);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir)
    {
        Direction side = dir.getOpposite(); // The given direction is from the wire's view
        if (level.getBlockEntity(pos) instanceof RedstoneLevelAdapter be)
        {
            return be.getRedstoneOutput(side);
        }
        return 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block adjBlock, @Nullable Orientation orientation, boolean moved)
    {
        // FIXME: the whole Orientation thing makes zero sense and iterating the four directions is stupid...
        Direction.Axis axis = getFacing(state).getAxis();
        for (Direction dir : DIRECTIONS)
        {
            if (dir.getAxis() != axis)
            {
                onNeighborChange(state, level, pos, pos.relative(dir));
            }
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos adjPos)
    {
        if (level.isClientSide()) return;

        Direction side = Utils.getDirection(pos, adjPos);
        if (side.getAxis() != getFacing(state).getAxis() && level.getBlockEntity(pos) instanceof RedstoneLevelAdapter be)
        {
            be.handleNeighborUpdate(adjPos, side);
        }
    }

    protected static Direction getFacing(BlockState state)
    {
        return state.getValue(BlockStateProperties.FACING);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static VoxelShape[] makeShapes(double height)
    {
        double inv = 16 - height;
        VoxelShape[] shapes = new VoxelShape[6];
        shapes[Direction.UP.ordinal()] =    box(  0, inv,   0,     16,     16,     16);
        shapes[Direction.DOWN.ordinal()] =  box(  0,   0,   0,     16, height,     16);
        shapes[Direction.NORTH.ordinal()] = box(  0,   0,   0,     16,     16, height);
        shapes[Direction.SOUTH.ordinal()] = box(  0,   0, inv,     16,     16,     16);
        shapes[Direction.WEST.ordinal()] =  box(  0,   0,   0, height,     16,     16);
        shapes[Direction.EAST.ordinal()] =  box(inv,   0,   0,     16,     16,     16);
        return shapes;
    }
}
