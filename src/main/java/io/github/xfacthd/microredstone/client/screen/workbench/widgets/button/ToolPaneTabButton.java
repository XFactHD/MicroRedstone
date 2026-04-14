package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.widgets.button.SimpleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ToolPane;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.util.CommonColors;

public final class ToolPaneTabButton extends SimpleButton implements DropFocusAfterClick {
    public static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH / ToolPaneTab.TAB_COUNT;
    public static final int HEIGHT = 18;
    private static final WidgetSprites SPRITES_UNSELECTED = sprites(Utils.id("tab/tab_unselected"));
    private static final WidgetSprites SPRITES_LEFT = sprites(Utils.id("tab/tab_selected_left"));
    private static final WidgetSprites SPRITES_CENTER = sprites(Utils.id("tab/tab_selected_center"));
    private static final WidgetSprites SPRITES_RIGHT = sprites(Utils.id("tab/tab_selected_right"));

    private final ToolPane owner;
    private final ToolPaneTab tab;
    private final WidgetSprites selectedSprites;

    public ToolPaneTabButton(ToolPane owner, ToolPaneTab tab, int x, int y) {
        super(x, y, WIDTH, HEIGHT, tab.getTitle().copy().withColor(CommonColors.DARK_GRAY).withoutShadow());
        this.owner = owner;
        this.tab = tab;
        if (tab.ordinal() == 0) {
            this.selectedSprites = SPRITES_LEFT;
        } else if (tab.ordinal() == ToolPaneTab.MAX_TAB_IDX) {
            this.selectedSprites = SPRITES_RIGHT;
        } else {
            this.selectedSprites = SPRITES_CENTER;
        }
    }

    @Override
    protected WidgetSprites getSprites() {
        return owner.getActiveTab() == tab ? selectedSprites : SPRITES_UNSELECTED;
    }

    @Override
    protected void extractDefaultLabel(ActiveTextCollector textCollector) {
        textCollector.accept(getX() + 5, getY() + 5, getMessage());
    }

    @Override
    public void onPress(InputWithModifiers input) {
        owner.setActiveTab(tab);
    }
}
