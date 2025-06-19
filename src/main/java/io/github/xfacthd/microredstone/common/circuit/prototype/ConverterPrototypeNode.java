package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundlePackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.primitive.BundleUnpackerCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public final class ConverterPrototypeNode extends PrototypeNode
{
    private final Type type;
    private int bitIndex = 0;
    @Nullable
    private Wire input = null;
    @Nullable
    private Wire output = null;

    public ConverterPrototypeNode(Type type)
    {
        this.type = type;
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
    protected boolean setConnectionInternal(Port port, Wire wire, boolean connect)
    {
        if (port == Port.LEFT && wire.getWireType() == type.inType)
        {
            input = connect ? wire : null;
            return true;
        }
        if (port == Port.RIGHT && wire.getWireType() == type.outType)
        {
            output = connect ? wire : null;
            return true;
        }
        return false;
    }

    @Override
    public void validate(ProblemReporter reporter)
    {
        if (input == null) reporter.report(() -> "Input unspecified");
        if (output == null) reporter.report(() -> "Output unspecified");
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        int inputWire = wireMapper.resolveWire(Objects.requireNonNull(input));
        Connector inputConnector = new Connector(Port.LEFT, inputWire, PortDir.INPUT, type.inType);
        int outputWire = wireMapper.resolveWire(Objects.requireNonNull(output));
        Connector outputConnector = new Connector(Port.RIGHT, outputWire, PortDir.OUTPUT, type.outType);
        return switch (type)
        {
            case PACK -> new BundlePackerCircuitNode(bitIndex, inputConnector, outputConnector);
            case UNPACK -> new BundleUnpackerCircuitNode(bitIndex, inputConnector, outputConnector);
        };
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
        if (wire == input) return type.inType;
        if (wire == output) return type.outType;
        throw new IllegalArgumentException("Invalid wire: " + wire);
    }

    public enum Type
    {
        PACK(WireType.SINGLE, WireType.BUNDLED),
        UNPACK(WireType.BUNDLED, WireType.SINGLE);

        private final WireType inType;
        private final WireType outType;

        Type(WireType inType, WireType outType)
        {
            this.inType = inType;
            this.outType = outType;
        }
    }
}
