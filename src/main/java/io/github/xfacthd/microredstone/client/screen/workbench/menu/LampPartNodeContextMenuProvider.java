package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuUtils;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.util.Set;

public final class LampPartNodeContextMenuProvider extends PartNodeContextMenuProvider<LampPrototypeNode>
{
    public static final Component ENTRY_LAMP_COLOR = Utils.translate("label", "circuit_workbench.canvas.menu.node.lamp.color");
    private static final SubMenuKey COLOR_SUB_MENU = new SubMenuKey("wire_colors");
    private static final Set<DyeColor> EXCLUDED_COLORS = Set.of(DyeColor.BLACK);

    public LampPartNodeContextMenuProvider(CircuitCanvas canvas, LampPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addSubMenuEntry(ENTRY_LAMP_COLOR, COLOR_SUB_MENU);
        super.fillRootMenu(menuBuilder);
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey)
    {
        if (subMenuKey == COLOR_SUB_MENU)
        {
            LampPrototypeNode node = this.node.getChainRoot();
            ContextMenuUtils.makeDyeColorMenu(menuBuilder, node::setColor, node::getColor, EXCLUDED_COLORS);
        }
    }
}
