package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

// TODO: add inhibit input (requires support for optional connections)
public final class ClockPrototypeNode extends PrototypeNode
{
    private int halfPeriodLength = 10;
    @Nullable
    private Wire output = null;

    public void setHalfPeriodLength(int halfCycleLength)
    {
        this.halfPeriodLength = halfCycleLength;
    }

    @Override
    protected boolean setConnectionInternal(Port port, Wire wire, boolean connect)
    {
        if (port == Port.RIGHT && wire.getWireType() == WireType.SINGLE)
        {
            output = connect ? wire : null;
            return true;
        }
        return false;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (output == null) reporter.report(() -> "Output unspecified");
    }

    @Override
    public ClockCircuitNode assemble(WireMapper wireMapper)
    {
        int outputWire = wireMapper.resolveWire(Objects.requireNonNull(output));
        Connector outCon = new Connector(Port.RIGHT, outputWire, PortDir.OUTPUT, WireType.SINGLE);
        return new ClockCircuitNode(halfPeriodLength, outCon);
    }

    @Override
    public Set<Wire> getConnectedInputWires()
    {
        return Set.of();
    }

    @Override
    public Set<Wire> getConnectedOutputWires()
    {
        return Set.of(Objects.requireNonNull(output));
    }

    @Override
    public WireType getOutputType(Wire wire)
    {
        return WireType.SINGLE;
    }
}
