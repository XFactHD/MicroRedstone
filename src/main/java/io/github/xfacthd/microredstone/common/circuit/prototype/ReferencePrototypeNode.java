package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

// TODO: find a way to implement nesting properly
public final class ReferencePrototypeNode extends PrototypeNode
{
    // TODO: handle dynamic port overlay for
    private static final IconConfig ICON = new IconConfig(Utils.rl("part/reference"), Utils.rl("port/full_single"));

    private final CompoundPrototypeNode referenced;
    private final @Nullable Wire[] portWires = new Wire[4];

    public ReferencePrototypeNode(CompoundPrototypeNode referenced)
    {
        super(makeIconConfig(referenced));
        this.referenced = referenced;
    }

    @Override
    protected boolean setConnectionInternal(Port port, Wire wire, boolean connect)
    {
        return false;
    }

    @Override
    protected boolean hasPortInternal(Port port, @Nullable WireType wireType)
    {
        return false;
    }

    @Override
    protected boolean isConnectedInternal(Port port)
    {
        return false;
    }

    @Override
    public void replaceWire(Wire oldWire, Wire newWire)
    {

    }

    @Override
    public void clearWires()
    {

    }

    @Override
    public void validate(ProblemReporter reporter)
    {

    }

    public Wire getPortWire(Port port)
    {
        return Objects.requireNonNull(portWires[port.ordinal()]);
    }

    @Override
    public CompoundCircuitNode assemble(WireMapper wireMapper)
    {
        return null;
    }

    @Override
    public Set<Wire> getConnectedInputWires()
    {
        return Set.of();
    }

    @Override
    public Set<Wire> getConnectedOutputWires()
    {
        return Set.of();
    }

    @Override
    public WireType getOutputType(Wire wire)
    {
        throw new UnsupportedOperationException();
    }

    private static IconConfig makeIconConfig(CompoundPrototypeNode referenced)
    {
        // TODO: compute port overlay from configured ports in the referenced node
        return ICON;
    }
}
