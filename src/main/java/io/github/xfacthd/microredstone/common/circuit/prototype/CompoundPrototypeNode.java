package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.PortConfig;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CompoundPrototypeNode extends PrototypeNode
{
    private static final PortConfig PORT_CONFIG = PortConfig.builder().build();

    private final List<PrototypeNode> childNodes = new ArrayList<>();
    private final Set<Wire> wires = new HashSet<>();
    private final @Nullable Connection[] connections = new Connection[4];

    public CompoundPrototypeNode()
    {
        super(PORT_CONFIG, null);
    }

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

    public List<Wire> getWiresCopy()
    {
        List<Wire> wireList = new ArrayList<>(wires.size());
        for (Wire wire : wires)
        {
            wireList.add(wire.copy());
        }
        return wireList;
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

    @Override
    public void validate(ProblemReporter reporter)
    {
        WireValidator wireValidator = new WireValidator();
        for (PrototypeNode childNode : childNodes)
        {
            ProblemReporter childReporter = reporter.forChild(childNode::toString);
            childNode.validate(childReporter);

            if (!wires.containsAll(childNode.getConnectedWires()))
            {
                childReporter.report(() -> "Unknown wires");
            }

            ProblemReporter outputsReporter = childReporter.forChild(() -> "drivers");
            Set<Wire> outputWires = childNode.getConnectedWires(PortDir.OUTPUT);
            int packerBit = -1;
            if (childNode instanceof ConverterPrototypeNode converter && converter.isPacker())
            {
                packerBit = converter.getBitIndex();
            }
            for (Wire wire : childNode.getConnectedWires())
            {
                boolean driver = outputWires.contains(wire);
                wireValidator.check(wire, childNode.getOutputType(wire), driver, packerBit, outputsReporter);
            }
        }
        for (Connection connection : connections)
        {
            if (connection != null)
            {
                ProblemReporter conReporter = reporter.forChild(connection::toString);
                connection.validate(conReporter);
                boolean driver = connection.getPortDir() == PortDir.INPUT;
                wireValidator.check(connection.getWire(), connection.getWireType(), driver, -1, conReporter);
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
                int count = driverCount;
                reporter.report(() -> "Wire " + entry.getKey() + " has incorrect amount of driving outputs: " + count);
            }
        }
    }

    @Override
    public CircuitNode assemble(WireMapper wireMapper)
    {
        throw new UnsupportedOperationException();
    }

    public void clear()
    {
        childNodes.clear();
        wires.clear();
        Arrays.fill(connections, null);
    }

    private static final class WireValidator
    {
        private final Reference2IntMap<Wire> counts = new Reference2IntOpenHashMap<>();
        private final Reference2IntMap<Wire> packers = new Reference2IntOpenHashMap<>();
        private final Reference2ObjectMap<Wire, WireType> types = new Reference2ObjectOpenHashMap<>();

        public void check(Wire wire, WireType type, boolean driver, int packerBit, ProblemReporter reporter)
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
                    reporter.report(() -> "Wire " + wire + " has multiple packers driving bit " + packerBit);
                }
                packerMask |= bitMask;
                packers.put(wire, packerMask);
            }

            WireType prevType = types.put(wire, type);
            if (prevType != null && prevType != type)
            {
                reporter.report(() -> String.format(Locale.ROOT, "Wire %s has mismatched port types (prev: %s, new: %s)", wire, prevType, type));
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
