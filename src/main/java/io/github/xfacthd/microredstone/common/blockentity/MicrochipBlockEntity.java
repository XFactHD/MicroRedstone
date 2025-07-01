package io.github.xfacthd.microredstone.common.blockentity;

import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.redstone.RedstoneType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

public final class MicrochipBlockEntity extends BlockEntity
{
    public static final ModelProperty<RedstoneType[]> PORT_TYPE_PROPERTY = new ModelProperty<>();

    private final RedstoneType[] portTypes = Utils.fillArray(new RedstoneType[4], $ -> RedstoneType.NONE);

    public MicrochipBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(MRContent.BLOCK_ENTITY_MICROCHIP.value(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MicrochipBlockEntity be)
    {
        //
    }

    @Override
    public ModelData getModelData()
    {
        return ModelData.of(PORT_TYPE_PROPERTY, portTypes.clone());
    }
}
