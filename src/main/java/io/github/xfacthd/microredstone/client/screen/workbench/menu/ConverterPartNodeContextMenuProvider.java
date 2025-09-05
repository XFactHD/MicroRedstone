package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

public final class ConverterPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ConverterPrototypeNode>
{
    public static final Component ENTRY_SET_BIT = Utils.translate("label", "circuit_workbench.canvas.menu.node.converter.set_bit");

    public ConverterPartNodeContextMenuProvider(CircuitCanvas canvas, ConverterPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_SET_BIT, this::openBitConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openBitConfigDialog()
    {
        // TODO: implement bit config dialog
    }
}
