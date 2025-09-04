package io.github.xfacthd.microredstone.client.screen.dialog;

import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class DialogScreenBuilder
{
    private final DialogScreen.Type type;
    private Component title;
    private final List<Component> messageLines = new ArrayList<>();
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
        return new DialogScreen(type, title, messageLines, okCallback, cancelCallback);
    }

    public void show()
    {
        Minecraft.getInstance().pushGuiLayer(build());
    }
}
