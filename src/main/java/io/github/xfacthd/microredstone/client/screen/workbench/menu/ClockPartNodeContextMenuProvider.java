package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

public final class ClockPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ClockPrototypeNode>
{
    public static final Component ENTRY_SET_PERIOD = Utils.translate("label", "circuit_workbench.canvas.menu.node.clock.set_period");

    public ClockPartNodeContextMenuProvider(CircuitCanvas canvas, ClockPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_SET_PERIOD, this::openPeriodConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openPeriodConfigDialog()
    {
        // TODO: implement period config dialog
    }
}
