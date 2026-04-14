package io.github.xfacthd.microredstone.client.model;

import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.redstone.RedstoneType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

import java.util.List;

public final class MicrochipBlockStateModel extends DelegateBlockStateModel {
    private final BlockStateModel[] singleModels;
    private final BlockStateModel[] bundledModels;

    MicrochipBlockStateModel(BlockStateModel baseModel, BlockStateModel[] singleModels, BlockStateModel[] bundledModels) {
        super(baseModel);
        this.singleModels = singleModels;
        this.bundledModels = bundledModels;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        super.collectParts(level, pos, state, random, parts);

        RedstoneType[] portTypes = level.getModelData(pos).get(MicrochipBlockEntity.PORT_TYPE_PROPERTY);
        if (portTypes == null) {
            return;
        }

        for (int i = 0; i < 4; i++) {
            switch (portTypes[i]) {
                case NONE -> { }
                case SINGLE -> singleModels[i].collectParts(level, pos, state, random, parts);
                case BUNDLED -> bundledModels[i].collectParts(level, pos, state, random, parts);
            }
        }
    }
}
