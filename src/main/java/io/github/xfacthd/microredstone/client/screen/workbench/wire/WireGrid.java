package io.github.xfacthd.microredstone.client.screen.workbench.wire;

import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;

public final class WireGrid implements Iterable<RoutedWire>
{
    private static final Port[] PORTS = Port.values();

    private final CircuitCanvas canvas;
    private final WireGridNode[] grid = new WireGridNode[CircuitCanvas.PART_COUNT];
    private final Set<RoutedWire> wires = new ReferenceOpenHashSet<>();

    public WireGrid(CircuitCanvas canvas)
    {
        this.canvas = canvas;
    }

    @Nullable
    public WireGridNode getWireNode(NodePos pos)
    {
        return grid[index(pos)];
    }

    public void addOrUpdateWireNode(NodePos pos, RoutedWire wire)
    {
        int idx = index(pos);
        WireGridNode wireNode = grid[idx];
        if (wireNode == null)
        {
            wireNode = grid[idx] = new WireGridNode(pos);
        }
        wireNode.wires.add(wire);
    }

    public void replaceWireInNode(NodePos pos, RoutedWire oldWire, RoutedWire newWire)
    {
        int idx = index(pos);
        WireGridNode wireNode = grid[idx];
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
        WireGridNode wireNode = grid[idx];
        if (wireNode != null)
        {
            wireNode.wires.remove(wire);
            if (wireNode.wires.isEmpty())
            {
                grid[idx] = null;
            }
        }
    }

    public void addWire(WireType wireType, DyeColor color, List<WireNode> wireNodes, List<RoutedWire.Section> wireSections)
    {
        if (wireNodes.size() < 2) return;

        PartGrid partGrid = canvas.getPartGrid();
        CompoundPrototypeNode circuit = canvas.getRootNode();

        WireGridNode startNode = getWireNode(wireNodes.getFirst().pos());
        WireGridNode endNode = getWireNode(wireNodes.getLast().pos());
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

        for (WireNode node : wireNodes)
        {
            if (node instanceof WireNode.Connection(NodePos pos, Port port, NodePos ignored))
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

    public void trimConnectedWires(NodePos pos, PlaceableNode partNode)
    {
        for (Port port : PORTS)
        {
            if (!partNode.hasPort(port, null) || !partNode.isConnected(port)) continue;

            NodePos adjPos = pos.offset(port);
            WireGridNode adjNode = getWireNode(adjPos);
            if (adjNode == null || adjNode.wires.isEmpty()) continue;

            if (adjNode.wires.size() > 1)
            {
                WireNode.Connection connection = null;
                RoutedWire conWire = null;
                for (RoutedWire wire : adjNode.wires)
                {
                    if ((connection = wire.findNode(pos, WireNode.Connection.class, con -> con.port() == port)) != null)
                    {
                        conWire = wire;
                        break;
                    }
                }
                if (conWire != null && connection.neighbor() != null)
                {
                    conWire.wire().getNodes().remove(connection);
                    WireNode.Branch branch = conWire.findNode(connection.neighbor(), WireNode.Branch.class, $ -> true);
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
            WireNode.Branch branch = wire.findNode(adjPos, WireNode.Branch.class, $ -> true);
            if (branch != null)
            {
                branch.ports().remove(port.getOpposite());
                branch.neighbors().remove(pos);
                wire.removeSection(adjPos, pos);
                continue;
            }

            WireNode.Connection connection = wire.findNode(pos, WireNode.Connection.class, con -> con.port() == port);
            if (connection != null && connection.neighbor() != null)
            {
                wire.replaceSection(connection.neighbor(), pos, adjPos);

                List<WireNode> nodes = wire.wire().getNodes();
                nodes.remove(connection);
                nodes.add(new WireNode.Branch(adjPos, Set.of(port), Set.of(connection.neighbor())));
                updateWireNodeNeighbors(nodes, pos, port, adjPos);
            }
        }
        grid[index(pos)] = null;
    }

    private static void updateWireNodeNeighbors(List<WireNode> nodes, NodePos pos, Port port, NodePos adjPos)
    {
        Port opposite = port.getOpposite();
        for (int i = 0; i < nodes.size(); i++)
        {
            WireNode node = nodes.get(i);
            if (node instanceof WireNode.Connection con && con.port() == opposite && pos.equals(con.neighbor()))
            {
                nodes.set(i, con.withNeighbor(opposite, adjPos));
            }
            else if (node instanceof WireNode.Branch branch && branch.ports().contains(opposite) && branch.neighbors().contains(pos))
            {
                branch.neighbors().remove(pos);
                branch.neighbors().add(adjPos);
            }
        }
    }

    // TODO: implement disambiguation when the node has multiple wires crossing and implement partial (i.e. single-section) deletion when fullWire is false
    public boolean removeWireAt(NodePos pos, boolean fullWire)
    {
        WireGrid.WireGridNode node = getWireNode(pos);
        if (node == null) return false;

        if (node.wires.size() != 1) return false;

        RoutedWire wire = node.wires.getFirst();
        wires.remove(wire);
        canvas.getRootNode().removeWire(wire.wire());

        for (WireGridNode gridNode : grid)
        {
            if (gridNode != null)
            {
                // Mutating while iterating works because grid is an array and the loop is secretly a for-i loop
                removeWireFromNode(gridNode.pos, wire);
            }
        }

        return true;
    }

    public void clear()
    {
        Arrays.fill(grid, null);
        wires.clear();
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

    public record WireGridNode(NodePos pos, SequencedSet<RoutedWire> wires)
    {
        WireGridNode(NodePos pos)
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
