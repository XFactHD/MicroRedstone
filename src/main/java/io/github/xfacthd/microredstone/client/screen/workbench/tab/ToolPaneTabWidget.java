package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ToolPaneTabButton;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract sealed class ToolPaneTabWidget permits PartsList, ToolsTab, LibraryBrowser
{
    public static final int TOOL_PANE_WIDTH = 144;
    private static final int MAX_HEIGHT = CircuitCanvas.MAX_HEIGHT;
    private static final int HEADER_HEIGHT = 15;
    private static final ResourceLocation BACKGROUND = Utils.rl("minecraft", "toast/tutorial");

    protected final CircuitWorkbenchScreen owner;
    private final ToolPaneTabButton tabButton;
    protected int paneX;
    protected int paneY;
    protected int height;

    protected ToolPaneTabWidget(CircuitWorkbenchScreen owner)
    {
        this.owner = owner;
        this.tabButton = new ToolPaneTabButton(owner, getType(), 0, 0);
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

    public abstract void init(Consumer<AbstractWidget> widgetAdder);

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
}
