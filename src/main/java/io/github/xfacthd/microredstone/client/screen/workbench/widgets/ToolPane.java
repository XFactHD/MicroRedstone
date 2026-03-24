package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import io.github.xfacthd.microredstone.client.screen.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.DragStart;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolsTab;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ToolPane implements GuiEventListener, Renderable
{
    private final PartsList partsList;
    private final ToolsTab toolsTab;
    private final LibraryBrowser libraryBrowser;
    private final ToolPaneTabWidget[] tabWidgets;
    private ToolPaneTab activeTab = ToolPaneTab.PARTS;

    public ToolPane(CircuitWorkbenchScreen owner)
    {
        this.partsList = new PartsList(owner, this);
        this.toolsTab = new ToolsTab(owner, this);
        this.libraryBrowser = new LibraryBrowser(owner, this);
        this.tabWidgets = new ToolPaneTabWidget[] { partsList, toolsTab, libraryBrowser };
    }

    public void init(Consumer<AbstractWidget> widgetAdder)
    {
        List<AbstractWidget> content = new ArrayList<>();
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.initHeader(widgetAdder);
            widget.initContent(content::add);
            widget.updateWidgetVisibility(widget.getType() == ToolPaneTab.PARTS);
        }
        content.forEach(widgetAdder);
    }

    public void computeLayout(int leftPos, int topPos, int imageWidth, int imageHeight, int toolPaneX, int toolPaneY, int height)
    {
        for (ToolPaneTabWidget widget : tabWidgets)
        {
            widget.computeLayout(leftPos, topPos, imageWidth, imageHeight, toolPaneX, toolPaneY, height);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        getActiveTabWidget().extractRenderState(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        return getActiveTabWidget().mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        return getActiveTabWidget().mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY)
    {
        return getActiveTabWidget().mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        return getActiveTabWidget().mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        return getActiveTabWidget().keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event)
    {
        return getActiveTabWidget().keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event)
    {
        return getActiveTabWidget().charTyped(event);
    }

    public boolean canDeletePart(FloatingNode floatingNode, @Nullable NodePos target, double mouseX, double mouseY)
    {
        if (target != null || floatingNode.lastPos() == null) return false;
        if (activeTab != ToolPaneTab.PARTS) return false;
        return partsList.getScrollableWidget().isMouseOver(mouseX, mouseY);
    }

    public void setActiveTab(ToolPaneTab tab)
    {
        if (tab != activeTab)
        {
            activeTab = tab;
            for (ToolPaneTabWidget widget : tabWidgets)
            {
                widget.updateWidgetVisibility(tab == widget.getType());
            }
        }
    }

    public ToolPaneTab getActiveTab()
    {
        return activeTab;
    }

    public ToolPaneTabWidget getActiveTabWidget()
    {
        return switch (activeTab)
        {
            case PARTS -> partsList;
            case TOOLS -> toolsTab;
            case LIBRARY -> libraryBrowser;
        };
    }

    public PartsList getPartsList()
    {
        return partsList;
    }

    public ToolsTab getToolsTab()
    {
        return toolsTab;
    }

    public LibraryBrowser getLibraryBrowser()
    {
        return libraryBrowser;
    }

    @Nullable
    public ContextMenuProvider getContextMenuProviderAt(double mouseX, double mouseY)
    {
        ScrollableWidget scrollable = getActiveTabWidget().getScrollableWidget();
        if (scrollable != null && scrollable.isMouseOver(mouseX, mouseY))
        {
            return scrollable.getContextMenuProvider(mouseX, mouseY);
        }
        return null;
    }

    public boolean isHoveringPartSource(double mouseX, double mouseY)
    {
        return activeTab == ToolPaneTab.PARTS && partsList.getScrollableWidget().isMouseOverList(mouseX, mouseY);
    }

    @Nullable
    public DragStart getClickedPart(double mouseX, double mouseY)
    {
        return partsList.getClickedPartIdx(mouseY);
    }

    @Override
    public void setFocused(boolean focused) { }

    @Override
    public boolean isFocused()
    {
        return false;
    }
}
