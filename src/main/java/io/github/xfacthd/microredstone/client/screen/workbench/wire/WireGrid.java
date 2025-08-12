package io.github.xfacthd.microredstone.client.screen.workbench.wire;

import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;

public final class WireGrid implements Iterable<RoutedWire>
{
    private final CircuitCanvas canvas;
    private final WireNode[] grid = new WireNode[CircuitCanvas.PART_COUNT];
    private final Set<RoutedWire> wires = new ReferenceOpenHashSet<>();

    public WireGrid(CircuitCanvas canvas)
    {
        this.canvas = canvas;
    }

    @Nullable
    public WireNode getWireNode(NodePos pos)
    {
        return grid[index(pos)];
    }

    public void addOrUpdateWireNode(NodePos pos, RoutedWire wire)
    {
        int idx = index(pos);
        WireNode wireNode = grid[idx];
        if (wireNode == null)
        {
            wireNode = grid[idx] = new WireNode(pos);
        }
        wireNode.wires.add(wire);
    }

    public void replaceWireInNode(NodePos pos, RoutedWire oldWire, RoutedWire newWire)
    {
        int idx = index(pos);
        WireNode wireNode = grid[idx];
        if (wireNode == null)
        {
            addOrUpdateWireNode(pos, newWire);
            return;
        }

        wireNode.wires.remove(oldWire);
        wireNode.wires.add(newWire);
    }

    public void removeWireFromNode(NodePos pos, RoutedWire wire)
    {
        int idx = index(pos);
        WireNode wireNode = grid[idx];
        if (wireNode != null)
        {
            wireNode.wires.remove(wire);
            if (wireNode.wires.isEmpty())
            {
                grid[idx] = null;
            }
        }
    }

    public void addWire(WireType wireType, DyeColor color, List<io.github.xfacthd.microredstone.common.circuit.connection.WireNode> wireNodes, List<RoutedWire.Section> wireSections)
    {
        if (wireNodes.size() < 2) return;

        PartGrid partGrid = canvas.getPartGrid();
        CompoundPrototypeNode circuit = canvas.getRootNode();

        WireNode startNode = getWireNode(wireNodes.getFirst().pos());
        WireNode endNode = getWireNode(wireNodes.getLast().pos());
        RoutedWire routedWire;
        if (startNode != null)
        {
            routedWire = startNode.wires.getFirst();
            if (endNode != null)
            {
                RoutedWire oldWire = endNode.wires.getFirst();
                oldWire.forAllNodes(pos -> replaceWireInNode(pos, oldWire, routedWire));
                partGrid.forEach(part -> part.replaceWire(oldWire.wire(), routedWire.wire()));
                circuit.replaceWireInConnections(oldWire.wire(), routedWire.wire());
                routedWire.sections().addAll(oldWire.sections());
                routedWire.wire().getNodes().addAll(oldWire.wire().getNodes());
            }
        }
        else if (endNode != null)
        {
            routedWire = endNode.wires.getFirst();
        }
        else
        {
            routedWire = new RoutedWire(new Wire(wireType, color), wireSections);
            circuit.addWire(routedWire.wire());
            wires.add(routedWire);
        }

        routedWire.wire().addNodes(wireNodes);
        if (startNode != null || endNode != null)
        {
            routedWire.sections().addAll(wireSections);
        }
        routedWire.fixNodes();

        for (io.github.xfacthd.microredstone.common.circuit.connection.WireNode node : wireNodes)
        {
            if (node instanceof io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Connection(NodePos pos, Port port, NodePos ignored))
            {
                PlaceableNode partNode = partGrid.getPartNode(pos);
                switch (partNode)
                {
                    case PrototypeNode part -> part.setConnection(port, routedWire.wire(), true);
                    case Connection con -> con.connect(routedWire.wire());
                    case null -> throw new NullPointerException("Tried connecting to non-existent node at " + pos);
                    default -> throw new IllegalArgumentException("Invalid node: " + partNode);
                }
            }
            else
            {
                addOrUpdateWireNode(node.pos(), routedWire);
            }
        }
        for (RoutedWire.Section section : wireSections)
        {
            section.forNonVertexNodes(pos -> addOrUpdateWireNode(pos, routedWire));
        }
    }

    public void trimConnectedWires(NodePos pos, PrototypeNode partNode)
    {
        for (Port port : Port.values())
        {
            if (!partNode.hasPort(port, null) || !partNode.isConnected(port)) continue;

            NodePos adjPos = pos.offset(port);
            WireNode adjNode = getWireNode(adjPos);
            if (adjNode == null || adjNode.wires.isEmpty()) continue;

            if (adjNode.wires.size() > 1)
            {
                io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Connection connection = null;
                RoutedWire conWire = null;
                for (RoutedWire wire : adjNode.wires)
                {
                    if ((connection = wire.findNode(pos, io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Connection.class, con -> con.port() == port)) != null)
                    {
                        conWire = wire;
                        break;
                    }
                }
                if (conWire != null && connection.neighbor() != null)
                {
                    conWire.wire().getNodes().remove(connection);
                    io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Branch branch = conWire.findNode(connection.neighbor(), io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Branch.class, $ -> true);
                    if (branch != null)
                    {
                        branch.ports().remove(port.getOpposite());
                        branch.neighbors().remove(connection.pos());
                    }
                    RoutedWire.Section section = conWire.removeSection(connection.neighbor(), pos);
                    if (section != null)
                    {
                        RoutedWire finalConWire = conWire;
                        section.forNonVertexNodes(nodePos -> removeWireFromNode(nodePos, finalConWire));
                    }
                }
                continue;
            }

            RoutedWire wire = adjNode.wires.getFirst();
            io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Branch branch = wire.findNode(adjPos, io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Branch.class, $ -> true);
            if (branch != null)
            {
                branch.ports().remove(port.getOpposite());
                branch.neighbors().remove(pos);
                wire.removeSection(adjPos, pos);
                continue;
            }

            io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Connection connection = wire.findNode(pos, io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Connection.class, con -> con.port() == port);
            if (connection != null && connection.neighbor() != null)
            {
                wire.replaceSection(connection.neighbor(), pos, adjPos);
                wire.wire().getNodes().remove(connection);
                wire.wire().getNodes().add(new io.github.xfacthd.microredstone.common.circuit.connection.WireNode.Branch(adjPos, Set.of(port), Set.of(connection.neighbor())));
            }
        }
        grid[index(pos)] = null;
    }

    @Override
    public Iterator<RoutedWire> iterator()
    {
        return wires.iterator();
    }

    private static int index(NodePos pos)
    {
        return pos.y() * CircuitCanvas.PART_COUNT_X + pos.x();
    }

    public record WireNode(NodePos pos, SequencedSet<RoutedWire> wires)
    {
        WireNode(NodePos pos)
        {
            this(pos, new ReferenceLinkedOpenHashSet<>());
        }

        boolean canConnect(WireType type, Port dir)
        {
            if (wires.size() != 1) return false;

            RoutedWire wire = wires.getFirst();
            return wire.wire().getWireType() == type && !wire.getBlockedDirsAt(pos).contains(dir.getOpposite());
        }
    }
}
