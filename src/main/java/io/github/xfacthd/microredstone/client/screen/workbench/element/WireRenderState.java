package io.github.xfacthd.microredstone.client.screen.workbench.element;

import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import net.minecraft.world.item.DyeColor;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public record WireRenderState(List<RoutedWire.Section> sections, List<NodePos> nodes, int packedColor)
{
    public WireRenderState(WireType type, DyeColor color, List<RoutedWire.Section> sections, List<NodePos> nodes, boolean powered)
    {
        this(sections, nodes, computeColor(type, color, powered));
    }

    public WireRenderState(WireType type, DyeColor color, List<RoutedWire.Section> sections, List<NodePos> nodes)
    {
        this(type, color, sections, nodes, false);
    }

    public void addSection(NodePos posOne, NodePos posTwo)
    {
        sections.add(new RoutedWire.Section(posOne, posTwo));
    }

    public void addNode(WireNode node)
    {
        if (node instanceof WireNode.Connection) { return; }

        if (node instanceof WireNode.Branch(NodePos pos, Set<Port> ignored, Set<NodePos> neighbors))
        {
            if (neighbors.size() == 2)
            {
                Iterator<NodePos> it = neighbors.iterator();
                NodePos adjOne = it.next();
                NodePos adjTwo = it.next();
                if ((pos.x() == adjOne.x() && adjOne.x() == adjTwo.x()) || (pos.y() == adjOne.y() && adjOne.y() == adjTwo.y()))
                {
                    // Hide unnecessary branch nodes on straight pieces
                    return;
                }
            }
        }
        nodes.add(node.pos());
    }

    public boolean isEmpty()
    {
        return sections.isEmpty() && nodes.isEmpty();
    }

    private static int computeColor(WireType type, DyeColor color, boolean powered)
    {
        if (type == WireType.BUNDLED) return 0xFFFFFFFF;
        if (powered) return color.getTextColor();
        return color.getTextureDiffuseColor();
    }
}
