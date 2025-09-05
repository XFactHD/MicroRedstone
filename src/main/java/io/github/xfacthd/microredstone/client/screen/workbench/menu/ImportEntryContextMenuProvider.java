package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.data.library.ClientCircuitLibrary;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class ImportEntryContextMenuProvider implements ContextMenuProvider
{
    public static final Component ENTRY_IMPORT = Utils.translate("label", "circuit_workbench.library_browser.import_entry.menu.import");
    public static final Component ENTRY_DETAILS = Utils.translate("label", "circuit_workbench.library_browser.import_entry.menu.details");
    public static final Component TITLE_DETAILS = Utils.translate("title", "circuit_workbench.library_browser.import_entry.details");
    public static final Component PROP_NAME = Utils.translate("label", "circuit_workbench.library_browser.import_entry.details.name");
    public static final Component PROP_AUTHOR = Utils.translate("label", "circuit_workbench.library_browser.import_entry.details.author");
    public static final Component PROP_TIME_CREATED = Utils.translate("label", "circuit_workbench.library_browser.import_entry.details.time_created");
    public static final Component PROP_TIME_MODIFIED = Utils.translate("label", "circuit_workbench.library_browser.import_entry.details.time_modified");
    public static final Component PROP_SHARE_TYPE = Utils.translate("label", "circuit_workbench.library_browser.import_entry.details.share_type");
    private static final int EXPECTED_MAX_NAME_WIDTH = 90;

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
                .addActionEntry(ENTRY_DETAILS, this::makeDetailsDialog);
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey) { }

    private void makeDetailsDialog()
    {
        CircuitLibraryEntry libEntry = ClientCircuitLibrary.getEntryById(entry.id());
        Supplier<Component> author = Utils.resolvePlayerName(libEntry.author(), Minecraft.getInstance());
        DialogScreen.builder(DialogScreen.Type.PROPERTIES)
                .withTitle(TITLE_DETAILS)
                .withProperty(PROP_NAME, entry.title())
                .withProperty(PROP_AUTHOR, author, EXPECTED_MAX_NAME_WIDTH)
                .withProperty(PROP_TIME_CREATED, Utils.formatInstant(libEntry.timeCreated()))
                .withProperty(PROP_TIME_MODIFIED, Utils.formatInstant(libEntry.timeModified()))
                .withProperty(PROP_SHARE_TYPE, libEntry.shareInfo().type().getTitle())
                .show();
    }
}
