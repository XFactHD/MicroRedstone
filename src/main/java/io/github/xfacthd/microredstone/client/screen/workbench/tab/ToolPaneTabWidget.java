package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ToolPane;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ToolPaneTabButton;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract sealed class ToolPaneTabWidget implements GuiEventListener permits PartsList, ToolsTab, LibraryBrowser
{
    public static final int TOOL_PANE_WIDTH = 144;
    private static final int MAX_HEIGHT = CircuitCanvas.HEIGHT;
    private static final int HEADER_HEIGHT = 15;
    private static final ResourceLocation BACKGROUND = Utils.rl("minecraft", "toast/tutorial");

    protected final CircuitWorkbenchScreen owner;
    protected final ToolPane toolPane;
    private final ToolPaneTabButton tabButton;
    protected int paneX;
    protected int paneY;
    protected int height;

    protected ToolPaneTabWidget(CircuitWorkbenchScreen owner, ToolPane toolPane)
    {
        this.owner = owner;
        this.toolPane = toolPane;
        this.tabButton = new ToolPaneTabButton(toolPane, getType(), 0, 0);
    }

    public final void render(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, paneX, paneY, TOOL_PANE_WIDTH, height);
        renderContent(graphics, mouseX, mouseY);
    }

    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY);

    public final void initHeader(Consumer<AbstractWidget> widgetAdder)
    {
        widgetAdder.accept(tabButton);
    }

    public abstract void initContent(Consumer<AbstractWidget> widgetAdder);

    public void computeLayout(int screenX, int screenY, int screenWidth, int screenHeight, int toolPaneX, int toolPaneY, int windowHeight)
    {
        int xOff = getType().ordinal() * ToolPaneTabButton.WIDTH;
        tabButton.setPosition(toolPaneX + xOff, toolPaneY);

        paneX = toolPaneX;
        paneY = toolPaneY + HEADER_HEIGHT;

        int windowPadding = CircuitWorkbenchScreen.PADDING * 2;
        this.height = Math.min(MAX_HEIGHT, windowHeight - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_HEIGHT) - HEADER_HEIGHT;
    }

    public abstract void updateWidgetVisibility(boolean active);

    @Nullable
    public ScrollableWidget getScrollableWidget()
    {
        return null;
    }

    public abstract ToolPaneTab getType();

    protected static <T extends ToolPaneTabWidget, E extends Enum<E>, B extends AbstractButton & ActionButton<E>> List<B> makeActionButtons(
            T tab, E[] actions, BiFunction<T, E, B> buttonFactory
    )
    {
        return Arrays.stream(actions)
                .map(action -> buttonFactory.apply(tab, action))
                .toList();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        ScrollableWidget scrollable = getScrollableWidget();
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && scrollable != null && (scrollable.isDragging() || scrollable.isMouseOverScrollBar(mouseX, mouseY)))
        {
            scrollable.dragScrollBar(mouseY);
            scrollable.setDragging(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        ScrollableWidget scrollable = getScrollableWidget();
        if (button == GLFW.GLFW_MOUSE_BUTTON_1 && scrollable != null && scrollable.isDragging())
        {
            scrollable.setDragging(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        ScrollableWidget scrollable = getScrollableWidget();
        if (scrollable != null && scrollable.isMouseOver(mouseX, mouseY))
        {
            scrollable.scroll(-scrollY);
            return true;
        }
        return false;
    }

    @Override
    public final boolean isFocused()
    {
        return false;
    }

    @Override
    public final void setFocused(boolean focused) { }
}
