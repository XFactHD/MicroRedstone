package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

@SuppressWarnings("UnusedReturnValue")
public interface ContextMenuBuilder
{
    ContextMenuBuilder addActionEntry(Component text, Runnable action);

    ContextMenuBuilder addActionEntry(Component text, KeyHint keyHint, Runnable action);

    ContextMenuBuilder addActionEntry(Component text, Runnable action, BooleanSupplier stateSupplier);

    ContextMenuBuilder addActionEntry(Component text, KeyHint keyHint, Runnable action, BooleanSupplier stateSupplier);

    ContextMenuBuilder addSubMenuEntry(Component text, SubMenuKey subMenuKey);

    ContextMenuBuilder addSeparator();
}
