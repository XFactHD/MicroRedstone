package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public interface QueryWidget {
    Component label();

    void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder);

    boolean isInputValid();

    void saveQueryResult();
}
