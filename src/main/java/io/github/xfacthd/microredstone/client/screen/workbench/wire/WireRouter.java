package io.github.xfacthd.microredstone.client.screen.workbench.wire;

import com.google.common.collect.Sets;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.Set;

final class WireRouter
{
    static List<Wire.Node> route(CircuitCanvas canvas, WireInProgress wip, NodePos cursorPos)
    {
        List<Wire.Node> wireNodes = wip.getWireNodes();
        if (!wireNodes.isEmpty() && cursorPos.equals(wireNodes.getLast().pos()))
        {
            return List.of();
        }

        return route(canvas, wip, wireNodes.getLast(), cursorPos, wip.getType());
    }

    private static List<Wire.Node> route(CircuitCanvas canvas, WireInProgress wip, Wire.Node startNode, NodePos endPos, WireType wireType)
    {
        Set<Port> dirsToEnd = startNode.pos().getDirsTowards(endPos);
        if (dirsToEnd.isEmpty())
        {
            return List.of();
        }

        return switch (startNode)
        {
            case Wire.Node.Dangling ignored ->
            {
                SequencedSet<Port> sectionDirs = sortDirsByLength(startNode.pos(), endPos, dirsToEnd);
                yield routeWithReverse(canvas, wip, sectionDirs, startNode, endPos, wireType);
            }
            case Wire.Node.Branch branch ->
            {
                Set<Port> openDirs = Sets.difference(dirsToEnd, branch.ports());
                int size = openDirs.size();
                if (size == 0)
                {
                    yield List.of();
                }
                if (size >= dirsToEnd.size())
                {
                    SequencedSet<Port> sectionDirs = sortDirsByLength(startNode.pos(), endPos, dirsToEnd);
                    yield routeWithReverse(canvas, wip, sectionDirs, startNode, endPos, wireType);
                }

                SequencedSet<Port> sectionDirs = new LinkedHashSet<>();
                sectionDirs.add(openDirs.iterator().next());
                sectionDirs.addAll(dirsToEnd);
                yield route(canvas, wip, sectionDirs, startNode, endPos, wireType);
            }
            case Wire.Node.Connection connection ->
            {
                if (!dirsToEnd.contains(connection.port()))
                {
                    yield List.of();
                }

                SequencedSet<Port> sectionDirs = new LinkedHashSet<>();
                sectionDirs.add(connection.port());
                sectionDirs.addAll(dirsToEnd);
                yield route(canvas, wip, sectionDirs, startNode, endPos, wireType);
            }
        };
    }

    // TODO: add option to invert preference
    private static SequencedSet<Port> sortDirsByLength(NodePos startPos, NodePos endPos, Set<Port> dirsToEnd)
    {
        NodePos diff = endPos.subtract(startPos);
        List<Port> sortedDirs = new ArrayList<>(dirsToEnd);
        sortedDirs.removeIf(dir -> dir.getLengthAlong(diff) == 0);
        sortedDirs.sort(Comparator.<Port>comparingInt(dir -> dir.getLengthAlong(diff)).reversed());
        return new LinkedHashSet<>(sortedDirs);
    }

    private static List<Wire.Node> routeWithReverse(
            CircuitCanvas canvas,
            WireInProgress wip,
            SequencedSet<Port> sectionDirs,
            Wire.Node startNode,
            NodePos endPos,
            WireType wireType
    )
    {
        List<Wire.Node> nodes = route(canvas, wip, sectionDirs, startNode, endPos, wireType);
        if (nodes.size() == sectionDirs.size() && nodes.getLast().pos().equals(endPos))
        {
            return nodes;
        }
        return route(canvas, wip, sectionDirs.reversed(), startNode, endPos, wireType);
    }

    private static List<Wire.Node> route(
            CircuitCanvas canvas,
            WireInProgress wip,
            SequencedSet<Port> sectionDirs,
            Wire.Node startNode,
            NodePos endPos,
            WireType wireType
    )
    {
        NodePos startPos = startNode.pos();
        Port firstDir = sectionDirs.getFirst();
        if (sectionDirs.size() == 1)
        {
            Wire.Node wireNode = computeNode(canvas, wip, startPos, endPos, firstDir, wireType, true);
            return wireNode != null ? List.of(wireNode) : List.of();
        }

        NodePos endPosOne = new NodePos(firstDir.select(endPos, startPos).x(), firstDir.select(startPos, endPos).y());
        Wire.Node wireOne = computeNode(canvas, wip, startPos, endPosOne, firstDir, wireType, false);
        if (wireOne == null)
        {
            return List.of();
        }
        if (!wireOne.pos().equals(endPosOne))
        {
            endPos = new NodePos(firstDir.select(wireOne.pos(), endPos).x(), firstDir.select(endPos, wireOne.pos()).y());
        }

        List<Wire.Node> nodes = new ArrayList<>(2);
        nodes.add(wireOne);
        Port lastDir = sectionDirs.getLast();
        NodePos endPosTwo = new NodePos(lastDir.select(endPos, wireOne.pos()).x(), lastDir.select(wireOne.pos(), endPos).y());
        nodes.addAll(route(canvas, wip, wireOne, endPosTwo, wireType));
        return nodes;
    }

    @Nullable
    private static Wire.Node computeNode(
            CircuitCanvas canvas,
            WireInProgress wip,
            NodePos startPos,
            NodePos endPos,
            Port dir,
            WireType type,
            boolean last
    )
    {
        if (wip.intersects(endPos))
        {
            return computeBacktrackedNode(canvas, startPos, endPos, dir);
        }
        PartGrid partGrid = canvas.getPartGrid();
        NodePos checkPos = startPos;
        while (!(checkPos = checkPos.offset(dir)).equals(endPos))
        {
            if (partGrid.getPartNode(checkPos) != null)
            {
                endPos = checkPos;
                return computeBacktrackedNode(canvas, startPos, endPos, dir);
            }
        }
        PlaceableNode partNode = partGrid.getPartNode(endPos);
        if (partNode != null)
        {
            Port partPort = dir.getOpposite();
            if (last && partNode.hasPort(partPort, type) && !partNode.isConnected(partPort))
            {
                return new Wire.Node.Connection(endPos, partPort, startPos);
            }
            return computeBacktrackedNode(canvas, startPos, endPos, dir);
        }
        WireGrid.WireNode wireNode = canvas.getWireGrid().getWireNode(endPos);
        if (wireNode == null || (last && wireNode.canConnect(type, dir)))
        {
            return new Wire.Node.Branch(endPos, Set.of(dir.getOpposite()), Set.of(startPos));
        }
        return computeBacktrackedNode(canvas, startPos, endPos, dir);
    }

    @Nullable
    private static Wire.Node computeBacktrackedNode(CircuitCanvas canvas, NodePos startPos, NodePos endPos, Port dir)
    {
        Port revDir = dir.getOpposite();
        while (!(endPos = endPos.offset(revDir)).equals(startPos))
        {
            if (!canvas.isNodeOccupied(endPos))
            {
                return new Wire.Node.Branch(endPos, Set.of(revDir), Set.of(startPos));
            }
        }
        return null;
    }

    private WireRouter() { }
}
