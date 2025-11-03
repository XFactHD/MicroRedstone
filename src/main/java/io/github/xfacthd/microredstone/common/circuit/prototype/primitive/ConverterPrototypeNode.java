package io.github.xfacthd.microredstone.common.circuit.prototype.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.InvalidConverterBitIndexProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConverterPrototypeNode extends PrototypeNode
{
    private final Type type;
    private int bitIndex = 0;

    public ConverterPrototypeNode(Type type)
    {
        super(type.portConfig, type.icon);
        this.type = type;
    }

    private ConverterPrototypeNode(Type type, int bitIndex)
    {
        this(type);
        this.bitIndex = bitIndex;
    }

    public Type getType()
    {
        return type;
    }

    public boolean isPacker()
    {
        return type == Type.PACK;
    }

    public void setBitIndex(int bitIndex)
    {
        this.bitIndex = bitIndex;
    }

    public int getBitIndex()
    {
        return bitIndex;
    }

    @Override
    protected void validateInternal(ProblemReporter reporter)
    {
        if (bitIndex < 0 || bitIndex > 15)
        {
            reporter.report(new InvalidConverterBitIndexProblem(this, bitIndex));
        }
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        Connector inputConnector = new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, type.inType);
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector outputConnector = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, type.outType);
        return switch (type)
        {
            case PACK -> new BundlePackerCircuitNode(bitIndex, inputConnector, outputConnector);
            case UNPACK -> new BundleUnpackerCircuitNode(bitIndex, inputConnector, outputConnector);
        };
    }

    @Override
    public Serializable serialize(List<Wire> wires)
    {
        return new Serializable(this, wires, type, bitIndex);
    }

    public enum Type implements StringRepresentable
    {
        PACK(WireType.SINGLE, WireType.BUNDLED, Utils.rl("part/packer")),
        UNPACK(WireType.BUNDLED, WireType.SINGLE, Utils.rl("part/unpacker"));

        private static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final WireType inType;
        private final WireType outType;
        private final PortConfig portConfig;
        private final IconConfig icon;

        Type(WireType inType, WireType outType, ResourceLocation icon)
        {
            this.inType = inType;
            this.outType = outType;
            this.portConfig = PortConfig.builder()
                    .addPort(Port.LEFT, inType, PortDir.INPUT)
                    .addPort(Port.RIGHT, outType, PortDir.OUTPUT)
                    .build();
            this.icon = IconConfig.of(icon, portConfig);
        }

        public IconConfig getIcon()
        {
            return icon;
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
                Type.CODEC.fieldOf("conv_type").forGetter(node -> node.type),
                Codec.intRange(0, 15).fieldOf("bit_index").forGetter(node -> node.bitIndex)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final Type type;
        private final int bitIndex;

        private Serializable(PrototypeNode node, List<Wire> wires, Type type, int bitIndex)
        {
            super(node, wires);
            this.type = type;
            this.bitIndex = bitIndex;
        }

        private Serializable(Type type, int bitIndex, Map<Port, Integer> connectedWires, NodePos pos, int rotation)
        {
            super(connectedWires, pos, rotation);
            this.type = type;
            this.bitIndex = bitIndex;
        }

        @Override
        protected PrototypeNode buildInternal()
        {
            return new ConverterPrototypeNode(type, bitIndex);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type()
        {
            return MRContent.PROTO_TYPE_CONVERTER.value();
        }
    }
}
