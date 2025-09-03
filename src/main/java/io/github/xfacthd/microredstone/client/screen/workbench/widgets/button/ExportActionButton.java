package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import net.minecraft.client.gui.components.Button;

public final class ExportActionButton extends Button implements DropFocusAfterClick, ActionButton<LibraryBrowser.ExportAction>
{
    private static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH - 10;
    public static final int HEIGHT = 20;

    private final LibraryBrowser.ExportAction action;

    public ExportActionButton(LibraryBrowser owner, LibraryBrowser.ExportAction action)
    {
        super(0, 0, WIDTH, HEIGHT, action.getTitle(), btn -> action.execute(owner), DEFAULT_NARRATION);
        this.action = action;
    }

    @Override
    public LibraryBrowser.ExportAction getAction()
    {
        return action;
    }
}
