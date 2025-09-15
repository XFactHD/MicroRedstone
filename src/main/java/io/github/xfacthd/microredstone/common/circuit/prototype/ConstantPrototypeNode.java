package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.problem.UnspecifiedConnectionProblem;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.ConstantCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;

public final class ConstantPrototypeNode extends PrototypeNode
{
    private static final PortConfig PORTS_SINGLE = PortConfig.builder()
            .addPort(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT)
            .build();
    private static final PortConfig PORTS_BUNDLED = PortConfig.builder()
            .addPort(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT)
            .build();
    public static final IconConfig ICON_SINGLE = new IconConfig(Utils.rl("part/constant"), Utils.rl("port/right_single"), false);
    public static final IconConfig ICON_BUNDLED = new IconConfig(Utils.rl("part/constant"), Utils.rl("port/right_bundled"), false);

    private final WireType wireType;
    private short value = 0;

    public ConstantPrototypeNode(WireType wireType)
    {
        super(wireType.select(PORTS_SINGLE, PORTS_BUNDLED), wireType.select(ICON_SINGLE, ICON_BUNDLED));
        this.wireType = wireType;
    }

    public WireType getWireType()
    {
        return wireType;
    }

    public short getValue()
    {
        return value;
    }

    public void setValue(short value)
    {
        this.value = value;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (!isConnectedNormalized(Port.RIGHT)) reporter.report(UnspecifiedConnectionProblem.output(Port.RIGHT));
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        int outputWire = wireMapper.resolveWire(getWireOrThrow(Port.RIGHT));
        Connector output = new Connector(getPos(), Port.RIGHT, outputWire, PortDir.OUTPUT, wireType);
        return new ConstantCircuitNode(value, output);
    }

    public static IconConfig icon(ConstantCircuitNode node)
    {
        return node.getOutputs()[0].type().select(ICON_SINGLE, ICON_BUNDLED);
    }
}
