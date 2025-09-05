package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

public final class ConnectionNodeContextMenuProvider extends BaseNodeContextMenuProvider<Connection>
{
    public static final Component ENTRY_PORT_DIR = Utils.translate("label", "circuit_workbench.canvas.menu.node.connection.port_dir");
    private static final SubMenuKey DIR_SUB_MENU = new SubMenuKey("port_direction");
    private static final PortDir[] PORT_DIRS = PortDir.values();

    public ConnectionNodeContextMenuProvider(CircuitCanvas canvas, Connection node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addSubMenuEntry(ENTRY_PORT_DIR, DIR_SUB_MENU);
        super.fillRootMenu(menuBuilder);
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey)
    {
        if (subMenuKey == DIR_SUB_MENU)
        {
            for (PortDir dir : PORT_DIRS)
            {
                menuBuilder.addActionEntry(dir.getTitle(), entry -> entry
                        .withAction(() -> node.setPortDir(dir))
                        .withStateSupplier(() -> node.getPortDir() == dir)
                );
            }
        }
    }
}
