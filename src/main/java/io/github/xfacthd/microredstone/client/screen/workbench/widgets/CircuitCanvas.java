package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.client.screen.workbench.element.CircuitCanvasContentRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.PartRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.element.WireRenderState;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ClockPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConnectionNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConverterPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.PartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.RoutedWire;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireInProgress;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireNode;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.ToIntFunction;

public final class CircuitCanvas
{
    private static final ResourceLocation BLUEPRINT = Utils.rl("blueprint");
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
    public static final int PART_COUNT_X = 48;
    public static final int PART_COUNT_Y = 24;
    public static final int PART_COUNT = PART_COUNT_X * PART_COUNT_Y;
    public static final int PART_SIZE = 8;
    public static final int PART_SLOT_SIZE = PART_SIZE + 1;
    public static final int BORDER_TOP_LEFT = 4;
    private static final int BORDER_BOTTOM_RIGHT = 5;
    public static final int WIDTH = PART_SLOT_SIZE * PART_COUNT_X + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;
    public static final int HEIGHT = PART_SLOT_SIZE * PART_COUNT_Y + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;
    public static final int CELL_COORD_TEXT_HEIGHT = 10;
    private static final int CELL_COORD_TEXT_OFF_Y = CELL_COORD_TEXT_HEIGHT + PADDING;

    private final CircuitWorkbenchScreen owner;
    private final CompoundPrototypeNode circuit = new CompoundPrototypeNode();
    private final PartGrid partGrid = new PartGrid(this);
    private final WireGrid wireGrid = new WireGrid(this);
    private final ErrorAnnotations errorAnnotations = new ErrorAnnotations(this);
    private int x;
    private int y;
    private int width;
    private int height;
    private float canvasOffX;
    private float canvasOffY;
    private float canvasScale = 1F; // TODO: implement zoom support
    @Nullable
    private WireInProgress wireInProgress;

    public CircuitCanvas(CircuitWorkbenchScreen owner)
    {
        this.owner = owner;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.enableScissor(x, y, x + width, y + height);

        int canvasX = x - (int) canvasOffX;
        int canvasY = y - (int) canvasOffY;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLUEPRINT, canvasX, canvasY, WIDTH, HEIGHT);

        List<PartRenderState> parts = new ArrayList<>();
        List<WireRenderState> wires = new ArrayList<>();

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
                parts.add(new PartRenderState(node));
            }
        });

        for (Connection connection : circuit.getConnections())
        {
            if (connection != null && !owner.isNodeFloating(connection))
            {
                parts.add(new PartRenderState(connection));
            }
        }

        if (!parts.isEmpty() || !wires.isEmpty())
        {
            graphics.submitGuiElementRenderState(CircuitCanvasContentRenderState.create(
                    parts, wires, canvasX, canvasY, graphics.peekScissorStack()
            ));
        }

        NodePos hovered = getNodePos(mouseX, mouseY);
        FloatingNode floatingNode = owner.getFloatingNode();
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
            if (hovered != null && (!canPlace || !hovered.equals(pos)))
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

        graphics.disableScissor();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CircuitWorkbenchScreen.WINDOW_FRAME, x, y, width, height);

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
        blitter.blit(pose, icon.portOverlay(), x, y, partSize);
        if (rotation != 0)
        {
            pose.popMatrix();
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return mouseX > x &&
               mouseX < (x + width - 1) &&
               mouseY > y &&
               mouseY < (y + height - 1);
    }

    @Nullable
    public NodePos getNodePos(int mouseX, int mouseY)
    {
        if (!isMouseOver(mouseX, mouseY)) return null;
        if (owner.getLibraryBrowser().isCoveredByInventory(mouseX, mouseY)) return null;

        int relX = mouseX - x + (int) canvasOffX - BORDER_TOP_LEFT;
        int relY = mouseY - y + (int) canvasOffY - BORDER_TOP_LEFT;
        if (relX < 0 || relY < 0) return null;

        int slotX = relX / PART_SLOT_SIZE;
        int slotY = relY / PART_SLOT_SIZE;
        return slotX < PART_COUNT_X && slotY < PART_COUNT_Y ? new NodePos(slotX, slotY) : null;
    }

    @Nullable
    public ExactNodePos getExactNodePos(double mouseX, double mouseY)
    {
        if (!isMouseOver(mouseX, mouseY)) return null;

        double relX = mouseX - x + (int) canvasOffX - BORDER_TOP_LEFT;
        double relY = mouseY - y + (int) canvasOffY - BORDER_TOP_LEFT;
        if (relX < 0 || relY < 0) return null;

        double slotX = relX / PART_SLOT_SIZE;
        double slotY = relY / PART_SLOT_SIZE;
        return slotX < PART_COUNT_X && slotY < PART_COUNT_Y ? new ExactNodePos(slotX, slotY) : null;
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
            case ConverterPrototypeNode conv -> new ConverterPartNodeContextMenuProvider(this, conv);
            case PrototypeNode proto -> new PartNodeContextMenuProvider<>(this, proto);
            case null, default -> null;
        };
    }

    public void computeWindowSize(int width, int height)
    {
        int windowPadding = CircuitWorkbenchScreen.PADDING * 2;
        this.width = Math.min(WIDTH, width - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_WIDTH);
        this.height = Math.min(HEIGHT, height - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_HEIGHT);
    }

    public void computeWindowPos(int leftPos, int topPos)
    {
        x = leftPos + CircuitWorkbenchScreen.BORDER_LEFT;
        y = topPos + CircuitWorkbenchScreen.OFFSET_TOP;
    }

    public int getWindowWidth()
    {
        return width;
    }

    public int getWindowHeight()
    {
        return height;
    }

    public void drag(ArrowKey.Direction dir)
    {
        drag(dir.getDiffX(), dir.getDiffY());
    }

    public void drag(float xDiff, float yDiff)
    {
        canvasOffX = Mth.clamp(canvasOffX + xDiff, 0, WIDTH - width);
        canvasOffY = Mth.clamp(canvasOffY + yDiff, 0, HEIGHT - height);
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
