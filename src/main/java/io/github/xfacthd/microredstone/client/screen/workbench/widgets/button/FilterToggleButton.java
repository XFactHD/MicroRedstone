package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public final class FilterToggleButton extends SimpleButton implements DropFocusAfterClick, ActionButton<ShareType>
{
    public static final int SIZE = 16;
    private static final int ICON_OFFSET = 2;
    private static final int ICON_SIZE = SIZE - ICON_OFFSET * 2;
    private static final WidgetSprites SPRITES_OFF = sprites(Utils.rl("button/toggle_button_off"));
    private static final WidgetSprites SPRITES_ON = sprites(Utils.rl("button/toggle_button_on"));
    public static final String TOOLTIP_OFF = Utils.translationKey("tooltip", "circuit_workbench.library_browser.filter.hidden");
    public static final String TOOLTIP_ON = Utils.translationKey("tooltip", "circuit_workbench.library_browser.filter.shown");

    private final LibraryBrowser owner;
    private final ShareType filter;

    public FilterToggleButton(LibraryBrowser owner, ShareType filter)
    {
        super(0, 0, SIZE, SIZE, filter.getTitle());
        this.owner = owner;
        this.filter = filter;
        updateTooltip();
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int iconX = getX() + ICON_OFFSET;
        int iconY = getY() + ICON_OFFSET;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, filter.getIcon(), iconX, iconY, ICON_SIZE, ICON_SIZE);
    }

    @Override
    protected WidgetSprites getSprites()
    {
        return owner.isFilterEnabled(filter) ? SPRITES_ON : SPRITES_OFF;
    }

    @Override
    public void onPress()
    {
        owner.setFilter(filter, Screen.hasShiftDown());
    }

    public void updateTooltip()
    {
        String key = owner.isFilterEnabled(filter) ? TOOLTIP_ON : TOOLTIP_OFF;
        setTooltip(Tooltip.create(Component.translatable(key, filter.getTitle())));
    }

    @Override
    public ShareType getAction()
    {
        return filter;
    }
}
