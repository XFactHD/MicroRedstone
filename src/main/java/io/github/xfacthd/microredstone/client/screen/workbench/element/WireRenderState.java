package io.github.xfacthd.microredstone.client.screen.workbench.element;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public record WireRenderState(WireType type, DyeColor color, Set<NodePos> nodes, List<WireSection> sections, boolean powered, Sprites sprites, int packedColor)
{
    private static final int WIRE_PERP_OFFSET = 4;
    private static final int WIRE_PAR_OFFSET_PART_MIN = 9;
    private static final int WIRE_PAR_OFFSET_PART_MAX = 1;
    private static final int WIRE_PAR_OFFSET_BRANCH_MIN = 7;
    private static final int WIRE_PAR_OFFSET_BRANCH_MAX = 3;
    private static final int WIRE_WIDTH = 2;

    public WireRenderState(WireType type, DyeColor color)
    {
        this(type, color, new ObjectLinkedOpenHashSet<>(), new ArrayList<>(), false, Sprites.DEFAULT, computeColor(type, color, false));
    }

    public void addSections(Collection<RoutedWire.Section> sections)
    {
        for (RoutedWire.Section section : sections)
        {
            NodePos posOne = section.posOne();
            NodePos posTwo = section.posTwo();
            if (posOne.x() == posTwo.x())
            {
                int minY = Math.min(posOne.y(), posTwo.y());
                int maxY = Math.max(posOne.y(), posTwo.y());
                boolean branchMin = nodes.contains(posOne.y() == minY ? posOne : posTwo);
                boolean branchMax = nodes.contains(posTwo.y() == maxY ? posTwo : posOne);
                int x1 = slotToPixel(posOne.x()) + WIRE_PERP_OFFSET;
                int x2 = x1 + WIRE_WIDTH;
                int y1 = slotToPixel(minY) + (branchMin ? WIRE_PAR_OFFSET_BRANCH_MIN : WIRE_PAR_OFFSET_PART_MIN);
                int y2 = slotToPixel(maxY) + (branchMax ? WIRE_PAR_OFFSET_BRANCH_MAX : WIRE_PAR_OFFSET_PART_MAX);
                this.sections.add(new WireSection(x1, y1, x2, y2));
            }
            else if (posOne.y() == posTwo.y())
            {
                int minX = Math.min(posOne.x(), posTwo.x());
                int maxX = Math.max(posOne.x(), posTwo.x());
                boolean branchMin = nodes.contains(posOne.x() == minX ? posOne : posTwo);
                boolean branchMax = nodes.contains(posTwo.x() == maxX ? posTwo : posOne);
                int x1 = slotToPixel(minX) + (branchMin ? WIRE_PAR_OFFSET_BRANCH_MIN : WIRE_PAR_OFFSET_PART_MIN);
                int x2 = slotToPixel(maxX) + (branchMax ? WIRE_PAR_OFFSET_BRANCH_MAX : WIRE_PAR_OFFSET_PART_MAX);
                int y1 = slotToPixel(posOne.y()) + WIRE_PERP_OFFSET;
                int y2 = y1 + WIRE_WIDTH;
                this.sections.add(new WireSection(x1, y1, x2, y2));
            }
        }
    }

    private static int slotToPixel(int coord)
    {
        return CircuitCanvas.BORDER_TOP_LEFT + coord * CircuitCanvas.PART_SLOT_SIZE;
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

    public WireRenderState withPowered(boolean powered)
    {
        if (powered == this.powered || type == WireType.BUNDLED) return this;
        return new WireRenderState(type, color, nodes, sections, powered, sprites, computeColor(type, color, powered));
    }

    private static int computeColor(WireType type, DyeColor color, boolean powered)
    {
        if (type == WireType.BUNDLED) return 0xFFFFFFFF;
        if (powered) return color.getTextColor();
        return color.getTextureDiffuseColor();
    }

    public record WireSection(int minX, int minY, int maxX, int maxY) {}

    public record Sprites(ResourceLocation nodeSprite, ResourceLocation sectionSpriteHor, ResourceLocation sectionSpriteVert)
    {
        private static final Sprites DEFAULT = new Sprites(
                CircuitCanvasContentRenderState.WHITE_SPRITE,
                CircuitCanvasContentRenderState.WHITE_SPRITE,
                CircuitCanvasContentRenderState.WHITE_SPRITE
        );
    }
}
