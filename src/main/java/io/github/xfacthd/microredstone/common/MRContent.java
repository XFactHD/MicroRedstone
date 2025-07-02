package io.github.xfacthd.microredstone.common;

import com.mojang.serialization.MapCodec;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.block.MicrochipBlock;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.NotLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ThreeInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.TwoInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.data.MRRegistries;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.menu.MicrochipMenu;
import io.github.xfacthd.microredstone.common.util.registration.DeferredBlockEntity;
import io.github.xfacthd.microredstone.common.util.registration.DeferredBlockEntityRegister;
import io.github.xfacthd.microredstone.common.util.registration.DeferredDataComponentType;
import io.github.xfacthd.microredstone.common.util.registration.DeferredDataComponentTypeRegister;
import io.github.xfacthd.microredstone.common.util.registration.DeferredMenuType;
import io.github.xfacthd.microredstone.common.util.registration.DeferredMenuTypeRegister;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
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
    private static final DeferredMenuTypeRegister MENU_TYPES = DeferredMenuTypeRegister.create(MicroRedstone.MOD_ID);
    private static final DeferredRegister<CircuitNodeType<?>> CIRCUIT_NODES = DeferredRegister.create(MRRegistries.CIRCUIT_NODE_TYPES, MicroRedstone.MOD_ID);
    // endregion

    // region Blocks
    public static final Holder<Block> BLOCK_MICROCHIP = registerBlock("microchip", MicrochipBlock::new); // TODO: rename
    // endregion

    // region Data Components
    public static final DeferredDataComponentType<StoredCircuit> DC_TYPE_CIRCUIT = DATA_COMPONENTS.registerComponentType(
            "circuit", builder -> builder.persistent(StoredCircuit.CODEC).networkSynchronized(StoredCircuit.STREAM_CODEC).cacheEncoding()
    );
    // endregion

    // region Items
    public static final Holder<Item> ITEM_INTEGRATED_CIRCUIT = ITEMS.registerItem(
            "integrated_circuit",
            props -> new Item(props.component(DC_TYPE_CIRCUIT, StoredCircuit.EMPTY))
    );
    // endregion

    // region BlockEntityTypes
    public static final DeferredBlockEntity<MicrochipBlockEntity> BLOCK_ENTITY_MICROCHIP = registerBlockEntity(
            "microchip", MicrochipBlockEntity::new, BLOCK_MICROCHIP
    );
    // endregion

    // region MenuTypes
    public static final DeferredMenuType<MicrochipMenu> MENU_TYPE_MICROCHIP = MENU_TYPES.registerSimpleMenuType(
            "microchip", MicrochipMenu::createClient
    );
    // endregion

    // region CircuitNodeTypes
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_CLOCK = registerCircuitNodeType(
            "clock", ClockCircuitNode.CODEC, ClockCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_BUFFER = registerCircuitNodeType(
            "buffer", BufferCircuitNode.CODEC, BufferCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_COMPOUND = registerCircuitNodeType(
            "compound", CompoundCircuitNode.CODEC, CompoundCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_LOGIC_NOT = registerCircuitNodeType(
            "not", NotLogicCircuitNode.CODEC, NotLogicCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_LOGIC_TWO_INPUT = registerCircuitNodeType(
            "logic_two_input", TwoInputLogicCircuitNode.CODEC, TwoInputLogicCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_LOGIC_THREE_INPUT = registerCircuitNodeType(
            "logic_three_input", ThreeInputLogicCircuitNode.CODEC, ThreeInputLogicCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_BUNDLE_PACKER = registerCircuitNodeType(
            "bundle_packer", BundlePackerCircuitNode.CODEC, BundlePackerCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_BUNDLE_UNPACKER = registerCircuitNodeType(
            "bundle_unpacker", BundleUnpackerCircuitNode.CODEC, BundleUnpackerCircuitNode.STREAM_CODEC
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

    private static <T extends CircuitNode> Holder<CircuitNodeType<?>> registerCircuitNodeType(
            String name, MapCodec<T> codec, StreamCodec<ByteBuf, T> streamCodec
    )
    {
        return CIRCUIT_NODES.register(name, () -> new CircuitNodeType<>(codec, streamCodec));
    }

    public static void init(IEventBus modBus)
    {
        modBus.addListener(MRRegistries::onRegisterNewRegistries);

        BLOCKS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENU_TYPES.register(modBus);
        CIRCUIT_NODES.register(modBus);
    }

    private MRContent() { }
}
