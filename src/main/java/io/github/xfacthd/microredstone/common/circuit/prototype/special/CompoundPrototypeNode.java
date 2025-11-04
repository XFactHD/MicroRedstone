package io.github.xfacthd.microredstone.common.circuit.prototype.special;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.NodeError;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.WireError;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CompoundPrototypeNode
{
    private final List<PrototypeNode> childNodes = new ArrayList<>();
    private final Set<Wire> wires = new HashSet<>();
    private final @Nullable Connection[] connections = new Connection[4];

    public void addChild(PrototypeNode node)
    {
        childNodes.add(node);
    }

    public void removeChild(PrototypeNode node)
    {
        childNodes.remove(node);
    }

    public void addWire(Wire wire)
    {
        wires.add(wire);
    }

    public void removeWire(Wire wire)
    {
        wires.remove(wire);
        for (PrototypeNode child : childNodes)
        {
            child.removeConnectedWire(wire);
        }
        for (Connection connection : connections)
        {
            if (connection != null)
            {
                connection.removeWire(wire);
            }
        }
    }

    public void setConnection(Port port, @Nullable Connection connection)
    {
        Connection oldConnection = connections[port.ordinal()];
        if (connection != null && oldConnection != null)
        {
            throw new IllegalStateException("Attempted to overwrite node for port " + port);
        }
        connections[port.ordinal()] = connection;
    }

    public List<PrototypeNode> getChildNodes()
    {
        return childNodes;
    }

    public int getWireCount()
    {
        return wires.size();
    }

    public Connection[] getConnections()
    {
        return connections;
    }

    public void replaceWireInConnections(Wire oldWire, Wire newWire)
    {
        for (Connection connection : connections)
        {
            if (connection != null && connection.getWire() == oldWire)
            {
                connection.connect(newWire);
            }
        }
    }

    public void validate(CircuitErrorCollector errors)
    {
        WireValidator wireValidator = new WireValidator();
        for (PrototypeNode childNode : childNodes)
        {
            childNode.validate(errors);

            Set<Wire> childWires = childNode.getConnectedWires();
            if (!wires.containsAll(childWires))
            {
                errors.submit(new NodeError.UnknownWires(childNode, Sets.difference(childWires, wires)));
            }

            Set<Wire> outputWires = childNode.getConnectedWires(PortDir.OUTPUT);
            int packerBit = -1;
            if (childNode instanceof ConverterPrototypeNode converter && converter.isPacker())
            {
                packerBit = converter.getBitIndex();
            }
            for (Wire wire : childWires)
            {
                boolean driver = outputWires.contains(wire);
                wireValidator.check(wire, driver, packerBit, errors);
            }
        }
        for (Connection connection : connections)
        {
            if (connection != null && connection.validate(errors))
            {
                boolean driver = connection.getPortDir() == PortDir.INPUT;
                wireValidator.check(connection.getWire(), driver, -1, errors);
            }
        }
        for (Reference2IntMap.Entry<Wire> entry : wireValidator.entries())
        {
            int driverCount = entry.getIntValue();
            if (wireValidator.getPackerMask(entry.getKey()) != 0)
            {
                driverCount++;
            }
            if (driverCount != 1)
            {
                errors.submit(new WireError.DriverCount(entry.getKey(), driverCount));
            }
        }
    }

    public void clear()
    {
        childNodes.clear();
        wires.clear();
        Arrays.fill(connections, null);
    }

    public boolean isEmpty()
    {
        return childNodes.isEmpty() && wires.isEmpty() && Arrays.stream(connections).allMatch(Objects::isNull);
    }

    public <T> DataResult<T> serialize(DynamicOps<T> ops)
    {
        List<Wire> wires = List.copyOf(this.wires);
        List<PrototypeNode.Serializable> childNodes = this.childNodes.stream().map(node -> node.serialize(wires)).toList();
        List<Connection.Serializable> connections = Arrays.stream(this.connections)
                .filter(Objects::nonNull)
                .map(con -> con.serialize(Port.ofPartRotation(con.getRotation()), wires))
                .toList();
        return CompoundPrototypeNode.Serializable.CODEC.encodeStart(ops, new Serializable(childNodes, connections, wires));
    }

    public static <T> DataResult<CompoundPrototypeNode> deserialize(DynamicOps<T> ops, T input)
    {
        return CompoundPrototypeNode.Serializable.CODEC.parse(ops, input).map(CompoundPrototypeNode.Serializable::build);
    }

    private record Serializable(List<PrototypeNode.Serializable> childNodes, List<Connection.Serializable> connections, List<Wire> wires)
    {
        private static final Codec<Serializable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                PrototypeNode.Serializable.CODEC.listOf().fieldOf("child_nodes").forGetter(CompoundPrototypeNode.Serializable::childNodes),
                Connection.Serializable.CODEC.listOf().fieldOf("connections").forGetter(CompoundPrototypeNode.Serializable::connections),
                Wire.CODEC.listOf().fieldOf("wires").forGetter(CompoundPrototypeNode.Serializable::wires)
        ).apply(inst, CompoundPrototypeNode.Serializable::new));

        private CompoundPrototypeNode build()
        {
            CompoundPrototypeNode cmpNode = new CompoundPrototypeNode();
            cmpNode.wires.addAll(wires);
            childNodes.stream().map(node -> node.build(wires)).forEach(cmpNode::addChild);
            connections.forEach(con -> cmpNode.setConnection(con.port(), con.build(wires)));
            cmpNode.childNodes.forEach(node -> node.finishDeserialization(cmpNode.childNodes));
            return cmpNode;
        }
    }

    private static final class WireValidator
    {
        private final Reference2IntMap<Wire> counts = new Reference2IntOpenHashMap<>();
        private final Reference2IntMap<Wire> packers = new Reference2IntOpenHashMap<>();

        public void check(Wire wire, boolean driver, int packerBit, CircuitErrorCollector errors)
        {
            if (!driver || packerBit == -1)
            {
                counts.computeInt(wire, ($, val) ->
                {
                    int inc = driver ? 1 : 0;
                    return val != null ? val + inc : inc;
                });
            }
            else
            {
                int bitMask = 1 << packerBit;
                int packerMask = packers.getInt(wire);
                if ((packerMask & bitMask) != 0)
                {
                    errors.submit(new WireError.MultipleBundlePackers(wire, packerBit));
                }
                packerMask |= bitMask;
                packers.put(wire, packerMask);
            }
        }

        public ObjectSet<Reference2IntMap.Entry<Wire>> entries()
        {
            return counts.reference2IntEntrySet();
        }

        public int getPackerMask(Wire wire)
        {
            return packers.getInt(wire);
        }
    }
}
