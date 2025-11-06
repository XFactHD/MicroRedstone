package io.github.xfacthd.microredstone.common;

import com.mojang.serialization.MapCodec;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.block.CircuitWorkbenchBlock;
import io.github.xfacthd.microredstone.common.block.MicrochipBlock;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ConstantCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.NotLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ThreeInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.TwoInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.LampCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.data.MRRegistries;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.item.CircuitItem;
import io.github.xfacthd.microredstone.common.item.block.MicrochipBlockItem;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.menu.MicrochipCircuitMenu;
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
import net.minecraft.world.item.BlockItem;
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
import java.util.function.BiFunction;
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
    private static final DeferredRegister<ProtoNodeType<?>> PROTO_NODES = DeferredRegister.create(MRRegistries.PROTO_NODE_TYPES, MicroRedstone.MOD_ID);
    // endregion

    // region Blocks
    public static final Holder<Block> BLOCK_MICROCHIP = registerBlock("microchip", MicrochipBlock::new, MicrochipBlockItem::new); // TODO: rename
    public static final Holder<Block> BLOCK_CIRCUIT_WORKBENCH = registerBlock("circuit_workbench", CircuitWorkbenchBlock::new);
    // endregion

    // region Data Components
    public static final DeferredDataComponentType<StoredCircuit> DC_TYPE_CIRCUIT = DATA_COMPONENTS.registerComponentType(
            "circuit", builder -> builder.persistent(StoredCircuit.CODEC).networkSynchronized(StoredCircuit.STREAM_CODEC).cacheEncoding()
    );
    // endregion

    // region Items
    public static final Holder<Item> ITEM_INTEGRATED_CIRCUIT = ITEMS.registerItem(
            "integrated_circuit",
            props -> new CircuitItem(props.component(DC_TYPE_CIRCUIT, StoredCircuit.EMPTY))
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
    public static final DeferredMenuType<MicrochipCircuitMenu> MENU_TYPE_MICROCHIP_CIRCUIT = MENU_TYPES.registerAdvancedMenuType(
            "microchip_circuit", MicrochipCircuitMenu::createClient
    );
    public static final DeferredMenuType<CircuitWorkbenchMenu> MENU_TYPE_CIRCUIT_WORKBENCH = MENU_TYPES.registerSimpleMenuType(
            "circuit_workbench", CircuitWorkbenchMenu::createClient
    );
    // endregion

    // region CircuitNodeTypes
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_CONSTANT = registerCircuitNodeType(
            "constant", ConstantCircuitNode.CODEC, ConstantCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_CLOCK = registerCircuitNodeType(
            "clock", ClockCircuitNode.CODEC, ClockCircuitNode.STREAM_CODEC
    );
    public static final Holder<CircuitNodeType<?>> NODE_TYPE_LAMP = registerCircuitNodeType(
            "lamp", LampCircuitNode.CODEC, LampCircuitNode.STREAM_CODEC
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

    // region ProtoNodeTypes
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_CONSTANT = registerProtoNodeType(
            "constant", ConstantPrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_CLOCK = registerProtoNodeType(
            "clock", ClockPrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_LAMP = registerProtoNodeType(
            "lamp", LampPrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_BUFFER = registerProtoNodeType(
            "buffer", BufferPrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_REFERENCE = registerProtoNodeType(
            "reference", ReferencePrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_PRIMITIVE = registerProtoNodeType(
            "primitive", PrimitivePrototypeNode.Serializable.CODEC
    );
    public static final Holder<ProtoNodeType<?>> PROTO_TYPE_CONVERTER = registerProtoNodeType(
            "converter", ConverterPrototypeNode.Serializable.CODEC
    );
    // endregion

    private static <B extends Block> DeferredBlock<B> registerBlock(String name, BlockFactory<B> blockFactory)
    {
        return registerBlock(name, blockFactory, BlockItem::new);
    }

    private static <B extends Block> DeferredBlock<B> registerBlock(String name, BlockFactory<B> blockFactory, BlockItemFactory<B> itemFactory)
    {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, blockFactory, BlockBehaviour.Properties.of());
        ITEMS.registerItem(name, props -> itemFactory.apply(block.value(), props.useBlockDescriptionPrefix()));
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

    private static <T extends PrototypeNode.Serializable> Holder<ProtoNodeType<?>> registerProtoNodeType(String name, MapCodec<T> codec)
    {
        return PROTO_NODES.register(name, () -> new ProtoNodeType<>(codec));
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
        PROTO_NODES.register(modBus);
    }

    @FunctionalInterface
    private interface BlockFactory<B extends Block> extends Function<BlockBehaviour.Properties, B>
    {
        @Override
        B apply(BlockBehaviour.Properties properties);
    }

    @FunctionalInterface
    private interface BlockItemFactory<B extends Block> extends BiFunction<B, Item.Properties, BlockItem>
    {
        @Override
        BlockItem apply(B block, Item.Properties properties);
    }

    private MRContent() { }
}
