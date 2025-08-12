package io.github.xfacthd.microredstone.common.circuit.prototype;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PrototypeNode implements PlaceableNode
{
    protected final PortConfig portConfig;
    @Nullable
    private final IconConfig icon;
    private final BiMap<Port, Wire> connectedWires = EnumHashBiMap.create(Port.class);
    private NodePos pos = new NodePos(0, 0);
    private int rotation = 0;

    protected PrototypeNode(PortConfig portConfig, @Nullable IconConfig icon)
    {
        this.portConfig = portConfig;
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
        Port portNorm = port.rotate(-rotation);
        if (portConfig.hasPort(portNorm, wire.getWireType()))
        {
            if (connect)
            {
                connectedWires.put(portNorm, wire);
            }
            else
            {
                connectedWires.remove(portNorm);
            }
        }
    }

    @Override
    public final boolean hasPort(Port port, @Nullable WireType wireType)
    {
        return portConfig.hasPort(port.rotate(-rotation), wireType);
    }

    @Override
    public final boolean isConnected(Port port)
    {
        return connectedWires.containsKey(port.rotate(-rotation));
    }

    public final Wire getWireOrThrow(Port port)
    {
        return Objects.requireNonNull(connectedWires.get(port));
    }

    public final void replaceWire(Wire oldWire, Wire newWire)
    {
        connectedWires.replaceAll((port, wire) -> wire == oldWire ? newWire : wire);
    }

    public final void clearWires()
    {
        connectedWires.clear();
    }

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
        return connectedWires.values();
    }

    public final Set<Wire> getConnectedWires(PortDir dir)
    {
        return portConfig.getPortsWithDir(dir)
                .map(connectedWires::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public final WireType getOutputType(Wire wire)
    {
        Port port = connectedWires.inverse().get(wire);
        Objects.requireNonNull(port);
        return Objects.requireNonNull(portConfig.getPortType(port));
    }
}
