package io.github.xfacthd.microredstone.client.screen.dialog;

import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DialogScreenBuilder
{
    private final DialogScreen.Type type;
    private Component title;
    private final List<Component> messageLines = new ArrayList<>();
    private final List<Property> properties = new ArrayList<>();
    private final List<QueryWidget> queryWidgets = new ArrayList<>();
    private Runnable okCallback = () -> {};
    private Runnable cancelCallback = () -> {};

    DialogScreenBuilder(DialogScreen.Type type)
    {
        this.type = type;
        this.title = type.defaultTitle;
    }

    public DialogScreenBuilder withTitle(Component title)
    {
        this.title = title;
        return this;
    }

    public DialogScreenBuilder withMessage(Component message)
    {
        messageLines.add(message);
        return this;
    }

    public DialogScreenBuilder withMessages(List<Component> messages)
    {
        messageLines.addAll(messages);
        return this;
    }

    public DialogScreenBuilder withProperty(Component label, Component value)
    {
        Preconditions.checkState(type == DialogScreen.Type.PROPERTIES);
        properties.add(new Property(label, new Property.ImmediateValue(value)));
        return this;
    }

    public DialogScreenBuilder withProperty(Component label, Supplier<@Nullable Component> delayedValue, int expectedMaxValueWidth)
    {
        Preconditions.checkState(type == DialogScreen.Type.PROPERTIES);
        properties.add(new Property(label, new Property.DelayedValue(delayedValue, expectedMaxValueWidth)));
        return this;
    }

    public DialogScreenBuilder withQueryWidget(QueryWidget queryWidget)
    {
        Preconditions.checkState(type == DialogScreen.Type.QUERY);
        this.queryWidgets.add(queryWidget);
        return this;
    }

    public DialogScreenBuilder withOkCallback(Runnable callback)
    {
        this.okCallback = callback;
        return this;
    }

    public DialogScreenBuilder withCancelCallback(Runnable callback)
    {
        Preconditions.checkState(type.hasCancel);
        this.cancelCallback = callback;
        return this;
    }

    public DialogScreen build()
    {
        return switch (type)
        {
            case PROPERTIES -> new PropertiesDialogScreen(title, messageLines, properties, okCallback);
            case QUERY -> new QueryDialogScreen(title, messageLines, queryWidgets, okCallback, cancelCallback);
            default -> new DialogScreen(type, title, messageLines, okCallback, cancelCallback);
        };
    }

    public void show()
    {
        Minecraft.getInstance().pushGuiLayer(build());
    }
}
