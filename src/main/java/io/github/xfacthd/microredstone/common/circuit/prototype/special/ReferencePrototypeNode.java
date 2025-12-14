package io.github.xfacthd.microredstone.common.circuit.prototype.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class ReferencePrototypeNode extends PrototypeNode
{
    private final CompoundCircuitNode referenced;

    public static ReferencePrototypeNode create(CompoundCircuitNode referenced, @Nullable IconConfig icon)
    {
        return new ReferencePrototypeNode(referenced, makePortConfig(referenced), icon);
    }

    private ReferencePrototypeNode(CompoundCircuitNode referenced, PortConfig portConfig, @Nullable IconConfig icon)
    {
        super(portConfig, icon != null ? icon : makeIconConfig(portConfig));
        this.referenced = referenced;
    }

    @Override
    public CompoundCircuitNode assemble(WireMapper wireMapper)
    {
        return referenced;
    }

    @Override
    public Serializable serialize(List<Wire> wires)
    {
        return new Serializable(this, wires, referenced);
    }

    private static PortConfig makePortConfig(CompoundCircuitNode referenced)
    {
        PortConfig.Builder<?> builder = PortConfig.builder();
        for (Connector connector : Utils.concatArrays(referenced.getInputs(), referenced.getOutputs()))
        {
            builder.addPort(connector.port(), connector.type(), connector.dir());
        }
        return builder.build();
    }

    public static IconConfig makeIconConfig(CompoundCircuitNode referenced)
    {
        return makeIconConfig(makePortConfig(referenced));
    }

    private static IconConfig makeIconConfig(PortConfig portConfig)
    {
        return IconConfig.of(Utils.rl("part/reference"), portConfig);
    }

    public static final class Serializable extends PrototypeNode.Serializable
    {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                CompoundCircuitNode.CODEC.fieldOf("referenced").forGetter(node -> node.referenced)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final CompoundCircuitNode referenced;

        private Serializable(PrototypeNode node, List<Wire> wires, CompoundCircuitNode referenced)
        {
            super(node, wires);
            this.referenced = referenced;
        }

        private Serializable(CompoundCircuitNode referenced, Map<Port, Integer> connectedWires, NodePos pos, int rotation)
        {
            super(connectedWires, pos, rotation);
            this.referenced = referenced;
        }

        @Override
        protected PrototypeNode buildInternal()
        {
            return create(referenced, null);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type()
        {
            return MRContent.PROTO_TYPE_REFERENCE.value();
        }
    }
}
