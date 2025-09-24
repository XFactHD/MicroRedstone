package io.github.xfacthd.microredstone.common.circuit.prototype.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnspecifiedConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ProtoNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;

import java.util.List;
import java.util.Map;

// TODO: add inhibit input (requires support for optional connections)
public final class ClockPrototypeNode extends PrototypeNode
{
    private static final PortConfig PORT_CONFIG = PortConfig.builder()
            //.addOptionalPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
            .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
            .build();
    public static final IconConfig ICON = new IconConfig(Utils.rl("part/clock"), Utils.rl("port/right_single"), false);

    private int halfPeriodLength = 10;

    public ClockPrototypeNode()
    {
        super(PORT_CONFIG, ICON);
    }

    private ClockPrototypeNode(int halfPeriodLength)
    {
        this();
        this.halfPeriodLength = halfPeriodLength;
    }

    public int getHalfPeriodLength()
    {
        return halfPeriodLength;
    }

    public void setHalfPeriodLength(int halfCycleLength)
    {
        this.halfPeriodLength = halfCycleLength;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (!isConnectedNormalized(Port.RIGHT)) reporter.report(UnspecifiedConnectionProblem.output(Port.RIGHT));
    }

    @Override
    public ClockCircuitNode assemble(WireMapper wireMapper)
    {
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector outCon = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, WireType.SINGLE);
        return new ClockCircuitNode(halfPeriodLength, outCon);
    }

    @Override
    public Serializable serialize(List<Wire> wires)
    {
        return new Serializable(this, wires, halfPeriodLength);
    }

    public static final class Serializable extends PrototypeNode.Serializable
    {
        public static final MapCodec<Serializable> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.INT.fieldOf("half_period_length").forGetter(node -> node.halfPeriodLength)
        ).and(commonFields(inst)).apply(inst, Serializable::new));

        private final int halfPeriodLength;

        private Serializable(PrototypeNode node, List<Wire> wires, int halfPeriodLength)
        {
            super(node, wires);
            this.halfPeriodLength = halfPeriodLength;
        }

        private Serializable(int halfPeriodLength, Map<Port, Integer> connectedWires, NodePos pos, int rotation)
        {
            super(connectedWires, pos, rotation);
            this.halfPeriodLength = halfPeriodLength;
        }

        @Override
        protected PrototypeNode buildInternal()
        {
            return new ClockPrototypeNode(halfPeriodLength);
        }

        @Override
        public ProtoNodeType<? extends PrototypeNode.Serializable> type()
        {
            return MRContent.PROTO_TYPE_CLOCK.value();
        }
    }
}
