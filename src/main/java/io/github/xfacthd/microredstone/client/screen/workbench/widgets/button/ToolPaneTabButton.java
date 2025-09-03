package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.widgets.button.SimpleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;

public final class ToolPaneTabButton extends SimpleButton implements DropFocusAfterClick
{
    public static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH / ToolPaneTab.TAB_COUNT;
    public static final int HEIGHT = 18;
    private static final WidgetSprites SPRITES_UNSELECTED = sprites(Utils.rl("tab/tab_unselected"));
    private static final WidgetSprites SPRITES_LEFT = sprites(Utils.rl("tab/tab_selected_left"));
    private static final WidgetSprites SPRITES_CENTER = sprites(Utils.rl("tab/tab_selected_center"));
    private static final WidgetSprites SPRITES_RIGHT = sprites(Utils.rl("tab/tab_selected_right"));

    private final CircuitWorkbenchScreen owner;
    private final ToolPaneTab tab;
    private final WidgetSprites selectedSprites;

    public ToolPaneTabButton(CircuitWorkbenchScreen owner, ToolPaneTab tab, int x, int y)
    {
        super(x, y, WIDTH, HEIGHT, tab.getTitle());
        this.owner = owner;
        this.tab = tab;
        if (tab.ordinal() == 0)
        {
            this.selectedSprites = SPRITES_LEFT;
        }
        else if (tab.ordinal() == ToolPaneTab.MAX_TAB_IDX)
        {
            this.selectedSprites = SPRITES_RIGHT;
        }
        else
        {
            this.selectedSprites = SPRITES_CENTER;
        }
    }

    @Override
    protected WidgetSprites getSprites()
    {
        return owner.getToolPaneTab() == tab ? selectedSprites : SPRITES_UNSELECTED;
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color)
    {
        graphics.drawString(font, getMessage(), getX() + 5, getY() + 5, 0xFF404040, false);
    }

    @Override
    public void onPress()
    {
        owner.setToolPaneTab(tab);
    }
}
