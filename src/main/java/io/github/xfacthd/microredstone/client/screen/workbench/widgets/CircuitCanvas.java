package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.element.LampRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ClockPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConnectionNodeContextMenuProvider;
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
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.ToIntFunction;

public final class CircuitCanvas extends AbstractCircuitCanvas
{
    private static final ResourceLocation MONOSPACE_FONT = Utils.rl("monospace");
    private static final ResourceLocation[] PORT_BORDERS = Util.make(new ResourceLocation[8], arr ->
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
        for (RoutedWire wire : wireGrid)
        {
            WireType type = wire.wire().getWireType();
            DyeColor color = wire.wire().getColor();

            WireRenderState renderState = new WireRenderState(type, color, new ArrayList<>(wire.sections()), new ArrayList<>());
            for (WireNode node : wire.wire().getNodes())
            {
                renderState.addNode(node);
            }
            wires.add(renderState);
        }

        if (wireInProgress != null)
        {
            wireInProgress.repath(this, mouseX, mouseY);

            WireType type = wireInProgress.getType();
            DyeColor color = wireInProgress.getColor();

            WireRenderState renderState = new WireRenderState(type, color, new ArrayList<>(wireInProgress.getSections()), new ArrayList<>());
            List<WireNode> wireNodes = wireInProgress.getWireNodes();
            if (!wireNodes.isEmpty())
            {
                for (WireNode node : wireNodes)
                {
                    renderState.addNode(node);
                }
            }
            List<WireNode> floatingNodes = wireInProgress.getFloatingNodes();
            if (!floatingNodes.isEmpty())
            {
                // If a floating node is present, then at least one pinned node exists
                NodePos lastPos = wireNodes.getLast().pos();
                for (WireNode node : floatingNodes)
                {
                    renderState.addSection(lastPos, node.pos());
                    renderState.addNode(node);
                    lastPos = node.pos();
                }
            }

            if (!renderState.isEmpty())
            {
                wires.add(renderState);
            }
        }

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
            }
        });

        for (Connection connection : circuit.getConnections())
        {
            if (connection != null && !owner.isNodeFloating(connection))
            {
                parts.add(new PartRenderState(connection));
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
            if (port != null && node.hasPort(port, type))
            {
                ResourceLocation icon = PORT_BORDERS[port.ordinal() << 1 | type.ordinal()];
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
        graphics.renderOutline(x, y, PART_SLOT_SIZE + 1, PART_SLOT_SIZE + 1, color);
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
        if (pos != null && floatingNode instanceof FloatingNode.Part part && part.node() instanceof LampPrototypeNode floatingLamp)
        {
            return getLampPlacementPos(pos, part, floatingLamp, mouseX, mouseY);
        }
        return pos;
    }

    private NodePos getLampPlacementPos(NodePos pos, FloatingNode.Part part, LampPrototypeNode floatingLamp, int mouseX, int mouseY)
    {
        if (!(partGrid.getPartNode(pos) instanceof LampPrototypeNode lamp)) return pos;
        if (lamp == floatingLamp) return pos;

        ExactNodePos exactPos = getExactNodePos(mouseX, mouseY);
        if (exactPos == null) return pos;

        Port hoveredPort = Port.ofCross(exactPos.fracX(), exactPos.fracY());
        if (Port.LEFT.rotate(lamp.getRotation()) == hoveredPort) return pos;
        if (Port.LEFT.rotate(part.rotation()) != hoveredPort.getOpposite()) return pos;

        NodePos lampPos = pos.offset(hoveredPort);
        return lampPos.isValid(PART_COUNT_X, PART_COUNT_Y) ? lampPos : pos;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return super.isMouseOver(mouseX, mouseY) && !owner.getLibraryBrowser().isCoveredByInventory(mouseX, mouseY);
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

    public void pullWire(double mouseX, double mouseY)
    {
        Objects.requireNonNull(wireInProgress);
        // TODO: inform user on failure
        wireInProgress.placeNextNode(this, mouseX, mouseY);
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

    @Nullable
    public ContextMenuProvider getContextMenuProvider(double mouseX, double mouseY)
    {
        NodePos pos = getNodePos((int) mouseX, (int) mouseY);
        if (pos == null) return null;

        PlaceableNode node = partGrid.getPartNode(pos);
        return switch (node)
        {
            case Connection con -> new ConnectionNodeContextMenuProvider(this, con);
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
}
