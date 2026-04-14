package io.github.xfacthd.microredstone.client.screen.widgets.button;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.DropFocusAfterClick;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public final class BasicButton extends SimpleButton implements DropFocusAfterClick {
    private final Runnable onPress;

    public BasicButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    protected WidgetSprites getSprites() {
        return SPRITES;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.run();
    }
}
