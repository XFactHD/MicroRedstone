package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

public abstract sealed class BaseNodeContextMenuProvider<T extends PlaceableNode> implements ContextMenuProvider
        permits ConnectionNodeContextMenuProvider, PartNodeContextMenuProvider
{
    public static final Component ENTRY_DELETE_NODE = Utils.translate("label", "circuit_workbench.canvas.menu.node.delete");

    protected final CircuitCanvas canvas;
    protected final T node;

    protected BaseNodeContextMenuProvider(CircuitCanvas canvas, T node)
    {
        this.canvas = canvas;
        this.node = node;
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        if (!menuBuilder.isEmpty())
        {
            menuBuilder.addSeparator();
        }
        menuBuilder.addActionEntry(ENTRY_DELETE_NODE, () -> canvas.getPartGrid().removePartNode(node.getPos()));
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey) { }
}
