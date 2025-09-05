package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class LibraryActionButton<A extends Enum<A> & LibraryActionButton.Action<LibraryBrowser>> extends Button implements DropFocusAfterClick, ActionButton<A>
{
    private static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH - 10;
    public static final int HEIGHT = 20;

    private final LibraryBrowser owner;
    private final A action;

    public LibraryActionButton(LibraryBrowser owner, A action)
    {
        super(0, 0, WIDTH, HEIGHT, action.getTitle(), btn -> action.execute(owner), DEFAULT_NARRATION);
        this.owner = owner;
        this.action = action;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        active = action.isActive(owner);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public A getAction()
    {
        return action;
    }

    public interface Action<H extends ToolPaneTabWidget>
    {
        boolean isActive(H handler);

        void execute(H handler);

        Component getTitle();
    }
}
