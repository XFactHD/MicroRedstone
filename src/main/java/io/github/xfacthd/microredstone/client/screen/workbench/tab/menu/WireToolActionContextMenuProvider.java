package io.github.xfacthd.microredstone.client.screen.workbench.tab.menu;

import io.github.xfacthd.microredstone.client.screen.workbench.WorkbenchConfig;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.util.ColorNames;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

public final class WireToolActionContextMenuProvider implements ContextMenuProvider
{
    public static final Component ENTRY_WIRE_COLOR = Utils.translate("label", "circuit_workbench.tools_tab.menu.wire.wire_color");
    public static final Component ENTRY_WIRE_ROUTE_PREF = Utils.translate("label", "circuit_workbench.tools_tab.menu.wire.wire_route_pref");
    private static final DyeColor[] COLORS = DyeColor.values();
    private static final SubMenuKey COLOR_SUB_MENU = new SubMenuKey("wire_colors");
    public static final ContextMenuProvider INSTANCE_SINGLE = new WireToolActionContextMenuProvider(true);
    public static final ContextMenuProvider INSTANCE_BUNDLED = new WireToolActionContextMenuProvider(false);

    private final boolean withWireColors;

    private WireToolActionContextMenuProvider(boolean withWireColors)
    {
        this.withWireColors = withWireColors;
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        if (withWireColors)
        {
            menuBuilder.addSubMenuEntry(ENTRY_WIRE_COLOR, COLOR_SUB_MENU)
                    .addSeparator();
        }

        WorkbenchConfig config = WorkbenchConfig.INSTANCE;
        menuBuilder.addActionEntry(
                ENTRY_WIRE_ROUTE_PREF,
                config::toggleRouteLongWireSectionFirst,
                config::isRouteLongWireSectionFirst
        );
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey)
    {
        if (subMenuKey == COLOR_SUB_MENU)
        {
            WorkbenchConfig config = WorkbenchConfig.INSTANCE;
            for (DyeColor color : COLORS)
            {
                menuBuilder.addActionEntry(
                        ColorNames.getName(color),
                        () -> config.setWireColor(color),
                        () -> config.getWireColor() == color
                );
            }
        }
    }
}
