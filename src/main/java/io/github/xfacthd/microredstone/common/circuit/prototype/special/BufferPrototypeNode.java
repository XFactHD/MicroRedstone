package io.github.xfacthd.microredstone.common.circuit.prototype.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;

import java.util.List;
import java.util.Map;

public final class BufferPrototypeNode extends PrototypeNode {
    private static final PortConfig PORTS_SINGLE = PortConfig.builder()
            .addPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
            .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
            .build();
    private static final PortConfig PORTS_BUNDLED = PortConfig.builder()
            .addPort(Port.LEFT, WireType.BUNDLED, PortDir.INPUT)
            .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
            .build();
    public static final IconConfig ICON_SINGLE = IconConfig.of(Utils.id("part/buffer"), PORTS_SINGLE);
    public static final IconConfig ICON_BUNDLED = IconConfig.of(Utils.id("part/buffer"), PORTS_BUNDLED);

    private final WireType wireType;

    public BufferPrototypeNode(WireType wireType) {
        super(wireType.select(PORTS_SINGLE, PORTS_BUNDLED), wireType.select(ICON_SINGLE, ICON_BUNDLED));
        this.wireType = wireType;
    }

    @Override
    public BufferCircuitNode assemble(WireMapper wireMapper) {
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        return new BufferCircuitNode(
                new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, wireType),
                new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, wireType)
        );
    }

    @Override
    public Serializable serialize(List<Wire> wires) {
        return new Serializable(this, wires, wireType);
    }

    public static IconConfig icon(BufferCircuitNode node) {
        return node.getInputs()[0].type().select(ICON_SINGLE, ICON_BUNDLED);
    }

    public static final class Serializable extends PrototypeNode.Serializable {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                WireType.CODEC.fieldOf("wire_type").forGetter(node -> node.wireType)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final WireType wireType;

        private Serializable(PrototypeNode node, List<Wire> wires, WireType wireType) {
            super(node, wires);
            this.wireType = wireType;
        }

        private Serializable(WireType wireType, Map<Port, Integer> connectedWires, NodePos pos, int rotation) {
            super(connectedWires, pos, rotation);
            this.wireType = wireType;
        }

        @Override
        protected PrototypeNode buildInternal() {
            return new BufferPrototypeNode(wireType);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type() {
            return MRContent.PROTO_TYPE_BUFFER.value();
        }
    }
}
