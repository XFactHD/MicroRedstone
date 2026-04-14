package io.github.xfacthd.microredstone.client.screen.workbench.element;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public record WireRenderState(WireType type, DyeColor color, List<NodePos> nodes, List<WireSection> sections, boolean powered, Sprites sprites, int packedColor) {
    private static final int WIRE_PERP_OFFSET = 4;
    private static final int WIRE_PAR_OFFSET_PART_MIN = 9;
    private static final int WIRE_PAR_OFFSET_PART_MAX = 1;
    private static final int WIRE_PAR_OFFSET_BRANCH_MIN = 7;
    private static final int WIRE_PAR_OFFSET_BRANCH_MAX = 3;
    private static final int WIRE_WIDTH = 2;

    public WireRenderState(WireType type, DyeColor color) {
        this(type, color, new ArrayList<>(), new ArrayList<>(), false, Sprites.DEFAULT, computeColor(type, color, false));
    }

    public void addSections(Set<NodePos> parts, Collection<RoutedWire.Section> sections) {
        sections.forEach(section -> addSection(parts, section.posOne(), section.posTwo()));
    }

    public void addSection(Set<NodePos> parts, NodePos posOne, NodePos posTwo) {
        if (posOne.x() == posTwo.x()) {
            int minY = Math.min(posOne.y(), posTwo.y());
            int maxY = Math.max(posOne.y(), posTwo.y());
            boolean partMin = parts.contains(posOne.y() == minY ? posOne : posTwo);
            boolean partMax = parts.contains(posTwo.y() == maxY ? posTwo : posOne);
            int x1 = slotToPixel(posOne.x()) + WIRE_PERP_OFFSET;
            int x2 = x1 + WIRE_WIDTH;
            int y1 = slotToPixel(minY) + (partMin ? WIRE_PAR_OFFSET_PART_MIN : WIRE_PAR_OFFSET_BRANCH_MIN);
            int y2 = slotToPixel(maxY) + (partMax ? WIRE_PAR_OFFSET_PART_MAX : WIRE_PAR_OFFSET_BRANCH_MAX);
            this.sections.add(new WireSection(x1, y1, x2, y2));
        } else if (posOne.y() == posTwo.y()) {
            int minX = Math.min(posOne.x(), posTwo.x());
            int maxX = Math.max(posOne.x(), posTwo.x());
            boolean partMin = parts.contains(posOne.x() == minX ? posOne : posTwo);
            boolean partMax = parts.contains(posTwo.x() == maxX ? posTwo : posOne);
            int x1 = slotToPixel(minX) + (partMin ? WIRE_PAR_OFFSET_PART_MIN : WIRE_PAR_OFFSET_BRANCH_MIN);
            int x2 = slotToPixel(maxX) + (partMax ? WIRE_PAR_OFFSET_PART_MAX : WIRE_PAR_OFFSET_BRANCH_MAX);
            int y1 = slotToPixel(posOne.y()) + WIRE_PERP_OFFSET;
            int y2 = y1 + WIRE_WIDTH;
            this.sections.add(new WireSection(x1, y1, x2, y2));
        }
    }

    private static int slotToPixel(int coord) {
        return CircuitCanvas.BORDER_TOP_LEFT + coord * CircuitCanvas.PART_SLOT_SIZE;
    }

    public void addNode(WireNode node) {
        if (!(node instanceof WireNode.Connection)) {
            nodes.add(node.pos());
        }
    }

    public boolean isEmpty() {
        return sections.isEmpty() && nodes.isEmpty();
    }

    public WireRenderState withPowered(boolean powered) {
        if (powered == this.powered || type == WireType.BUNDLED) {
            return this;
        }
        return new WireRenderState(type, color, nodes, sections, powered, sprites, computeColor(type, color, powered));
    }

    private static int computeColor(WireType type, DyeColor color, boolean powered) {
        if (type == WireType.BUNDLED) {
            return 0xFFFFFFFF;
        }
        if (powered) {
            return color.getTextColor();
        }
        return color.getTextureDiffuseColor();
    }

    public record WireSection(int minX, int minY, int maxX, int maxY) { }

    public record Sprites(Identifier nodeSprite, Identifier sectionSpriteHor, Identifier sectionSpriteVert) {
        private static final Sprites DEFAULT = new Sprites(
                CircuitCanvasContentRenderState.WHITE_SPRITE,
                CircuitCanvasContentRenderState.WHITE_SPRITE,
                CircuitCanvasContentRenderState.WHITE_SPRITE
        );
    }
}
