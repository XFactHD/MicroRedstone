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
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplate;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.stream.Stream;

public final class MRBlockModelProvider extends ModelProvider
{
    private static final TextureSlot OVERLAY = TextureSlot.create("overlay");
    private static final Quadrant[] QUADRANTS = Quadrant.values();

    public MRBlockModelProvider(PackOutput output)
    {
        super(output, MicroRedstone.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels)
    {
        blockModels.createCraftingTableLike(MRContent.BLOCK_CIRCUIT_WORKBENCH.value(), Blocks.SMOOTH_STONE, TextureMapping::craftingTable);

        makeMicrochipBlockModel(blockModels);

        for (int edge = 0; edge < 4; edge++)
        {
            plateOverlay(blockModels, UnbakedMicrochipModel.LOCATIONS_SINGLE[edge], Utils.rl("block/overlay_single"), edge, true, true);
            plateOverlay(blockModels, UnbakedMicrochipModel.LOCATIONS_BUNDLED[edge], Utils.rl("block/overlay_bundled"), edge, true, true);
        }
    }

    private static void makeMicrochipBlockModel(BlockModelGenerators blockModels)
    {
        Identifier name = Utils.getKeyOrThrow(MRContent.BLOCK_MICROCHIP).identifier();
        Identifier baseLoc = name.withPrefix("block/");
        Identifier baseLocCircuit = baseLoc.withSuffix("_circuit");

        MultiVariantGenerator generator = MultiVariantGenerator.dispatch(MRContent.BLOCK_MICROCHIP.value())
                .with(PropertyDispatch.initial(BlockStateProperties.FACING, PropertyHolder.ROTATION, PropertyHolder.HAS_CIRCUIT).generate((dir, rot, hasCircuit) ->
                {
                    Identifier model = hasCircuit ? baseLocCircuit : baseLoc;

                    Quadrant rotX = switch (dir)
                    {
                        case DOWN, WEST, EAST -> Quadrant.R0;
                        case UP -> Quadrant.R180;
                        case NORTH -> Quadrant.R270;
                        case SOUTH -> Quadrant.R90;
                    };
                    Quadrant rotY = switch (dir)
                    {
                        case DOWN -> QUADRANTS[rot.ordinal()];
                        case UP, EAST -> QUADRANTS[(rot.ordinal() + 1) % 4];
                        case NORTH, SOUTH -> Quadrant.R0;
                        case WEST -> QUADRANTS[(rot.ordinal() + 3) % 4];
                    };
                    Quadrant rotZ = switch (dir)
                    {
                        case UP, DOWN -> Quadrant.R0;
                        case NORTH -> QUADRANTS[rot.ordinal()];
                        case SOUTH -> QUADRANTS[(6 - rot.ordinal()) % 4];
                        case WEST -> Quadrant.R90;
                        case EAST -> Quadrant.R270;
                    };

                    return MultiVariant.of(new UnbakedMicrochipModelBuilder(model, Variant.SimpleModelState.DEFAULT))
                            .with(VariantMutator.X_ROT.withValue(rotX))
                            .with(VariantMutator.Y_ROT.withValue(rotY))
                            .with(VariantMutator.Z_ROT.withValue(rotZ));
                }));
        blockModels.blockStateOutput.accept(generator);

        blockModels.registerSimpleItemModel(MRContent.BLOCK_MICROCHIP.value(), baseLocCircuit);
    }

    private static void plateOverlay(BlockModelGenerators blockModels, Identifier name, Identifier texture, int edge, boolean withSide, boolean mirrorTopX)
    {
        ExtendedModelTemplate template = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(OVERLAY)
                .requiredTextureSlot(TextureSlot.PARTICLE)
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

        Material material = new Material(texture);
        TextureMapping textures = new TextureMapping()
                .put(OVERLAY, material)
                .put(TextureSlot.PARTICLE, material);
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
}
