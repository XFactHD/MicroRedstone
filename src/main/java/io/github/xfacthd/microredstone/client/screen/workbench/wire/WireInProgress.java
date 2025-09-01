package io.github.xfacthd.microredstone.client.screen.workbench.wire;

import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public final class WireInProgress
{
    private static final long DOUBLE_CLICK_DELAY = 250;

    private final WireType type;
    private final DyeColor color;
    private final WireType.Icon icon;
    private final List<WireNode> wireNodes = new ArrayList<>();
    private final List<RoutedWire.Section> sections = new ArrayList<>();
    @Nullable
    private NodePos lastCursorPos = null;
    private List<WireNode> floatingNodes = List.of();
    private long lastClick = -1;

    public WireInProgress(WireType wireType, @Nullable DyeColor color)
    {
        this.type = wireType;
        this.color = Objects.requireNonNullElse(color, DyeColor.RED);
        this.icon = wireType.getIcon().withColor(color);
    }

    public WireType getType()
    {
        return type;
    }

    public DyeColor getColor()
    {
        return color;
    }

    public WireType.Icon getIcon()
    {
        return icon;
    }

    public List<WireNode> getWireNodes()
    {
        return wireNodes;
    }

    public List<RoutedWire.Section> getSections()
    {
        return sections;
    }

    public List<WireNode> getFloatingNodes()
    {
        return floatingNodes;
    }

    public void repath(CircuitCanvas canvas, int mouseX, int mouseY)
    {
        if (wireNodes.isEmpty()) { return; }

        NodePos cursorPos = canvas.getNodePos(mouseX, mouseY);
        if (cursorPos != null && !cursorPos.equals(lastCursorPos))
        {
            floatingNodes = new ArrayList<>(WireRouter.route(canvas, this, cursorPos));
            lastCursorPos = cursorPos;
        }
    }

    public PlaceResult placeNextNode(CircuitCanvas canvas, double mouseX, double mouseY)
    {
        boolean doubleClick = System.currentTimeMillis() - lastClick < DOUBLE_CLICK_DELAY;
        lastClick = System.currentTimeMillis();

        if (wireNodes.isEmpty())
        {
            ExactNodePos exactPos = canvas.getExactNodePos(mouseX, mouseY);
            if (exactPos == null)
            {
                return PlaceResult.OUTSIDE_CANVAS;
            }

            NodePos pos = exactPos.pos();
            PlaceableNode partNode = canvas.getPartGrid().getPartNode(pos);
            if (partNode != null)
            {
                return tryConnectPart(pos, partNode, true, null, () -> Port.ofCross(exactPos.fracX(), exactPos.fracY()));
            }
            WireGrid.WireNode wire = canvas.getWireGrid().getWireNode(pos);
            if (wire != null && wire.wires().size() == 1)
            {
                RoutedWire routed = wire.wires().getFirst();
                RoutedWire.Section section = routed.findIntersectedSection(pos);
                Set<NodePos> neighbors = section != null ? Set.of(section.posOne(), section.posTwo()) : Set.of();
                wireNodes.add(new WireNode.Branch(pos, routed.getBlockedDirsAt(pos), neighbors));
                return PlaceResult.SUCCESS;
            }
            wireNodes.add(new WireNode.Dangling(pos));
            return PlaceResult.SUCCESS;
        }

        repath(canvas, (int) mouseX, (int) mouseY);
        NodePos pos = canvas.getNodePos((int) mouseX, (int) mouseY);
        if (!floatingNodes.isEmpty())
        {
            if (floatingNodes.getLast().pos().equals(pos))
            {
                // floatingNodes can only contain zero, one or two entries
                WireNode lastNode = floatingNodes.size() > 1 ? floatingNodes.getFirst() : wireNodes.getLast();
                Port dir = lastNode.pos().getDirTowards(pos);

                PlaceableNode partNode = canvas.getPartGrid().getPartNode(pos);
                if (partNode != null)
                {
                    floatingNodes.removeLast();
                    PlaceResult result = tryConnectPart(pos, partNode, false, lastNode.pos(), dir::getOpposite);
                    if (result == PlaceResult.SUCCESS)
                    {
                        addMissingSectionsAndComplete(canvas);
                    }
                    return result;
                }
                WireGrid.WireNode wireNode = canvas.getWireGrid().getWireNode(pos);
                if (wireNode != null)
                {
                    if (wireNode.canConnect(type, dir))
                    {
                        wireNodes.add(floatingNodes.removeLast());
                        addMissingSectionsAndComplete(canvas);
                        return PlaceResult.SUCCESS;
                    }
                    return PlaceResult.GENERIC_FAIL;
                }

                WireNode node = floatingNodes.getFirst();
                sections.add(new RoutedWire.Section(wireNodes.getLast().pos(), node.pos()));
                updateNodeAt(wireNodes.size() - 1, node);
                wireNodes.add(node);
                lastCursorPos = null;
                return PlaceResult.SUCCESS;
            }
        }
        else if (doubleClick && wireNodes.getLast().pos().equals(pos))
        {
            completeWire(canvas);
            return PlaceResult.SUCCESS;
        }
        return PlaceResult.GENERIC_FAIL;
    }

    private PlaceResult tryConnectPart(
            NodePos pos,
            PlaceableNode part,
            boolean updateNode,
            @Nullable NodePos neighbor,
            Supplier<Port> portSupplier
    )
    {
        Port port = portSupplier.get();
        if (!part.hasPort(port, type)) return PlaceResult.NO_PORT;
        if (part.isConnected(port)) return PlaceResult.BLOCKED_PORT;

        WireNode.Connection node = new WireNode.Connection(pos, port, neighbor);
        if (updateNode && !wireNodes.isEmpty())
        {
            updateNodeAt(wireNodes.size() - 1, node);
        }
        wireNodes.add(node);
        return PlaceResult.SUCCESS;
    }

    private void addMissingSectionsAndComplete(CircuitCanvas canvas)
    {
        int firstNewNode = wireNodes.size() - 1;
        if (firstNewNode > 0)
        {
            WireNode connected = floatingNodes.isEmpty() ? wireNodes.getLast() : floatingNodes.getFirst();
            updateNodeAt(firstNewNode - 1, connected);
        }
        wireNodes.addAll(wireNodes.size() - 1, floatingNodes);
        for (int i = Math.max(firstNewNode, 1); i < wireNodes.size(); i++)
        {
            sections.add(new RoutedWire.Section(wireNodes.get(i - 1).pos(), wireNodes.get(i).pos()));
        }
        completeWire(canvas);
    }

    private void updateNodeAt(int index, WireNode connected)
    {
        WireNode node = wireNodes.get(index);
        Port dir = node.pos().getDirTowards(connected.pos());
        wireNodes.set(index, node.withNeighbor(dir, connected.pos()));
    }

    private void completeWire(CircuitCanvas canvas)
    {
        canvas.getWireGrid().addWire(type, color, wireNodes, sections);
        canvas.cancelWirePull();
    }

    public boolean intersects(NodePos pos)
    {
        for (RoutedWire.Section section : sections)
        {
            if (section.intersects(pos))
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Port getTargettedPort(ExactNodePos exactPos)
    {
        if (wireNodes.isEmpty())
        {
            return Port.ofCross(exactPos.fracX(), exactPos.fracY());
        }
        if (!floatingNodes.isEmpty())
        {
            if (!floatingNodes.getLast().pos().equals(exactPos.pos())) return null;

            WireNode lastNode = floatingNodes.size() > 1 ? floatingNodes.getFirst() : wireNodes.getLast();
            return lastNode.pos().getDirTowards(exactPos.pos()).getOpposite();
        }
        return null;
    }

    public boolean isEmpty()
    {
        return wireNodes.isEmpty() && floatingNodes.isEmpty();
    }

    public enum PlaceResult
    {
        SUCCESS,
        OUTSIDE_CANVAS,
        NO_PORT,
        BLOCKED_PORT,
        GENERIC_FAIL
    }
}
