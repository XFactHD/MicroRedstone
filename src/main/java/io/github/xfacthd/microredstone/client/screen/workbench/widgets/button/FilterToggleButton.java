package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.widgets.button.SimpleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.WorkbenchConfig;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public final class FilterToggleButton extends SimpleButton implements DropFocusAfterClick, ActionButton<ShareType>
{
    public static final int SIZE = 16;
    private static final int ICON_OFFSET = 2;
    private static final int ICON_SIZE = SIZE - ICON_OFFSET * 2;
    private static final WidgetSprites SPRITES_OFF = sprites(Utils.rl("button/toggle_button_off"));
    private static final WidgetSprites SPRITES_ON = sprites(Utils.rl("button/toggle_button_on"));
    public static final String TOOLTIP_OFF = Utils.translationKey("tooltip", "circuit_workbench.parts_list.filter.hidden");
    public static final String TOOLTIP_ON = Utils.translationKey("tooltip", "circuit_workbench.parts_list.filter.shown");

    private final PartsList owner;
    private final ShareType filter;

    public FilterToggleButton(PartsList owner, ShareType filter)
    {
        super(0, 0, SIZE, SIZE, filter.getTitle());
        this.owner = owner;
        this.filter = filter;
        updateTooltip();
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        int iconX = getX() + ICON_OFFSET;
        int iconY = getY() + ICON_OFFSET;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, filter.getIcon(), iconX, iconY, ICON_SIZE, ICON_SIZE);
    }

    private boolean isEnabled()
    {
        return WorkbenchConfig.INSTANCE.isImportFilterEnabled(filter);
    }

    @Override
    protected WidgetSprites getSprites()
    {
        return isEnabled() ? SPRITES_ON : SPRITES_OFF;
    }

    @Override
    public void onPress(InputWithModifiers input)
    {
        owner.setFilter(filter, Minecraft.getInstance().hasShiftDown());
    }

    public void updateTooltip()
    {
        String key = isEnabled() ? TOOLTIP_ON : TOOLTIP_OFF;
        setTooltip(Tooltip.create(Component.translatable(key, filter.getTitle())));
    }

    @Override
    public ShareType getAction()
    {
        return filter;
    }
}
