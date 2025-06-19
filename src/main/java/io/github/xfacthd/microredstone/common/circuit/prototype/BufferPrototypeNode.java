package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public final class BufferPrototypeNode extends PrototypeNode
{
    private final WireType wireType;
    @Nullable
    private Wire input = null;
    @Nullable
    private Wire output = null;

    public BufferPrototypeNode(WireType wireType)
    {
        this.wireType = wireType;
    }

    @Override
    protected boolean setConnectionInternal(Port port, Wire wire, boolean connect)
    {
        if (wire.getWireType() != wireType) return false;

        switch (port)
        {
            case Port.RIGHT -> input = connect ? wire : null;
            case Port.LEFT -> output = connect ? wire : null;
        }
        return port == Port.LEFT || port == Port.RIGHT;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (input == null) reporter.report(() -> "Input unspecified");
        if (output == null) reporter.report(() -> "Output unspecified");
        if (input == output) reporter.report(() -> "Connected to self");
    }

    @Override
    public BufferCircuitNode assemble(WireMapper wireMapper)
    {
        int inputWire = wireMapper.resolveWire(Objects.requireNonNull(input));
        int outputWire = wireMapper.resolveWire(Objects.requireNonNull(output));
        return new BufferCircuitNode(
                new Connector(Port.RIGHT, inputWire, PortDir.INPUT, wireType),
                new Connector(Port.LEFT, outputWire, PortDir.OUTPUT, wireType)
        );
    }

    @Override
    public Set<Wire> getConnectedInputWires()
    {
        return Set.of(Objects.requireNonNull(input));
    }

    @Override
    public Set<Wire> getConnectedOutputWires()
    {
        return Set.of(Objects.requireNonNull(output));
    }

    @Override
    public WireType getOutputType(Wire wire)
    {
        return wireType;
    }
}
