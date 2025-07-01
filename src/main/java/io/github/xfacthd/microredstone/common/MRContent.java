package io.github.xfacthd.microredstone.common;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.block.MicrochipBlock;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.util.registration.DeferredBlockEntity;
import io.github.xfacthd.microredstone.common.util.registration.DeferredBlockEntityRegister;
import io.github.xfacthd.microredstone.common.util.registration.DeferredDataComponentTypeRegister;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class MRContent
{
    // region Registries
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MicroRedstone.MOD_ID);
    private static final DeferredDataComponentTypeRegister DATA_COMPONENTS = DeferredDataComponentTypeRegister.create(MicroRedstone.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MicroRedstone.MOD_ID);
    private static final DeferredBlockEntityRegister BLOCK_ENTITIES = DeferredBlockEntityRegister.create(MicroRedstone.MOD_ID);
    // endregion

    // region Blocks
    public static final Holder<Block> BLOCK_MICROCHIP = registerBlock("microchip", MicrochipBlock::new);
    // endregion

    // region Data Components

    // endregion

    // region Items

    // endregion

    // region BlockEntityTypes
    public static final DeferredBlockEntity<MicrochipBlockEntity> BLOCK_ENTITY_MICROCHIP = registerBlockEntity(
            "microchip", MicrochipBlockEntity::new, BLOCK_MICROCHIP
    );
    // endregion

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name, Function<BlockBehaviour.Properties, T> blockFactory
    )
    {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, blockFactory, BlockBehaviour.Properties.of());
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    @SafeVarargs
    private static <T extends BlockEntity> DeferredBlockEntity<T> registerBlockEntity(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Holder<Block>... blocks
    )
    {
        Supplier<Set<Block>> blockSet = () -> Arrays.stream(blocks).map(Holder::value).collect(Collectors.toSet());
        return BLOCK_ENTITIES.registerBlockEntity(name, factory, blockSet, false);
    }

    public static void init(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private MRContent() { }
}
