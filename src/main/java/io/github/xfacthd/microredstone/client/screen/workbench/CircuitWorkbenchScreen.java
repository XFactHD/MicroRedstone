package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenu;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolsTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.DropFocusAfterClick;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProviderProxy;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.menu.slot.ToggleableSlot;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

// TODO: add "modified" flag to prevent data loss when accidentally closing the screen (can be re-used to cache assembly result)
public final class CircuitWorkbenchScreen extends AbstractContainerScreen<CircuitWorkbenchMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("background");
    public static final ResourceLocation WINDOW_FRAME = Utils.rl("window_frame");
    public static final int PADDING = 5;
    public static final int BORDER_LEFT = PADDING * 2;
    public static final int BORDER_RIGHT = PADDING * 2 + 1;
    public static final int OFFSET_TOP = 20;
    private static final int BORDER_BOTTOM = PADDING * 2 + 1;
    public static final int NON_CIRCUIT_WIDTH = BORDER_LEFT + BORDER_RIGHT + ToolPaneTabWidget.TOOL_PANE_WIDTH + PADDING;
    public static final int NON_CIRCUIT_HEIGHT = OFFSET_TOP + BORDER_BOTTOM + CircuitCanvas.CELL_COORD_TEXT_HEIGHT;
    private static final long ARROW_REPEAT_DELAY_INITIAL = 300;

    private final CircuitCanvas canvas = new CircuitCanvas(this);
    private final PartsList partsList = new PartsList(this);
    private final ToolsTab toolsTab = new ToolsTab(this);
    private final LibraryBrowser libraryBrowser = new LibraryBrowser(this);
    private final ToolPaneTabWidget[] tabWidgets = { partsList, toolsTab, libraryBrowser };
    private final ContextMenu contextMenu = new ContextMenu(this);
    private ToolPaneTab toolPaneTab = ToolPaneTab.PARTS;
    @Nullable
    private DragStart dragStart = null;
    @Nullable
    private FloatingNode floatingNode = null;
    @Nullable
    private ArrowKey activeArrowKey = null;

    public CircuitWorkbenchScreen(CircuitWorkbenchMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        // TODO: consider making the canvas size configurable in the workbench screen (requires storing the size in the assembled circuit for reconstruction)
        canvas.setCanvasSize(CircuitCanvas.MAX_WIDTH, CircuitCanvas.MAX_HEIGHT);
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.updateWidgetVisibility(false);
        }
    }

    @Override
    protected void init()
    {
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.initHeader(this::addRenderableWidget);
        }
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.init(this::addRenderableWidget);
        }
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
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.computeLayout(leftPos, topPos, imageWidth, imageHeight, toolPaneX, toolPaneY, height);
        }

        inventoryLabelX = libraryBrowser.getInvLabelX() - leftPos;
        inventoryLabelY = libraryBrowser.getInvLabelY() - topPos;

        canvas.drag(0, 0); // Clamp canvas offset
        partsList.getScrollableWidget().scroll(0); // Clamp parts list offset
        libraryBrowser.getScrollableWidget().scroll(0); // Clamp import list offset
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        boolean overMenu = contextMenu.isOpen() && contextMenu.isMouseOver(mouseX, mouseY);
        super.render(graphics, overMenu ? -1 : mouseX, overMenu ? -1 : mouseY, partialTick);
        contextMenu.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        canvas.render(graphics, mouseX, mouseY);
        getActiveTabWidget().render(graphics, mouseX, mouseY);
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderContents(graphics, mouseX, mouseY, partialTick);

        if (floatingNode != null)
        {
            CircuitCanvas.drawPartNode(graphics, floatingNode.node().getIcon(), mouseX - 4, mouseY - 4, floatingNode.rotation());
        }
        if (canvas.isPullingWire() && Objects.requireNonNull(canvas.getWireInProgress()).isEmpty())
        {
            WireType.Icon icon = canvas.getWireInProgress().getIcon();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.texture(), mouseX - 4, mouseY - 8, 8, 8, icon.color());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        if (toolPaneTab == ToolPaneTab.LIBRARY && libraryBrowser.isInExportMode())
        {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (contextMenu.isOpen())
        {
            if (!contextMenu.shouldKeepMenuOpen((int) mouseX, (int) mouseY, true))
            {
                contextMenu.close();
            }
            else if (contextMenu.isMouseOver(mouseX, mouseY))
            {
                return contextMenu.mouseClicked(mouseX, mouseY, button);
            }
        }
        else if (!hasActiveEditAction() && button == GLFW.GLFW_MOUSE_BUTTON_2)
        {
            ContextMenuProvider provider = getContextMenuProviderAt(mouseX, mouseY);
            if (provider != null)
            {
                if (contextMenu.open((int) mouseX, (int) mouseY, provider))
                {
                    setFocused(contextMenu);
                }
                return true;
            }
        }
        if (!isDragging() && button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            GuiEventListener focused = getFocused();
            if (focused != null && !focused.isMouseOver(mouseX, mouseY))
            {
                setFocused(null);
            }

            if (canvas.isPullingWire())
            {
                canvas.pullWire((int) mouseX, (int) mouseY);
                return true;
            }

            NodePos nodePos = canvas.getNodePos((int) mouseX, (int) mouseY);
            if (nodePos != null)
            {
                dragStart = DragStart.canvas(nodePos);
            }
            else if (toolPaneTab == ToolPaneTab.PARTS && partsList.getScrollableWidget().isMouseOverList(mouseX, mouseY))
            {
                dragStart = partsList.getClickedPartIdx(mouseY);
            }
            else if (toolPaneTab == ToolPaneTab.LIBRARY && libraryBrowser.isMouseOverImportList(mouseX, mouseY))
            {
                dragStart = libraryBrowser.getClickedEntryIdx(mouseY);
            }
            if (dragStart != null)
            {
                setDragging(true);
                return true;
            }
        }
        if (super.mouseClicked(mouseX, mouseY, button))
        {
            if (getFocused() instanceof DropFocusAfterClick && getFocused().isMouseOver(mouseX, mouseY))
            {
                setFocused(null);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
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
        if (button == GLFW.GLFW_MOUSE_BUTTON_3 && canvas.isMouseOver(mouseX, mouseY))
        {
            canvas.drag((float) -dragX, (float) -dragY);
            return true;
        }
        ScrollableWidget scrollable = getActiveTabWidget().getScrollableWidget();
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && scrollable != null && (scrollable.isDragging() || scrollable.isMouseOverScrollBar(mouseX, mouseY)))
        {
            scrollable.dragScrollBar(mouseY);
            scrollable.setDragging(true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        ScrollableWidget scrollable = getActiveTabWidget().getScrollableWidget();
        if (scrollable != null && scrollable.isDragging())
        {
            scrollable.setDragging(false);
            return true;
        }

        if (isDragging())
        {
            if (floatingNode != null)
            {
                NodePos target = canvas.getNodePos((int) mouseX, (int) mouseY);
                boolean revertToLast = false;
                if (target != null && !floatingNode.canPlaceAt(canvas, target))
                {
                    target = null;
                }
                if (target == null)
                {
                    target = floatingNode.lastPos();
                    revertToLast = true;
                }
                if (target != null)
                {
                    floatingNode.placeAt(canvas, target, revertToLast);
                }
                floatingNode = null;
            }
            setDragging(false);
            dragStart = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        ScrollableWidget scrollable = getActiveTabWidget().getScrollableWidget();
        if (scrollable != null && scrollable.isMouseOver(mouseX, mouseY))
        {
            scrollable.scroll(-scrollY);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (contextMenu.isOpen())
        {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE)
            {
                contextMenu.close();
                return true;
            }
            return contextMenu.keyPressed(keyCode, scanCode, modifiers);
        }
        ArrowKey.Direction arrowDir = ArrowKey.Direction.of(keyCode);
        if (arrowDir != null)
        {
            activeArrowKey = new ArrowKey(arrowDir, System.currentTimeMillis());
            canvas.drag(arrowDir);
            return true;
        }
        if (canvas.isPullingWire() && keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            canvas.cancelWirePull();
            return true;
        }
        if (!hasActiveEditAction())
        {
            if (keyCode == GLFW.GLFW_KEY_W)
            {
                canvas.startWirePull(WireType.SINGLE);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_B)
            {
                canvas.startWirePull(WireType.BUNDLED);
                return true;
            }
        }
        if (floatingNode != null && keyCode == GLFW.GLFW_KEY_R)
        {
            floatingNode = floatingNode.rotate(Screen.hasShiftDown() ? -1 : 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers)
    {
        if (activeArrowKey != null && ArrowKey.Direction.of(keyCode) == activeArrowKey.dir())
        {
            activeArrowKey = null;
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
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

    public void setToolPaneTab(ToolPaneTab tab)
    {
        if (tab != toolPaneTab)
        {
            toolPaneTab = tab;
            for (ToolPaneTabWidget widget : tabWidgets)
            {
                widget.updateWidgetVisibility(tab == widget.getType());
            }
        }
    }

    public ToolPaneTab getToolPaneTab()
    {
        return toolPaneTab;
    }

    public Stream<ToggleableSlot> getSlots()
    {
        return menu.slots.stream()
                .filter(ToggleableSlot.class::isInstance)
                .map(ToggleableSlot.class::cast);
    }

    private ToolPaneTabWidget getActiveTabWidget()
    {
        return switch (toolPaneTab)
        {
            case PARTS -> partsList;
            case TOOLS -> toolsTab;
            case LIBRARY -> libraryBrowser;
        };
    }

    @Nullable
    private ContextMenuProvider getContextMenuProviderAt(double mouseX, double mouseY)
    {
        Optional<GuiEventListener> child = getChildAt(mouseX, mouseY);
        if (child.isEmpty()) return null;

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

    public void assembleAndExport(ExportTarget target)
    {
        // TODO: implement circuit assembly, error handling, overwrite handling and target storage
    }

    public CircuitCanvas getCanvas()
    {
        return canvas;
    }

    public LibraryBrowser getLibraryBrowser()
    {
        return libraryBrowser;
    }

    public enum ExportTarget
    {
        LIBRARY,
        CIRCUIT_ITEM,
    }
}
