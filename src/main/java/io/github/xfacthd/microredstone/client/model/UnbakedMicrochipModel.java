package io.github.xfacthd.microredstone.client.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

public final class UnbakedMicrochipModel implements CustomUnbakedBlockStateModel
{
    public static final MapCodec<UnbakedMicrochipModel> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("model").forGetter(model -> model.baseModel),
            Variant.SimpleModelState.MAP_CODEC.forGetter(model -> model.variantState)
    ).apply(inst, UnbakedMicrochipModel::new));
    private static final String[] EDGE_SUFFIXES = new String[] { "n", "e", "s", "w" };
    public static final ResourceLocation[] LOCATIONS_SINGLE = Utils.fillArray(new ResourceLocation[4], edge ->
            Utils.rl("block/type_single_" + EDGE_SUFFIXES[edge])
    );
    public static final ResourceLocation[] LOCATIONS_BUNDLED = Utils.fillArray(new ResourceLocation[4], edge ->
            Utils.rl("block/type_bundled_" + EDGE_SUFFIXES[edge])
    );

    private final ResourceLocation baseModel;
    private final Variant.SimpleModelState variantState;
    private final ModelState modelState;

    UnbakedMicrochipModel(ResourceLocation baseModel, Variant.SimpleModelState variantState)
    {
        this.baseModel = baseModel;
        this.variantState = variantState;
        this.modelState = variantState.asModelState();
    }

    @Override
    public BlockStateModel bake(ModelBaker baker)
    {
        BlockStateModel[] singleModels = new BlockStateModel[4];
        BlockStateModel[] bundledModels = new BlockStateModel[4];

        Transformation xform = baker.getModel(baseModel)
                .getTopAdditionalProperties()
                .getOrDefault(NeoForgeModelProperties.TRANSFORM, Transformation.identity());
        ModelState xformModelState = UnbakedElementsHelper.composeRootTransformIntoModelState(modelState, xform);
        for (int edge = 0; edge < 4; edge++)
        {
            int outEdge = (edge + 2) % 4;
            singleModels[outEdge] = bakePart(baker, LOCATIONS_SINGLE[edge], xformModelState);
            bundledModels[outEdge] = bakePart(baker, LOCATIONS_BUNDLED[edge], xformModelState);
        }

        return new MicrochipBlockStateModel(bakePart(baker, baseModel, modelState), singleModels, bundledModels);
    }

    private static BlockStateModel bakePart(ModelBaker baker, ResourceLocation model, ModelState modelState)
    {
        return new SingleVariant(SimpleModelWrapper.bake(baker, model, modelState));
    }

    @Override
    public void resolveDependencies(Resolver resolver)
    {
        resolver.markDependency(baseModel);
        for (int edge = 0; edge < 4; edge++)
        {
            resolver.markDependency(LOCATIONS_SINGLE[edge]);
            resolver.markDependency(LOCATIONS_BUNDLED[edge]);
        }
    }

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec()
    {
        return CODEC;
    }
}
