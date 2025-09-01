package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolsTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.FilterToggleButton;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.block.CircuitWorkbenchBlock;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.compat.atlasviewer.AtlasViewerCompat;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.Nullable;

public final class MRLanguageProvider extends LanguageProvider
{
    public MRLanguageProvider(PackOutput output)
    {
        super(output, MicroRedstone.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations()
    {
        addBlockTranslations();
        addItemTranslations();
        addScreenTranslations();
        addSpecialTranslations();
    }

    private void addBlockTranslations()
    {
        add(MRContent.BLOCK_MICROCHIP.value(), "Microchip");
    }

    private void addItemTranslations()
    {
        add(MRContent.ITEM_INTEGRATED_CIRCUIT.value(), "Integrated Circuit");

        add(StoredCircuit.LABEL_CIRCUIT, "Circuit: %s");
    }

    private void addScreenTranslations()
    {
        add(MicrochipBlockEntity.MENU_TITLE, "Microchip");

        add(CircuitWorkbenchBlock.MENU_TITLE, "Circuit Workbench");
        add(CircuitCanvas.CELL_COORD_TRANSLATION, "Cell: %s, %s");
        add(ToolPaneTab.PARTS.getTitle(), "Parts");
        add(ToolPaneTab.TOOLS.getTitle(), "Tools");
        add(ToolPaneTab.LIBRARY.getTitle(), "Library");
        add(LibraryBrowser.LABEL_MODE, "Mode:");
        add(LibraryBrowser.LABEL_FILTER, "Filter:");
        add(LibraryBrowser.Mode.IMPORT.getTitle(), "Import");
        add(LibraryBrowser.Mode.EXPORT.getTitle(), "Export");
        add(LibraryBrowser.ExportAction.EXPORT_TO_LIBRARY.getTitle(), "Export to Library");
        add(LibraryBrowser.ExportAction.EXPORT_TO_ITEM.getTitle(), "Export to Item");
        add(LibraryBrowser.ExportAction.CLEAR_ERROR_ANNOTATIONS.getTitle(), "Clear Error Annotations");
        add(ShareType.PRIVATE.getTitle(), "Private");
        add(ShareType.SHARED.getTitle(), "Shared");
        add(ShareType.PUBLIC.getTitle(), "Public");
        add(ShareType.BUILTIN.getTitle(), "Built-In");
        add(FilterToggleButton.TOOLTIP_OFF, "%s Circuits: Hidden");
        add(FilterToggleButton.TOOLTIP_ON, "%s Circuits: Shown");
        add(ToolsTab.ToolAction.CREATE_SINGLE_WIRE.getTitle(), "Add Wire");
        add(ToolsTab.ToolAction.CREATE_BUNDLED_WIRE.getTitle(), "Add Bundled Wire");
        addPart("connection_single", "Connector", "Single", "");
        addPart("connection_bundled", "Connector", "Bundled", "");
        addPart("clock", "Clock", null, "");
        addPart("buffer_single", "Buffer", "Single", "");
        addPart("buffer_bundled", "Buffer", "Bundled", "");
        addPart("not_single", "NOT", "Single", "");
        addPart("and_two_single", "AND", "2-Input, Single", "");
        addPart("and_three_single", "AND", "3-Input, Single", "");
        addPart("or_two_single", "OR", "2-Input, Single", "");
        addPart("or_three_single", "OR", "3-Input, Single", "");
        addPart("xor_two_single", "XOR", "2-Input, Single", "");
        addPart("xor_three_single", "XOR", "3-Input, Single", "");
        addPart("nand_two_single", "NAND", "2-Input, Single", "");
        addPart("nand_three_single", "NAND", "3-Input, Single", "");
        addPart("nor_two_single", "NOR", "2-Input, Single", "");
        addPart("nor_three_single", "NOR", "3-Input, Single", "");
        addPart("xnor_two_single", "XNOR", "2-Input, Single", "");
        addPart("xnor_three_single", "XNOR", "3-Input, Single", "");
        addPart("not_bundled", "NOT", "Bundled", "");
        addPart("and_two_bundled", "AND", "2-Input, Bundled", "");
        addPart("and_three_bundled", "AND", "3-Input, Bundled", "");
        addPart("or_two_bundled", "OR", "2-Input, Bundled", "");
        addPart("or_three_bundled", "OR", "3-Input, Bundled", "");
        addPart("xor_two_bundled", "XOR", "2-Input, Bundled", "");
        addPart("xor_three_bundled", "XOR", "3-Input, Bundled", "");
        addPart("nand_two_bundled", "NAND", "2-Input, Bundled", "");
        addPart("nand_three_bundled", "NAND", "3-Input, Bundled", "");
        addPart("nor_two_bundled", "NOR", "2-Input, Bundled", "");
        addPart("nor_three_bundled", "NOR", "3-Input, Bundled", "");
        addPart("xnor_two_bundled", "XNOR", "2-Input, Bundled", "");
        addPart("xnor_three_bundled", "XNOR", "3-Input, Bundled", "");
        addPart("packer", "Bundle Packer", null, "");
        addPart("unpacker", "Bundle Unpacker", null, "");
    }

    private void addSpecialTranslations()
    {
        add(AtlasViewerCompat.LABEL_MASK_TEXTURE, "Texture");
        add(AtlasViewerCompat.LABEL_MASK_SPRITE, "Sprite");
        add(AtlasViewerCompat.LABEL_MASK_AREA, "Area");
        add(AtlasViewerCompat.VALUE_MASK_AREA, "X: %s Y: %s Width: %s Height: %s");
        add(AtlasViewerCompat.LABEL_PORT_SPRITE, "Sprite");
        add(AtlasViewerCompat.LABEL_PORT_ENTRIES, "Ports");
        add(AtlasViewerCompat.VALUE_PORT_ENTRY, "  - %s = %s");
        add(AtlasViewerCompat.LABEL_PORT_TYPE_PREFIX, "Type Prefix");
    }

    // TODO: add descriptions
    private void addPart(String partName, String title, @Nullable String subTitle, String description)
    {
        PartsList.Entry entry = PartsList.getEntryByName(partName);
        add(entry.title(), title);
        if (entry.subTitle() != null && subTitle != null)
        {
            add(entry.subTitle(), subTitle);
        }
        add(entry.description(), description);
    }

    private void add(Component key, String value)
    {
        ComponentContents contents = key.getContents();
        if (contents instanceof TranslatableContents translatable)
        {
            add(translatable.getKey(), value);
        }
        else
        {
            add(key.getString(), value);
        }
    }
}
