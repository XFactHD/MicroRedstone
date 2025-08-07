package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

// TODO: - move port presence, port types, port directions and port overlay RL to a PortConfig object passed to ctor
//       - change connectedWires to BiMap<Wire, Port>
public abstract class PrototypeNode implements PlaceableNode
{
    @Nullable
    private final IconConfig icon;
    private final Set<Wire> connectedWires = new ReferenceOpenHashSet<>();
    private NodePos pos = new NodePos(0, 0);
    private int rotation = 0;

    protected PrototypeNode(@Nullable IconConfig icon)
    {
        this.icon = icon;
    }

    public final void setPos(NodePos pos)
    {
        this.pos = pos;
    }

    public final void setRotation(int rotation)
    {
        this.rotation = rotation;
    }

    public final void setConnection(Port port, Wire wire, boolean connect)
    {
        if (setConnectionInternal(port.rotate(-rotation), wire, connect))
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

    @Override
    public final boolean hasPort(Port port, @Nullable WireType wireType)
    {
        return hasPortInternal(port.rotate(-rotation), wireType);
    }

    protected abstract boolean hasPortInternal(Port port, @Nullable WireType wireType);

    @Override
    public final boolean isConnected(Port port)
    {
        return isConnectedInternal(port.rotate(-rotation));
    }

    protected abstract boolean isConnectedInternal(Port port);

    public abstract void replaceWire(Wire oldWire, Wire newWire);

    public abstract void clearWires();

    public abstract void validate(ProblemReporter reporter);

    public abstract CircuitNode assemble(WireMapper wireMapper);

    @Override
    public final NodePos getPos()
    {
        return pos;
    }

    @Override
    public final int getRotation()
    {
        return rotation;
    }

    @Override
    public final IconConfig getIcon()
    {
        return Objects.requireNonNull(icon);
    }

    public final Set<Wire> getConnectedWires()
    {
        return connectedWires;
    }

    public abstract Set<Wire> getConnectedInputWires();

    public abstract Set<Wire> getConnectedOutputWires();

    public abstract WireType getOutputType(Wire wire);
}
