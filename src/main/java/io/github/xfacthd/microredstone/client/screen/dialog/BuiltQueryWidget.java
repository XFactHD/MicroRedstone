package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

record BuiltQueryWidget(Component label, int labelY, List<AbstractWidget> widgets)
{
    BuiltQueryWidget offsetLabelY(int topPos)
    {
        return new BuiltQueryWidget(label, labelY + topPos, widgets);
    }
}
