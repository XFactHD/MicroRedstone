package io.github.xfacthd.microredstone.common.datagen.providers;

import com.mojang.math.Quadrant;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.model.UnbakedMicrochipModel;
import io.github.xfacthd.microredstone.client.model.UnbakedMicrochipModelBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.data.PropertyHolder;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplate;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import org.joml.Vector3f;

import java.util.stream.Stream;

public final class MRBlockModelProvider extends ModelProvider
{
    private static final TextureSlot OVERLAY = TextureSlot.create("overlay");

    public MRBlockModelProvider(PackOutput output)
    {
        super(output, MicroRedstone.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels)
    {
        makeBlockModelWithZRotation(blockModels, MRContent.BLOCK_MICROCHIP, model ->
                MultiVariant.of(new UnbakedMicrochipModelBuilder(model, Variant.SimpleModelState.DEFAULT))
        );

        for (int edge = 0; edge < 4; edge++)
        {
            plateOverlay(blockModels, UnbakedMicrochipModel.LOCATIONS_SINGLE[edge], Utils.rl("block/overlay_single"), edge, true, true);
            plateOverlay(blockModels, UnbakedMicrochipModel.LOCATIONS_BUNDLED[edge], Utils.rl("block/overlay_bundled"), edge, true, true);
        }
    }

    private static void makeBlockModelWithZRotation(BlockModelGenerators blockModels, Holder<Block> block)
    {
        makeBlockModelWithZRotation(blockModels, block, BlockModelGenerators::plainVariant);
    }

    private static void makeBlockModelWithZRotation(BlockModelGenerators blockModels, Holder<Block> block, VariantGenerator variantGenerator)
    {
        ResourceLocation name = Utils.getKeyOrThrow(block).location();
        ResourceLocation baseLoc = name.withPrefix("block/");

        ResourceLocation[] models = new ResourceLocation[] {
                baseLoc,
                rotateAroundZ(blockModels, baseLoc, baseLoc.withSuffix("_cw90"), 90),
                rotateAroundZ(blockModels, baseLoc, baseLoc.withSuffix("_cw180"), 180),
                rotateAroundZ(blockModels, baseLoc, baseLoc.withSuffix("_ccw90"), -90)
        };

        MultiVariantGenerator generator = MultiVariantGenerator.dispatch(block.value())
                .with(PropertyDispatch.initial(PropertyHolder.ROTATION).generate(rot ->
                        variantGenerator.apply(models[rot.ordinal()])
                ))
                .with(PropertyDispatch.modify(BlockStateProperties.FACING).generate(dir ->
                {
                    Quadrant rotX = switch (dir)
                    {
                        case UP -> Quadrant.R180;
                        case DOWN -> Quadrant.R0;
                        default -> Quadrant.R90;
                    };
                    Quadrant rotY = Quadrant.R0;
                    if (dir.getAxis() != Direction.Axis.Y)
                    {
                        rotY = Quadrant.values()[(int) dir.toYRot() / 90];
                    }
                    return VariantMutator.X_ROT.withValue(rotX).then(VariantMutator.Y_ROT.withValue(rotY));
                }));
        blockModels.blockStateOutput.accept(generator);

        blockModels.registerSimpleItemModel(block.value(), baseLoc);
    }

    private static ResourceLocation rotateAroundZ(BlockModelGenerators blockModels, ResourceLocation converter, ResourceLocation name, int rot)
    {
        ModelTemplate template = ExtendedModelTemplateBuilder.builder()
                .parent(converter)
                .rootTransforms(xforms ->
                        xforms.origin(new Vector3f(.5F, 0, .5F))
                                .rotation(0, rot, 0, true)
                )
                .build();

        return template.create(name, new TextureMapping(), blockModels.modelOutput);
    }

    private static void plateOverlay(BlockModelGenerators blockModels, ResourceLocation name, ResourceLocation texture, int edge, boolean withSide, boolean mirrorTopX)
    {
        ExtendedModelTemplate template = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(OVERLAY)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .renderType("minecraft:cutout")
                .element(element ->
                {
                    element.from(0, 0, 0)
                            .to(16, 2, 16)
                            .face(Direction.UP, face ->
                                    face.uvs(0, mirrorTopX ? 16 : 0, 16, mirrorTopX ? 0 : 16)
                                            .rotation(Quadrant.values()[edge])
                                            .texture(OVERLAY)
                            );

                    if (withSide)
                    {
                        Direction edgeDir = Direction.from2DDataValue(edge);
                        element.face(edgeDir, face ->
                                face.cullface(edgeDir)
                                        .uvs(0, 0, 16, 2)
                                        .texture(OVERLAY)
                        );
                    }
                })
                .build();

        TextureMapping textures = new TextureMapping().put(OVERLAY, texture).put(TextureSlot.PARTICLE, texture);
        template.create(name, textures, blockModels.modelOutput);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems()
    {
        return super.getKnownItems().filter(item -> item.value() instanceof BlockItem);
    }

    @Override
    public String getName()
    {
        return "Block Models - MicroRedstone";
    }

    @FunctionalInterface
    private interface VariantGenerator
    {
        MultiVariant apply(ResourceLocation model);
    }
}
