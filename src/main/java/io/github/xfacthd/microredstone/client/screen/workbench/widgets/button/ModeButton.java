package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.widgets.button.SimpleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.MultiModeTab;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;

public final class ModeButton<E extends MultiModeTab.Mode> extends SimpleButton implements DropFocusAfterClick
{
    public static final int WIDTH = 52;
    private static final int HEIGHT = 16;
    private static final WidgetSprites SPRITES_UNSELECTED = sprites(Utils.rl("button/mode_button"));
    private static final WidgetSprites SPRITES_SELECTED = sprites(Utils.rl("button/mode_button_selected"));

    private final MultiModeTab<E> owner;
    private final E mode;

    public ModeButton(MultiModeTab<E> owner, E mode, int x, int y)
    {
        super(x, y, WIDTH, HEIGHT, mode.getTitle());
        this.owner = owner;
        this.mode = mode;
    }

    @Override
    protected WidgetSprites getSprites()
    {
        return owner.getMode() == mode ? SPRITES_SELECTED : SPRITES_UNSELECTED;
    }

    @Override
    public void onPress(InputWithModifiers input)
    {
        owner.setMode(mode);
    }
}
