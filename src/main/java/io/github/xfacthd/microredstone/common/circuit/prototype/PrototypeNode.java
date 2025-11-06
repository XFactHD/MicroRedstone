package io.github.xfacthd.microredstone.common.circuit.prototype;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartSetMode;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.NodeError;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import io.github.xfacthd.microredstone.common.data.MRRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PrototypeNode implements PlaceableNode
{
    private static final Port[] PORTS = Port.values();

    protected final PortConfig portConfig;
    @Nullable
    private final IconConfig icon;
    final Map<Port, Wire> connectedWires = new EnumMap<>(Port.class);
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
        return isConnectedNormalized(port.rotate(-rotation));
    }

    protected final boolean isConnectedNormalized(Port port)
    {
        return connectedWires.containsKey(port);
    }

    public final Wire getWireOrThrow(Port port)
    {
        return Objects.requireNonNull(connectedWires.get(port));
    }

    @Nullable
    public final Wire getWireOptional(Port port)
    {
        return connectedWires.get(port);
    }

    public final void replaceWire(Wire oldWire, Wire newWire)
    {
        connectedWires.replaceAll((port, wire) -> wire == oldWire ? newWire : wire);
    }

    public final void removeConnectedWire(Wire wire)
    {
        connectedWires.values().removeIf(mapWire -> mapWire == wire);
    }

    public final void clearWires()
    {
        connectedWires.clear();
    }

    public final void validate(CircuitErrorCollector errors)
    {
        for (Port port : PORTS)
        {
            boolean hasPort = portConfig.hasPort(port);
            boolean connected = isConnectedNormalized(port);
            if (!hasPort)
            {
                if (connected)
                {
                    errors.submit(new NodeError.UnexpectedConnection(this, port));
                }
                continue;
            }
            if (!connected)
            {
                if (portConfig.isRequired(this, port))
                {
                    PortDir portDir = Objects.requireNonNull(portConfig.getPortDir(port));
                    errors.submit(new NodeError.MissingConnection(this, port, portDir));
                }
                continue;
            }
            Wire wire = getWireOrThrow(port);
            WireType portType = Objects.requireNonNull(portConfig.getPortType(port));
            if (wire.getWireType() != portType)
            {
                errors.submit(new NodeError.MismatchedConnection(this, port, portType, wire.getWireType()));
            }
        }
        validateInternal(errors);
    }

    protected void validateInternal(CircuitErrorCollector errors) {}

    @Nullable
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
        return Set.copyOf(connectedWires.values());
    }

    public final Set<Wire> getConnectedWires(PortDir dir)
    {
        return portConfig.getPortsWithDir(dir)
                .map(connectedWires::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void performPostPlaceAction(CircuitCanvasAccess canvas, int mouseX, int mouseY, @Nullable PartSetMode mode, boolean revertToLast) {}

    public void performPreRemoveAction(CircuitCanvasAccess canvas) {}

    public abstract Serializable serialize(List<Wire> wires);

    public void finishDeserialization(List<PrototypeNode> nodes) {}

    public abstract static class Serializable
    {
        public static final Codec<Serializable> CODEC = MRRegistries.PROTO_NODE_TYPES.byNameCodec()
                .dispatch(PrototypeNode.Serializable::type, ProtoNodeType::codec);

        final Map<Port, Integer> connectedWires;
        final NodePos pos;
        final int rotation;

        protected Serializable(PrototypeNode node, List<Wire> wires)
        {
            this.connectedWires = new EnumMap<>(Port.class);
            node.connectedWires.forEach((port, wire) ->
            {
                int wireIdx = wires.indexOf(wire);
                if (wireIdx >= 0)
                {
                    connectedWires.put(port, wireIdx);
                }
            });
            this.pos = node.getPos();
            this.rotation = node.getRotation();
        }

        protected Serializable(Map<Port, Integer> connectedWires, NodePos pos, int rotation)
        {
            this.connectedWires = connectedWires;
            this.pos = pos;
            this.rotation = rotation;
        }

        public final PrototypeNode build(List<Wire> wires)
        {
            PrototypeNode node = buildInternal();
            connectedWires.forEach((port, wireIdx) ->
            {
                Wire wire = wires.get(wireIdx);
                if (wire != null)
                {
                    node.setConnection(port, wire, true);
                }
            });
            node.setPos(pos);
            node.setRotation(rotation);
            return node;
        }

        protected abstract PrototypeNode buildInternal();

        public abstract ProtoNodeType<? extends Serializable> type();

        protected static <T extends Serializable> Products.P3<RecordCodecBuilder.Mu<T>, Map<Port, Integer>, NodePos, Integer> commonFields(
                RecordCodecBuilder.Instance<T> inst
        )
        {
            return inst.group(
                    Codec.unboundedMap(Port.CODEC, Codec.INT).fieldOf("connected_wires").forGetter(node -> node.connectedWires),
                    NodePos.CODEC.fieldOf("pos").forGetter(node -> node.pos),
                    Codec.intRange(0, 3).fieldOf("rotation").forGetter(node -> node.rotation)
            );
        }
    }
}
