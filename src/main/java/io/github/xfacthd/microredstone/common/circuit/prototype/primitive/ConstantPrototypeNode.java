package io.github.xfacthd.microredstone.common.circuit.prototype.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.NodeError;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ConstantCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;

import java.util.List;
import java.util.Map;

public final class ConstantPrototypeNode extends PrototypeNode {
    private static final PortConfig PORTS_SINGLE = PortConfig.builder()
            .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
            .build();
    private static final PortConfig PORTS_BUNDLED = PortConfig.builder()
            .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
            .build();
    public static final IconConfig ICON_SINGLE = new IconConfig(Utils.id("part/constant"), Utils.id("port/right_single"), false);
    public static final IconConfig ICON_BUNDLED = new IconConfig(Utils.id("part/constant"), Utils.id("port/right_bundled"), false);
    public static final int MAX_VAL_SINGLE = 1;
    public static final int MAX_VAL_BUNDLED = 65535;

    private final WireType wireType;
    private int value = 0;

    public ConstantPrototypeNode(WireType wireType) {
        super(wireType.select(PORTS_SINGLE, PORTS_BUNDLED), wireType.select(ICON_SINGLE, ICON_BUNDLED));
        this.wireType = wireType;
    }

    private ConstantPrototypeNode(WireType wireType, int value) {
        this(wireType);
        this.value = value;
    }

    public WireType getWireType() {
        return wireType;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    protected void validateInternal(CircuitErrorCollector errors) {
        int maxValue = wireType.select(MAX_VAL_SINGLE, MAX_VAL_BUNDLED);
        if (value < 0 || value > maxValue) {
            errors.submit(new NodeError.InvalidConstantValue(this, value));
        }
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper) {
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector output = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, wireType);
        return new ConstantCircuitNode((short) value, output);
    }

    @Override
    public Serializable serialize(List<Wire> wires) {
        return new Serializable(this, wires, wireType, value);
    }

    public static IconConfig icon(ConstantCircuitNode node) {
        return node.getOutputs()[0].type().select(ICON_SINGLE, ICON_BUNDLED);
    }

    public static final class Serializable extends PrototypeNode.Serializable {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                WireType.CODEC.fieldOf("wire_type").forGetter(node -> node.wireType),
                Codec.INT.fieldOf("value").forGetter(node -> node.value)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final WireType wireType;
        private final int value;

        private Serializable(PrototypeNode node, List<Wire> wires, WireType wireType, int value) {
            super(node, wires);
            this.wireType = wireType;
            this.value = value;
        }

        private Serializable(WireType wireType, int value, Map<Port, Integer> connectedWires, NodePos pos, int rotation) {
            super(connectedWires, pos, rotation);
            this.wireType = wireType;
            this.value = value;
        }

        @Override
        protected PrototypeNode buildInternal() {
            return new ConstantPrototypeNode(wireType, value);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type() {
            return MRContent.PROTO_TYPE_CONSTANT.value();
        }
    }
}
