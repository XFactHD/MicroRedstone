package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
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
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;

// TODO: move canvas content rendering to a PiP renderer
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
    private static final int PART_COUNT_Y = 24;
    public static final int PART_COUNT = PART_COUNT_X * PART_COUNT_Y;
    private static final int PART_SIZE = 8;
    private static final int PART_SLOT_SIZE = PART_SIZE + 1;
    private static final int BORDER_TOP_LEFT = 4;
    private static final int BORDER_BOTTOM_RIGHT = 5;
    public static final int MAX_WIDTH = PART_SLOT_SIZE * PART_COUNT_X + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;
    public static final int MAX_HEIGHT = PART_SLOT_SIZE * PART_COUNT_Y + BORDER_TOP_LEFT + BORDER_BOTTOM_RIGHT;
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
    private int canvasWidth;
    private int canvasHeight;
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

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BLUEPRINT, canvasX, canvasY, canvasWidth, canvasHeight);

        for (RoutedWire wire : wireGrid)
        {
            WireType type = wire.wire().getWireType();
            DyeColor color = wire.wire().getColor();
            for (RoutedWire.Section section : wire.sections())
            {
                drawWireSection(graphics, type, color, section.posOne(), section.posTwo(), canvasX, canvasY);
            }
            for (WireNode node : wire.wire().getNodes())
            {
                drawWireNode(graphics, type, color, node, canvasX, canvasY);
            }
        }

        if (wireInProgress != null)
        {
            wireInProgress.repath(this, mouseX, mouseY);

            WireType type = wireInProgress.getType();
            DyeColor color = wireInProgress.getColor();
            List<RoutedWire.Section> sections = wireInProgress.getSections();
            if (!sections.isEmpty())
            {
                for (RoutedWire.Section section : sections)
                {
                    drawWireSection(graphics, type, color, section.posOne(), section.posTwo(), canvasX, canvasY);
                }
            }
            List<WireNode> wireNodes = wireInProgress.getWireNodes();
            if (!wireNodes.isEmpty())
            {
                for (WireNode node : wireNodes)
                {
                    drawWireNode(graphics, type, color, node, canvasX, canvasY);
                }
            }
            List<WireNode> floatingNodes = wireInProgress.getFloatingNodes();
            if (!floatingNodes.isEmpty())
            {
                // If a floating node is present, then at least one pinned node exists
                NodePos lastPos = wireNodes.getLast().pos();
                for (WireNode node : floatingNodes)
                {
                    drawWireSection(graphics, type, color, lastPos, node.pos(), canvasX, canvasY);
                    drawWireNode(graphics, type, color, node, canvasX, canvasY);
                    lastPos = node.pos();
                }
            }
        }

        partGrid.forEach(node ->
        {
            if (!owner.isNodeFloating(node))
            {
                NodePos pos = node.getPos();
                int iconX = canvasX + BORDER_TOP_LEFT + 1 + pos.x() * PART_SLOT_SIZE;
                int iconY = canvasY + BORDER_TOP_LEFT + 1 + pos.y() * PART_SLOT_SIZE;
                drawPartNode(graphics, node, iconX, iconY);
            }
        });

        for (Connection connection : circuit.getConnections())
        {
            if (connection == null || owner.isNodeFloating(connection)) continue;

            NodePos pos = connection.getPos();
            int iconX = canvasX + BORDER_TOP_LEFT + 1 + pos.x() * PART_SLOT_SIZE;
            int iconY = canvasY + BORDER_TOP_LEFT + 1 + pos.y() * PART_SLOT_SIZE;
            drawPartNode(graphics, connection.getIcon(), iconX, iconY, connection.getRotation());
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

    private static void drawPartNode(GuiGraphics graphics, PrototypeNode node, int x, int y)
    {
        drawPartNode(graphics, node.getIcon(), x, y, node.getRotation());
    }

    public static void drawPartNode(GuiGraphics graphics, IconConfig icon, int x, int y, int rotation)
    {
        drawPartNode(graphics, icon, x, y, rotation, PART_SIZE);
    }

    public static void drawPartNode(GuiGraphics graphics, IconConfig icon, int x, int y, int rotation, int partSize)
    {
        if (!icon.rotateTexture())
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.icon(), x, y, partSize, partSize);
        }
        if (rotation != 0)
        {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().rotateAbout((float) Math.toRadians(90 * rotation), partSize / 2F, partSize / 2F);
            x = 0;
            y = 0;
        }
        if (icon.rotateTexture())
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.icon(), x, y, partSize, partSize);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.portOverlay(), x, y, partSize, partSize);
        if (rotation != 0)
        {
            graphics.pose().popMatrix();
        }
    }

    private static void drawWireNode(GuiGraphics graphics, WireType type, DyeColor color, WireNode node, int canvasX, int canvasY)
    {
        if (node instanceof WireNode.Connection) return;

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

        NodePos pos = node.pos();
        int x = canvasX + BORDER_TOP_LEFT + 1 + pos.x() * PART_SLOT_SIZE + 4;
        int y = canvasY + BORDER_TOP_LEFT + 1 + pos.y() * PART_SLOT_SIZE + 4;
        int packedColor = type == WireType.BUNDLED ? 0xFFFFFFFF : color.getTextureDiffuseColor();
        graphics.fill(x - 2, y - 2, x + 2, y + 2, packedColor);
    }

    private static void drawWireSection(GuiGraphics graphics, WireType type, DyeColor color, NodePos posOne, NodePos posTwo, int canvasX, int canvasY)
    {
        // TODO: shorten sections going into parts to only enter the part by one pixel and then render wires after parts again
        // TODO: draw textures instead of colored lines (same for nodes)

        int packedColor = type == WireType.BUNDLED ? 0xFFFFFFFF : color.getTextureDiffuseColor();
        if (posOne.x() == posTwo.x())
        {
            int minY = Math.min(posOne.y(), posTwo.y());
            int maxY = Math.max(posOne.y(), posTwo.y());
            int x = canvasX + 4 + posOne.x() * PART_SLOT_SIZE + 4;
            int y1 = canvasY + 4 + minY * PART_SLOT_SIZE + 4;
            int y2 = canvasY + 5 + maxY * PART_SLOT_SIZE + 4;
            graphics.vLine(x, y1, y2, packedColor);
            graphics.vLine(x + 1, y1, y2, packedColor);
        }
        else if (posOne.y() == posTwo.y())
        {
            int minX = Math.min(posOne.x(), posTwo.x());
            int maxX = Math.max(posOne.x(), posTwo.x());
            int x1 = canvasX + 4 + minX * PART_SLOT_SIZE + 4;
            int x2 = canvasX + 5 + maxX * PART_SLOT_SIZE + 4;
            int y = canvasY + 4 + posOne.y() * PART_SLOT_SIZE + 4;
            graphics.hLine(x1, x2, y, packedColor);
            graphics.hLine(x1, x2, y + 1, packedColor);
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
        return slotX < getGridWidth() && slotY < getGridHeight() ? new NodePos(slotX, slotY) : null;
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
        return slotX < getGridWidth() && slotY < getGridHeight() ? new ExactNodePos(slotX, slotY) : null;
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

    public void startWirePull(WireType wireType, @Nullable DyeColor color)
    {
        wireInProgress = new WireInProgress(wireType, color);
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

    public void computeWindowSize(int width, int height)
    {
        int windowPadding = CircuitWorkbenchScreen.PADDING * 2;
        this.width = Math.min(MAX_WIDTH, width - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_WIDTH);
        this.height = Math.min(MAX_HEIGHT, height - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_HEIGHT);
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

    public int getGridWidth()
    {
        return PART_COUNT_X;
    }

    public int getGridHeight()
    {
        return PART_COUNT_Y;
    }

    @SuppressWarnings("SameParameterValue")
    public void setCanvasSize(int width, int height)
    {
        canvasWidth = width;
        canvasHeight = height;
    }

    public void drag(ArrowKey.Direction dir)
    {
        drag(dir.getDiffX(), dir.getDiffY());
    }

    public void drag(float xDiff, float yDiff)
    {
        canvasOffX = Mth.clamp(canvasOffX + xDiff, 0, canvasWidth - width);
        canvasOffY = Mth.clamp(canvasOffY + yDiff, 0, canvasHeight - height);
    }

    public void clear()
    {
        circuit.clear();
        partGrid.clear();
        wireGrid.clear();
        errorAnnotations.clear();
    }
}
