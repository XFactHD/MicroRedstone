package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ProblemReporter;

import java.util.Set;

public abstract class PrototypeNode
{
    private final Set<Wire> connectedWires = new ReferenceOpenHashSet<>();
    private NodePos pos = new NodePos(0, 0);

    public void setPos(NodePos pos)
    {
        this.pos = pos;
    }

    public final void setConnection(Port port, Wire wire, boolean connect)
    {
        if (setConnectionInternal(port, wire, connect))
        {
            if (connect)
            {
                connectedWires.add(wire);
            }
            else
            {
                connectedWires.remove(wire);
            }
        }
    }

    protected abstract boolean setConnectionInternal(Port port, Wire wire, boolean connect);

    public abstract void validate(ProblemReporter reporter);

    public abstract CircuitNode assemble(WireMapper wireMapper);

    public NodePos getPos()
    {
        return pos;
    }

    public final Set<Wire> getConnectedWires()
    {
        return connectedWires;
    }

    public abstract Set<Wire> getConnectedInputWires();

    public abstract Set<Wire> getConnectedOutputWires();

    public abstract WireType getOutputType(Wire wire);
}
