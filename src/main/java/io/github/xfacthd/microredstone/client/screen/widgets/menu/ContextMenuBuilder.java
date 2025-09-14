package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public interface ContextMenuBuilder
{
    ContextMenuBuilder addActionEntry(Component text, Runnable action);

    ContextMenuBuilder addActionEntry(Component text, Consumer<ActionEntryBuilder> consumer);

    ContextMenuBuilder addSubMenuEntry(Component text, SubMenuKey subMenuKey);

    ContextMenuBuilder addSeparator();

    boolean isEmpty();

    interface ActionEntryBuilder
    {
        ActionEntryBuilder withAction(Runnable action);

        ActionEntryBuilder withKeyHint(KeyHint keyHint);

        ActionEntryBuilder withStateSupplier(BooleanSupplier stateSupplier);
    }
}
