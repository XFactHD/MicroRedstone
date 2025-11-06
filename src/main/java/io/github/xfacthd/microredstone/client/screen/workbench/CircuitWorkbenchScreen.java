package io.github.xfacthd.microredstone.client.screen.workbench;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenu;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProviderProxy;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ToolPane;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.DropFocusAfterClick;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.client.util.Icon;
import io.github.xfacthd.microredstone.client.util.ScreenUtils;
import io.github.xfacthd.microredstone.common.block.CircuitWorkbenchBlock;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.menu.slot.ToggleableSlot;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

// TODO: add "modified" flag to prevent data loss when accidentally closing the screen and adjust close confirmation
//       to use it instead of canvas emptiness, can be re-used to cache assembly result
// TODO: add local storage for (partial) designs in prototype stage
public final class CircuitWorkbenchScreen extends AbstractContainerScreen<CircuitWorkbenchMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("background");
    public static final ResourceLocation WINDOW_FRAME = Utils.rl("window_frame");
    public static final Component TITLE_CONFIRM_CLOSE = Utils.translate("title", "circuit_workbench.close.confirm");
    public static final Component MESSAGE_CONFIRM_CLOSE_LINE_ONE = Utils.translate("msg", "circuit_workbench.close.confirm_line_one", CircuitWorkbenchBlock.MENU_TITLE);
    public static final Component MESSAGE_CONFIRM_CLOSE_LINE_TWO = Utils.translate("msg", "circuit_workbench.close.confirm_line_two");
    public static final int PADDING = 5;
    public static final int BORDER_LEFT = PADDING * 2;
    public static final int BORDER_RIGHT = PADDING * 2 + 1;
    public static final int OFFSET_TOP = 20;
    private static final int BORDER_BOTTOM = PADDING * 2 + 1;
    public static final int NON_CIRCUIT_WIDTH = BORDER_LEFT + BORDER_RIGHT + ToolPaneTabWidget.TOOL_PANE_WIDTH + PADDING;
    public static final int NON_CIRCUIT_HEIGHT = OFFSET_TOP + BORDER_BOTTOM + CircuitCanvas.CELL_COORD_TEXT_HEIGHT;
    private static final long ARROW_REPEAT_DELAY_INITIAL = 300;

    private final CircuitCanvas canvas = new CircuitCanvas(this);
    private final ToolPane toolPane = new ToolPane(this);
    private final ContextMenu contextMenu = new ContextMenu(this);
    private final ImportExportHandler importExportHandler = new ImportExportHandler(this);
    @Nullable
    private DragStart dragStart = null;
    @Nullable
    private FloatingNode floatingNode = null;
    @Nullable
    private ArrowKey activeArrowKey = null;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean isDraggingCanvas = false;

    public CircuitWorkbenchScreen(CircuitWorkbenchMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
    }

    @Override
    protected void init()
    {
        addRenderableOnly(canvas);
        addRenderableOnly(toolPane);
        toolPane.init(this::addRenderableWidget);
        super.init();
        repositionElements();
    }

    @Override
    protected void repositionElements()
    {
        if (contextMenu.isOpen())
        {
            contextMenu.close();
        }

        canvas.computeWindowSize(width, height);
        imageWidth = canvas.getWindowWidth() + NON_CIRCUIT_WIDTH;
        imageHeight = canvas.getWindowHeight() + NON_CIRCUIT_HEIGHT;

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

        canvas.computeWindowPos(leftPos, topPos);

        int toolPaneX = leftPos + imageWidth - BORDER_RIGHT - ToolPaneTabWidget.TOOL_PANE_WIDTH;
        int toolPaneY = topPos + OFFSET_TOP;
        toolPane.computeLayout(leftPos, topPos, imageWidth, imageHeight, toolPaneX, toolPaneY, height);

        LibraryBrowser libraryBrowser = toolPane.getLibraryBrowser();
        inventoryLabelX = libraryBrowser.getInvLabelX() - leftPos;
        inventoryLabelY = libraryBrowser.getInvLabelY() - topPos;

        canvas.drag(0, 0); // Clamp canvas offset
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        boolean overMenu = contextMenu.isOpen() && contextMenu.isMouseOver(mouseX, mouseY);
        lastMouseX = overMenu ? -1 : mouseX;
        lastMouseY = overMenu ? -1 : mouseY;
        super.render(graphics, lastMouseX, lastMouseY, partialTick);
        contextMenu.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isDraggingCanvas)
        {
            graphics.requestCursor(CursorTypes.RESIZE_ALL);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderContents(graphics, mouseX, mouseY, partialTick);

        if (floatingNode != null)
        {
            CircuitCanvas.drawPartNode(graphics, floatingNode.node().getIcon(), mouseX - 4, mouseY - 4, floatingNode.rotation(), CircuitCanvas.PART_SIZE);
        }
        if (canvas.isPullingWire() && Objects.requireNonNull(canvas.getWireInProgress()).isEmpty())
        {
            Icon icon = canvas.getWireInProgress().getIcon();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.texture(), mouseX - 4, mouseY - 8, 8, 8, icon.color());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        if (toolPane.getActiveTab() == ToolPaneTab.LIBRARY)
        {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (contextMenu.isOpen())
        {
            if (!contextMenu.shouldKeepMenuOpen((int) event.x(), (int) event.y(), true))
            {
                contextMenu.close();
            }
            else if (contextMenu.isMouseOver(event.x(), event.y()))
            {
                return contextMenu.mouseClicked(event, doubleClick);
            }
        }
        else if (!hasActiveEditAction() && event.button() == GLFW.GLFW_MOUSE_BUTTON_2)
        {
            ContextMenuProvider provider = getContextMenuProviderAt(event.x(), event.y());
            if (provider != null)
            {
                if (contextMenu.open((int) event.x(), (int) event.y(), provider))
                {
                    setFocused(contextMenu);
                }
                return true;
            }
        }
        if (!isDragging() && event.button() == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            GuiEventListener focused = getFocused();
            if (focused != null && !focused.isMouseOver(event.x(), event.y()))
            {
                setFocused(null);
            }

            if (canvas.isPullingWire())
            {
                canvas.pullWire((int) event.x(), (int) event.y(), doubleClick);
                return true;
            }

            NodePos nodePos = canvas.getNodePos((int) event.x(), (int) event.y());
            if (nodePos != null)
            {
                dragStart = DragStart.canvas(nodePos);
            }
            else if (toolPane.isHoveringPartSource(event.x(), event.y()))
            {
                dragStart = toolPane.getClickedPart(event.x(), event.y());
            }
            if (dragStart != null)
            {
                setDragging(true);
                return true;
            }
        }
        if (super.mouseClicked(event, doubleClick))
        {
            if (getFocused() instanceof DropFocusAfterClick && getFocused().isMouseOver(event.x(), event.y()))
            {
                setFocused(null);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)
    {
        if (floatingNode != null)
        {
            return true;
        }
        if (dragStart != null)
        {
            PlaceableNode node = dragStart.resolve(this);
            if (node != null)
            {
                NodePos lastPos = dragStart.canvas().orElse(null);
                floatingNode = FloatingNode.of(node, lastPos);
            }
            dragStart = null;
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_3 && canvas.canDrag(event.x(), event.y()))
        {
            canvas.drag((float) -dragX, (float) -dragY);
            isDraggingCanvas = true;
            return true;
        }
        if (toolPane.mouseDragged(event, dragX, dragY))
        {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (toolPane.mouseReleased(event))
        {
            return true;
        }

        if (isDraggingCanvas && event.button() == GLFW.GLFW_MOUSE_BUTTON_3)
        {
            isDraggingCanvas = false;
            return true;
        }
        if (isDragging())
        {
            if (floatingNode != null)
            {
                NodePos target = canvas.getNodePlacementPos((int) event.x(), (int) event.y(), floatingNode);
                boolean revertToLast = false;
                if (target != null && !floatingNode.canPlaceAt(canvas, target))
                {
                    target = null;
                }
                if (target == null && !toolPane.canDeletePart(floatingNode, target, event.x(), event.y()))
                {
                    target = floatingNode.lastPos();
                    revertToLast = true;
                }
                if (target != null)
                {
                    floatingNode.placeAt(canvas, target, revertToLast, (int) event.x(), (int) event.y());
                }
                else if (floatingNode.lastPos() != null)
                {
                    canvas.getPartGrid().removePartNode(Objects.requireNonNull(floatingNode.lastPos()));
                }
                floatingNode = null;
            }
            setDragging(false);
            dragStart = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (toolPane.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
        {
            return true;
        }
        if (floatingNode != null)
        {
            floatingNode = floatingNode.rotate(Mth.sign(-scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (contextMenu.isOpen())
        {
            if (event.isEscape())
            {
                contextMenu.close();
                return true;
            }
            return contextMenu.keyPressed(event);
        }
        if (toolPane.keyPressed(event) || (toolPane.getLibraryBrowser().isNameEditFocused() && !event.isEscape()))
        {
            return true;
        }
        ArrowKey.Direction arrowDir = ArrowKey.Direction.of(event.key());
        if (arrowDir != null)
        {
            activeArrowKey = new ArrowKey(arrowDir, System.currentTimeMillis());
            canvas.drag(arrowDir);
            return true;
        }
        if (canvas.isPullingWire() && event.isEscape())
        {
            canvas.cancelWirePull();
            return true;
        }
        if (!hasActiveEditAction())
        {
            if (event.key() == GLFW.GLFW_KEY_W)
            {
                canvas.startWirePull(WireType.SINGLE);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_B)
            {
                canvas.startWirePull(WireType.BUNDLED);
                return true;
            }
        }
        if (floatingNode != null && event.key() == GLFW.GLFW_KEY_R)
        {
            floatingNode = floatingNode.rotate(Minecraft.getInstance().hasShiftDown() ? -1 : 1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE)
        {
            NodePos pos = canvas.getNodePos(lastMouseX, lastMouseY);
            if (pos != null && canvas.getPartGrid().getPartNode(pos) != null)
            {
                canvas.getPartGrid().removePartNode(pos);
                return true;
            }
            if (pos != null && canvas.getWireGrid().removeWireAt(pos, Minecraft.getInstance().hasShiftDown()))
            {
                return true;
            }
        }
        if ((event.isEscape() || ScreenUtils.isInventoryKey(event)) && !canvas.isEmpty())
        {
            DialogScreen.builder(DialogScreen.Type.CONFIRM)
                    .withTitle(TITLE_CONFIRM_CLOSE)
                    .withMessage(MESSAGE_CONFIRM_CLOSE_LINE_ONE)
                    .withMessage(MESSAGE_CONFIRM_CLOSE_LINE_TWO)
                    .withOkCallback(this::onClose)
                    .show();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        if (activeArrowKey != null && ArrowKey.Direction.of(event.key()) == activeArrowKey.dir())
        {
            activeArrowKey = null;
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY)
    {
        if (contextMenu.isOpen())
        {
            Optional<GuiEventListener> child = contextMenu.getChildAt(mouseX, mouseY);
            if (child.isPresent()) return child;
        }
        return super.getChildAt(mouseX, mouseY);
    }

    @Override
    protected void containerTick()
    {
        if (activeArrowKey != null)
        {
            long diff = System.currentTimeMillis() - activeArrowKey.startTime();
            if (diff > ARROW_REPEAT_DELAY_INITIAL)
            {
                canvas.drag(activeArrowKey.dir());
            }
        }
    }

    @Nullable
    public FloatingNode getFloatingNode()
    {
        return floatingNode;
    }

    public boolean isNodeFloating(PlaceableNode node)
    {
        return floatingNode != null && floatingNode.node() == node;
    }

    public boolean hasActiveEditAction()
    {
        return floatingNode != null || canvas.isPullingWire();
    }

    public Stream<ToggleableSlot> getSlots()
    {
        return menu.slots.stream()
                .filter(ToggleableSlot.class::isInstance)
                .map(ToggleableSlot.class::cast);
    }

    @Nullable
    private ContextMenuProvider getContextMenuProviderAt(double mouseX, double mouseY)
    {
        Optional<GuiEventListener> child = getChildAt(mouseX, mouseY);
        if (child.isPresent())
        {
            GuiEventListener listener = child.get();
            if (listener instanceof ContextMenuProvider provider)
            {
                return provider;
            }
            if (listener instanceof ContextMenuProviderProxy proxy)
            {
                return proxy.getContextMenuProvider();
            }
            return null;
        }
        ContextMenuProvider provider = toolPane.getContextMenuProviderAt(mouseX, mouseY);
        if (provider != null)
        {
            return provider;
        }
        if (canvas.isMouseOver(mouseX, mouseY))
        {
            return canvas.getContextMenuProvider(mouseX, mouseY);
        }
        return null;
    }

    public CircuitCanvas getCanvas()
    {
        return canvas;
    }

    public ToolPane getToolPane()
    {
        return toolPane;
    }

    public ImportExportHandler getImportExportHandler()
    {
        return importExportHandler;
    }
}
