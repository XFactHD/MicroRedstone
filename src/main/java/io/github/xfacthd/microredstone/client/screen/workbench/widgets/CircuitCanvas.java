package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.element.LampRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ClockPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConnectionNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConstantPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConverterPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.LampPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.PartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireInProgress;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.CircuitCanvasAccess;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

public final class CircuitCanvas extends AbstractCircuitCanvas implements CircuitCanvasAccess
{
    private static final FontDescription MONOSPACE_FONT = new FontDescription.Resource(Utils.rl("monospace"));
    private static final Identifier[] PORT_BORDERS = Util.make(new Identifier[8], arr ->
    {
        for (Port port : Port.values())
        {
            String prefix = "port/border_" + port.getSerializedName() + "_";
            arr[port.ordinal() << 1 | WireType.SINGLE.ordinal()] = Utils.rl(prefix + "single");
            arr[port.ordinal() << 1 | WireType.BUNDLED.ordinal()] = Utils.rl(prefix + "bundled");
        }
    });

    public static final String CELL_COORD_TRANSLATION = Utils.translationKey("label", "circuit_workbench.cell_coord");

    private static final int PADDING = 5;
    public static final int CELL_COORD_TEXT_HEIGHT = 10;
    private static final int CELL_COORD_TEXT_OFF_Y = CELL_COORD_TEXT_HEIGHT + PADDING;

    private final CircuitWorkbenchScreen owner;
    private final CompoundPrototypeNode circuit = new CompoundPrototypeNode();
    private final PartGrid partGrid = new PartGrid(this);
    private final WireGrid wireGrid = new WireGrid(this);
    private final ErrorAnnotations errorAnnotations = new ErrorAnnotations(this);
    @Nullable
    private WireInProgress wireInProgress;

    public CircuitCanvas(CircuitWorkbenchScreen owner)
    {
        this.owner = owner;
    }

    @Override
    protected void collectCanvasContent(
            int canvasX,
            int canvasY,
            List<PartRenderState> parts,
            List<WireRenderState> wires,
            List<LampRenderState> lamps,
            int mouseX,
            int mouseY
    )
    {
        Set<NodePos> partPositions = new HashSet<>();
        partGrid.forEach(node ->
        {
            if (!owner.isNodeFloating(node))
            {
                if (node instanceof LampPrototypeNode lamp)
                {
                    LampPrototypeNode chainedLamp = lamp.getNodeChainedTo();
                    boolean chained = chainedLamp != null && !owner.isNodeFloating(chainedLamp);
                    lamps.add(new LampRenderState(lamp, chained));
                }
                else
                {
                    parts.add(new PartRenderState(node));
                }
                partPositions.add(node.getPos());
            }
        });
        for (Connection connection : circuit.getConnections())
        {
            if (connection != null && !owner.isNodeFloating(connection))
            {
                parts.add(new PartRenderState(connection));
                partPositions.add(connection.getPos());
            }
        }

        for (RoutedWire wire : wireGrid)
        {
            WireType type = wire.wire().getWireType();
            DyeColor color = wire.wire().getColor();

            WireRenderState renderState = new WireRenderState(type, color);
            for (WireNode node : wire.wire().getNodes())
            {
                renderState.addNode(node);
            }
            renderState.addSections(partPositions, wire.sections());
            wires.add(renderState);
        }

        if (wireInProgress != null)
        {
            wireInProgress.repath(this, mouseX, mouseY);

            WireType type = wireInProgress.getType();
            DyeColor color = wireInProgress.getColor();

            WireRenderState renderState = new WireRenderState(type, color);
            List<WireNode> wireNodes = wireInProgress.getWireNodes();
            if (!wireNodes.isEmpty())
            {
                for (WireNode node : wireNodes)
                {
                    renderState.addNode(node);
                }
            }
            renderState.addSections(partPositions, wireInProgress.getSections());
            List<WireNode> floatingNodes = wireInProgress.getFloatingNodes();
            if (!floatingNodes.isEmpty())
            {
                // If a floating node is present, then at least one pinned node exists
                NodePos lastPos = wireNodes.getLast().pos();
                for (WireNode node : floatingNodes)
                {
                    renderState.addSection(partPositions, lastPos, node.pos());
                    renderState.addNode(node);
                    lastPos = node.pos();
                }
            }

            if (!renderState.isEmpty())
            {
                wires.add(renderState);
            }
        }
    }

    @Override
    protected void renderCanvasOverlays(GuiGraphics graphics, int canvasX, int canvasY, int mouseX, int mouseY)
    {
        FloatingNode floatingNode = owner.getFloatingNode();
        NodePos hovered = getNodePlacementPos(mouseX, mouseY, floatingNode);
        if (floatingNode != null)
        {
            NodePos pos = floatingNode.lastPos();
            if (pos != null)
            {
                int iconX = canvasX + BORDER_TOP_LEFT + pos.x() * PART_SLOT_SIZE;
                int iconY = canvasY + BORDER_TOP_LEFT + pos.y() * PART_SLOT_SIZE;
                drawNodeFrame(graphics, iconX, iconY, 0xFFFFFF00);
            }

            boolean canPlace = hovered != null && floatingNode.canPlaceAt(this, hovered);
            if (hovered != null && (!canPlace || !hovered.equals(pos) || !hovered.equals(getNodePos(mouseX, mouseY))))
            {
                int targetX = canvasX + BORDER_TOP_LEFT + hovered.x() * PART_SLOT_SIZE;
                int targetY = canvasY + BORDER_TOP_LEFT + hovered.y() * PART_SLOT_SIZE;
                int color = canPlace ? 0xFF00FF00 : 0xFFFF0000;
                drawNodeFrame(graphics, targetX, targetY, color);
            }
        }

        PlaceableNode node;
        if (isPullingWire() && hovered != null && (node = partGrid.getPartNode(hovered)) != null)
        {
            // If the coarse pos is non-null, then this is as well
            ExactNodePos exactPos = Objects.requireNonNull(getExactNodePos(mouseX, mouseY));
            Port port = wireInProgress.getTargettedPort(exactPos);
            WireType type = wireInProgress.getType();
            if (port != null && canConnectToPart(node, hovered, port, type))
            {
                Identifier icon = PORT_BORDERS[port.ordinal() << 1 | type.ordinal()];
                int iconX = canvasX + BORDER_TOP_LEFT + hovered.x() * PART_SLOT_SIZE;
                int iconY = canvasY + BORDER_TOP_LEFT + hovered.y() * PART_SLOT_SIZE;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, PART_SIZE + 2, PART_SIZE + 2);
            }
        }
    }

    @Override
    protected void renderAdditionalContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        NodePos hovered = getNodePos(mouseX, mouseY);
        Component cellText = Component.translatable(
                CELL_COORD_TRANSLATION,
                formatCellCoord(hovered, NodePos::x),
                formatCellCoord(hovered, NodePos::y)
        ).withStyle(style -> style.withFont(MONOSPACE_FONT));
        int y = owner.getGuiTop() + owner.getYSize() - CELL_COORD_TEXT_OFF_Y;
        graphics.drawString(owner.getFont(), cellText, x, y, 0xFF404040, false);
    }

    private static String formatCellCoord(@Nullable NodePos pos, ToIntFunction<NodePos> coordGetter)
    {
        return pos != null ? String.format(Locale.ROOT, "%2d", coordGetter.applyAsInt(pos)) : " -";
    }

    private static void drawNodeFrame(GuiGraphics graphics, int x, int y, int color)
    {
        // Can't use GuiGraphics#submitOutline() as it renders too late
        int width = PART_SLOT_SIZE + 1;
        int height = PART_SLOT_SIZE + 1;
        graphics.fill(x,             y,              x + width, y + 1,          color);
        graphics.fill(x,             y + height - 1, x + width, y + height,     color);
        graphics.fill(x,             y + 1,          x + 1,     y + height - 1, color);
        graphics.fill(x + width - 1, y + 1,          x + width, y + height - 1, color);
    }

    public static void drawPartNode(GuiGraphics graphics, IconConfig icon, int x, int y, int rotation, int partSize)
    {
        drawPartNode(graphics.pose(), icon, x, y, rotation, partSize, PartBlitter.of(graphics));
    }

    public static void drawPartNode(Matrix3x2fStack pose, IconConfig icon, int x, int y, int rotation, int partSize, PartBlitter blitter)
    {
        if (!icon.rotateTexture())
        {
            blitter.blit(pose, icon.icon(), x, y, partSize);
        }
        if (rotation != 0)
        {
            pose.pushMatrix();
            pose.translate(x, y);
            pose.rotateAbout((float) Math.toRadians(90 * rotation), partSize / 2F, partSize / 2F);
            x = 0;
            y = 0;
        }
        if (icon.rotateTexture())
        {
            blitter.blit(pose, icon.icon(), x, y, partSize);
        }
        if (icon.portOverlay() != null)
        {
            blitter.blit(pose, icon.portOverlay(), x, y, partSize);
        }
        if (rotation != 0)
        {
            pose.popMatrix();
        }
    }

    @Nullable
    public NodePos getNodePlacementPos(int mouseX, int mouseY, @Nullable FloatingNode floatingNode)
    {
        NodePos pos = getNodePos(mouseX, mouseY);
        if (pos != null && floatingNode != null)
        {
            return floatingNode.node().nudgePlacementPos(this, pos, floatingNode.rotation(), mouseX, mouseY);
        }
        return pos;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return super.isMouseOver(mouseX, mouseY) && !owner.getToolPane().getLibraryBrowser().isCoveredByInventory(mouseX, mouseY);
    }

    public CompoundPrototypeNode getRootNode()
    {
        return circuit;
    }

    public PartGrid getPartGrid()
    {
        return partGrid;
    }

    public WireGrid getWireGrid()
    {
        return wireGrid;
    }

    public boolean isNodeOccupied(NodePos pos)
    {
        return partGrid.getPartNode(pos) != null || wireGrid.getWireNode(pos) != null;
    }

    public boolean canConnectToPart(PlaceableNode node, NodePos pos, Port port, WireType type)
    {
        return node.hasPort(port, type) && !node.isConnected(port) && !isPortObstructed(pos, port);
    }

    public boolean isPortObstructed(NodePos pos, Port port)
    {
        return partGrid.getPartNode(pos.offset(port)) != null;
    }

    public boolean isPullingWire()
    {
        return wireInProgress != null;
    }

    public void startWirePull(WireType wireType)
    {
        wireInProgress = new WireInProgress(wireType);
    }

    public void cancelWirePull()
    {
        wireInProgress = null;
    }

    public void pullWire(double mouseX, double mouseY, boolean doubleClick)
    {
        Objects.requireNonNull(wireInProgress);
        // TODO: inform user on failure
        wireInProgress.placeNextNode(this, mouseX, mouseY, doubleClick);
    }

    @Nullable
    public WireInProgress getWireInProgress()
    {
        return wireInProgress;
    }

    public ErrorAnnotations getErrorAnnotations()
    {
        return errorAnnotations;
    }

    public void importPrototype(CompoundPrototypeNode circuit)
    {
        clear();

        circuit.getWires().forEach(wireGrid::importWireDirect);
        circuit.getChildNodes().forEach(partGrid::importPartNode);
        this.circuit.copyConnectionsFrom(circuit);
    }

    @Nullable
    public ContextMenuProvider getContextMenuProvider(double mouseX, double mouseY)
    {
        NodePos pos = getNodePos((int) mouseX, (int) mouseY);
        if (pos == null) return null;

        PlaceableNode node = partGrid.getPartNode(pos);
        return switch (node)
        {
            case Connection con -> new ConnectionNodeContextMenuProvider(this, con);
            case ConstantPrototypeNode constant -> new ConstantPartNodeContextMenuProvider(this, constant);
            case ClockPrototypeNode clock -> new ClockPartNodeContextMenuProvider(this, clock);
            case LampPrototypeNode lamp -> new LampPartNodeContextMenuProvider(this, lamp);
            case ConverterPrototypeNode conv -> new ConverterPartNodeContextMenuProvider(this, conv);
            case PrototypeNode proto -> new PartNodeContextMenuProvider<>(this, proto);
            case null, default -> null;
        };
    }

    @Override
    public void computeWindowSize(int width, int height)
    {
        int windowPadding = CircuitWorkbenchScreen.PADDING * 2;
        this.width = Math.min(WIDTH, width - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_WIDTH);
        this.height = Math.min(HEIGHT, height - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_HEIGHT);
    }

    @Override
    public void computeWindowPos(int leftPos, int topPos)
    {
        x = leftPos + CircuitWorkbenchScreen.BORDER_LEFT;
        y = topPos + CircuitWorkbenchScreen.OFFSET_TOP;
    }

    public void clear()
    {
        circuit.clear();
        partGrid.clear();
        wireGrid.clear();
        errorAnnotations.clear();
    }

    public boolean isEmpty()
    {
        return circuit.isEmpty();
    }

    @Override
    @Nullable
    public PlaceableNode getPartNode(NodePos pos)
    {
        return partGrid.getPartNode(pos);
    }

    @Override
    public boolean isValidPos(NodePos pos)
    {
        return pos.isValid(PART_COUNT_X, PART_COUNT_Y);
    }
}
