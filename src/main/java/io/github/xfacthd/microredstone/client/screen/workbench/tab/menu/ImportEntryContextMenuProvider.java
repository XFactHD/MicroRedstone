package io.github.xfacthd.microredstone.client.screen.workbench.tab.menu;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

public final class ImportEntryContextMenuProvider implements ContextMenuProvider
{
    public static final Component ENTRY_IMPORT = Utils.translate("label", "circuit_workbench.library_browser.import_entry.menu.import");
    public static final Component ENTRY_DETAILS = Utils.translate("label", "circuit_workbench.library_browser.import_entry.menu.details");

    private final CircuitWorkbenchScreen owner;
    private final PartsList.LibraryEntry entry;

    public ImportEntryContextMenuProvider(CircuitWorkbenchScreen owner, PartsList.LibraryEntry entry)
    {
        this.owner = owner;
        this.entry = entry;
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_IMPORT, () -> owner.getImportExportHandler().importCircuit(entry.name(), entry.node()))
                .addActionEntry(ENTRY_DETAILS, () -> { /* TODO: pull details from library */ });
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey) { }
}
