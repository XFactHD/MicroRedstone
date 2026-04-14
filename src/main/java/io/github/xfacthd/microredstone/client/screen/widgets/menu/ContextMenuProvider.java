package io.github.xfacthd.microredstone.client.screen.widgets.menu;

public interface ContextMenuProvider {
    void fillRootMenu(ContextMenuBuilder menuBuilder);

    void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey);
}
