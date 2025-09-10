package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnspecifiedConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;

public final class BufferPrototypeNode extends PrototypeNode
{
    private static final PortConfig PORTS_SINGLE = PortConfig.builder()
            .addPort(Port.LEFT, WireType.SINGLE, PortDir.INPUT)
            .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
            .build();
    private static final PortConfig PORTS_BUNDLED = PortConfig.builder()
            .addPort(Port.LEFT, WireType.BUNDLED, PortDir.INPUT)
            .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
            .build();
    public static final IconConfig ICON_SINGLE = new IconConfig(Utils.rl("part/buffer"), Utils.rl("port/hor_single"));
    public static final IconConfig ICON_BUNDLED = new IconConfig(Utils.rl("part/buffer"), Utils.rl("port/hor_bundled"));

    private final WireType wireType;

    public BufferPrototypeNode(WireType wireType)
    {
        super(wireType.select(PORTS_SINGLE, PORTS_BUNDLED), wireType.select(ICON_SINGLE, ICON_BUNDLED));
        this.wireType = wireType;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (!isConnected(Port.LEFT)) reporter.report(UnspecifiedConnectionProblem.input(Port.LEFT));
        if (!isConnected(Port.RIGHT)) reporter.report(UnspecifiedConnectionProblem.output(Port.RIGHT));
    }

    @Override
    public BufferCircuitNode assemble(WireMapper wireMapper)
    {
        int inputWire = wireMapper.resolveWire(getWireOrThrow(Port.LEFT));
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        return new BufferCircuitNode(
                new Connector(getPos(), Port.LEFT, inputWire, PortDir.INPUT, wireType),
                new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, wireType)
        );
    }

    public static IconConfig icon(BufferCircuitNode node)
    {
        return node.getInputs()[0].type().select(ICON_SINGLE, ICON_BUNDLED);
    }
}
