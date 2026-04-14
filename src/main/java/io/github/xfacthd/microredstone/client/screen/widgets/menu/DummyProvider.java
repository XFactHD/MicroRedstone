package io.github.xfacthd.microredstone.client.screen.widgets.menu;

final class DummyProvider implements ContextMenuProvider {
    static final DummyProvider INSTANCE = new DummyProvider();

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder) { }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey) { }
}
