package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;

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

    public void setHalfPeriodLength(int halfCycleLength)
    {
        this.halfPeriodLength = halfCycleLength;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (!isConnected(Port.RIGHT)) reporter.report(() -> "Output unspecified");
    }

    @Override
    public ClockCircuitNode assemble(WireMapper wireMapper)
    {
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector outCon = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, WireType.SINGLE);
        return new ClockCircuitNode(halfPeriodLength, outCon);
    }
}
