package io.github.xfacthd.microredstone.common.circuit.prototype.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.NotLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.PrimitiveCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ThreeInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.TwoInputLogicCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class PrimitivePrototypeNode extends PrototypeNode
{
    private final Type type;
    private final int inputCount;
    private final WireType wireType;

    public PrimitivePrototypeNode(Type type, int inputCount, WireType wireType)
    {
        super(type.portConfig(inputCount, wireType), type.icon(inputCount, wireType));
        type.validateInputCount(inputCount);
        this.type = type;
        this.inputCount = inputCount;
        this.wireType = wireType;
    }

    public Type getType()
    {
        return type;
    }

    public boolean isMultiBit()
    {
        return wireType == WireType.BUNDLED;
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        List<Connector> inputConnectors = new ArrayList<>();
        portConfig.getPortsWithDir(PortDir.INPUT).forEach(port ->
        {
            int wire = wireMapper.resolveWire(getWireOrThrow(port));
            inputConnectors.add(new Connector(getPos(), port, wire, PortDir.INPUT, wireType));
        });
        int outWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector outputConnector = new Connector(getPos(), Port.RIGHT, outWire, PortDir.OUTPUT, wireType);
        return switch (inputCount)
        {
            case 1 ->
            {
                if (type != Type.NOT)
                {
                    throw new IllegalStateException("Invalid single-input logic op: " + type);
                }
                yield new NotLogicCircuitNode(isMultiBit(), inputConnectors.getFirst(), outputConnector);
            }
            case 2 -> new TwoInputLogicCircuitNode(type, isMultiBit(), inputConnectors, outputConnector);
            case 3 -> new ThreeInputLogicCircuitNode(type, isMultiBit(), inputConnectors, outputConnector);
            default -> throw new IllegalStateException("Invalid input count: " + inputCount);
        };
    }

    @Override
    public Serializable serialize(List<Wire> wires)
    {
        return new Serializable(this, wires, type, inputCount, wireType);
    }

    public static IconConfig icon(PrimitiveCircuitNode node)
    {
        return switch (node)
        {
            case NotLogicCircuitNode not -> Type.NOT.icon(1, not.getInputs()[0].type());
            case TwoInputLogicCircuitNode logicTwo -> logicTwo.getType().icon(2, logicTwo.getInputs()[0].type());
            case ThreeInputLogicCircuitNode logicThree -> logicThree.getType().icon(3, logicThree.getInputs()[0].type());
            default -> throw new IllegalStateException("Unexpected value: " + node);
        };
    }

    public enum Type implements StringRepresentable
    {
        NOT(false, false, Utils.rl("part/not")),
        AND(true, false, Utils.rl("part/and")),
        OR(true, false, Utils.rl("part/or")),
        XOR(true, false, Utils.rl("part/xor")),
        NAND(true, true, Utils.rl("part/nand")),
        NOR(true, true, Utils.rl("part/nor")),
        XNOR(true, true, Utils.rl("part/xnor")),
        ;

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        private static final IntFunction<Type> BY_ID = ByIdMap.continuous(Type::ordinal, Type.values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Type::ordinal);

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final boolean multiInput;
        private final boolean invertsResult;
        private final IntFunction<PortConfig> portsSingle;
        private final IntFunction<PortConfig> portsBundled;
        private final IntFunction<IconConfig> iconSingle;
        private final IntFunction<IconConfig> iconBundled;

        Type(boolean multiInput, boolean invertsResult, ResourceLocation icon)
        {
            this.multiInput = multiInput;
            this.invertsResult = invertsResult;
            if (multiInput)
            {
                PortConfig portsSingleTwo = PortConfig.builder()
                        .addPort(Port.UP, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.DOWN, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
                        .build();
                PortConfig portsSingleThree = PortConfig.builder()
                        .addPort(Port.UP, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.DOWN, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
                        .build();
                PortConfig portsBundledTwo = PortConfig.builder()
                        .addPort(Port.UP, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.DOWN, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
                        .build();
                PortConfig portsBundledThree = PortConfig.builder()
                        .addPort(Port.UP, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.LEFT, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.DOWN, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
                        .build();
                this.portsSingle = inputs -> inputs == 2 ? portsSingleTwo : portsSingleThree;
                this.portsBundled = inputs -> inputs == 2 ? portsBundledTwo : portsBundledThree;
                IconConfig iconSingleTwo = IconConfig.of(icon, portsSingleTwo);
                IconConfig iconBundledTwo = IconConfig.of(icon, portsBundledTwo);
                IconConfig iconSingleThree = IconConfig.of(icon, portsSingleThree);
                IconConfig iconBundledThree = IconConfig.of(icon, portsBundledThree);
                this.iconSingle = inputs -> inputs == 2 ? iconSingleTwo : iconSingleThree;
                this.iconBundled = inputs -> inputs == 2 ? iconBundledTwo : iconBundledThree;
            }
            else
            {
                PortConfig portsSingle = PortConfig.builder()
                        .addPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
                        .build();
                PortConfig portsBundled = PortConfig.builder()
                        .addPort(Port.LEFT, WireType.BUNDLED, PortDir.INPUT)
                        .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
                        .build();
                this.portsSingle = inputs -> portsSingle;
                this.portsBundled = inputs -> portsBundled;
                IconConfig iconSingle = IconConfig.of(icon, portsSingle);
                IconConfig iconBundled = IconConfig.of(icon, portsBundled);
                this.iconSingle = inputs -> iconSingle;
                this.iconBundled = inputs -> iconBundled;
            }
        }

        public boolean invertsResult()
        {
            return invertsResult;
        }

        public PortConfig portConfig(int inputCount, WireType wireType)
        {
            validateInputCount(inputCount);
            return wireType.select(portsSingle, portsBundled).apply(inputCount);
        }

        public IconConfig icon(int inputCount, WireType wireType)
        {
            validateInputCount(inputCount);
            return wireType.select(iconSingle, iconBundled).apply(inputCount);
        }

        public Supplier<PrototypeNode> factory(int inputCount, WireType wireType)
        {
            validateInputCount(inputCount);
            return () -> new PrimitivePrototypeNode(this, inputCount, wireType);
        }

        private void validateInputCount(int inputCount)
        {
            if ((inputCount > 1) != multiInput)
            {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid input count %d for type %s", inputCount, this));
            }
            if (inputCount < 1 || inputCount > 3)
            {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid input count %d, expected 1 <= count <= 3", inputCount));
            }
        }

        @Override
        public String getSerializedName()
        {
            return name;
        }
    }

    public static final class Serializable extends PrototypeNode.Serializable
    {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Type.CODEC.fieldOf("logic_type").forGetter(node -> node.type),
                Codec.INT.fieldOf("input_count").forGetter(node -> node.inputCount),
                WireType.CODEC.fieldOf("wire_type").forGetter(node -> node.wireType)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final Type type;
        private final int inputCount;
        private final WireType wireType;

        private Serializable(PrototypeNode node, List<Wire> wires, Type type, int inputCount, WireType wireType)
        {
            super(node, wires);
            this.type = type;
            this.inputCount = inputCount;
            this.wireType = wireType;
        }

        private Serializable(Type type, int inputCount, WireType wireType, Map<Port, Integer> connectedWires, NodePos pos, int rotation)
        {
            super(connectedWires, pos, rotation);
            this.type = type;
            this.inputCount = inputCount;
            this.wireType = wireType;
        }

        @Override
        protected PrototypeNode buildInternal()
        {
            return new PrimitivePrototypeNode(type, inputCount, wireType);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type()
        {
            return MRContent.PROTO_TYPE_PRIMITIVE.value();
        }
    }
}
