package io.github.xfacthd.microredstone.client.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public final class UnbakedMicrochipModel implements CustomUnbakedBlockStateModel {
    public static final MapCodec<UnbakedMicrochipModel> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Identifier.CODEC.fieldOf("model").forGetter(model -> model.baseModel),
            Variant.SimpleModelState.MAP_CODEC.forGetter(model -> model.variantState)
    ).apply(inst, UnbakedMicrochipModel::new));
    private static final String[] EDGE_SUFFIXES = new String[] { "n", "e", "s", "w" };
    public static final Identifier[] LOCATIONS_SINGLE = Utils.fillArray(new Identifier[4], edge ->
            Utils.rl("block/type_single_" + EDGE_SUFFIXES[edge])
    );
    public static final Identifier[] LOCATIONS_BUNDLED = Utils.fillArray(new Identifier[4], edge ->
            Utils.rl("block/type_bundled_" + EDGE_SUFFIXES[edge])
    );

    private final Identifier baseModel;
    private final Variant.SimpleModelState variantState;
    private final ModelState modelState;
    private final boolean up;

    UnbakedMicrochipModel(Identifier baseModel, Variant.SimpleModelState variantState) {
        this.baseModel = baseModel;
        this.variantState = variantState;
        this.modelState = variantState.asModelState();
        this.up = variantState.x() == Quadrant.R180;
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        BlockStateModel[] singleModels = new BlockStateModel[4];
        BlockStateModel[] bundledModels = new BlockStateModel[4];

        for (int edge = 0; edge < 4; edge++) {
            int outEdge = (edge + (up ? 1 : 2)) % 4;
            singleModels[outEdge] = bakePart(baker, LOCATIONS_SINGLE[edge], modelState);
            bundledModels[outEdge] = bakePart(baker, LOCATIONS_BUNDLED[edge], modelState);
        }

        return new MicrochipBlockStateModel(bakePart(baker, baseModel, modelState), singleModels, bundledModels);
    }

    private static BlockStateModel bakePart(ModelBaker baker, Identifier model, ModelState modelState) {
        return new SingleVariant(SimpleModelWrapper.bake(baker, model, modelState));
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        resolver.markDependency(baseModel);
        for (int edge = 0; edge < 4; edge++) {
            resolver.markDependency(LOCATIONS_SINGLE[edge]);
            resolver.markDependency(LOCATIONS_BUNDLED[edge]);
        }
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }
}
