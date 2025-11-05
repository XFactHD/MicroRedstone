package io.github.xfacthd.microredstone.common.datagen.providers;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.data.LocalCircuitStorage;
import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.filebrowser.FileBrowserScreen;
import io.github.xfacthd.microredstone.client.screen.microchip.MicrochipCircuitScreen;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.KeyHint;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ImportExportHandler;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.BaseNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ClockPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConnectionNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConstantPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ConverterPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.LampPartNodeContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LogicGateList;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.PartsList;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolsTab;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.ImportEntryContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.WireToolActionContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.LibraryBrowser;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.FilterToggleButton;
import io.github.xfacthd.microredstone.client.util.ColorNames;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.block.CircuitWorkbenchBlock;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.compat.atlasviewer.AtlasViewerCompat;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.apache.commons.lang3.StringUtils;
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
        addWorkbenchScreenTranslations();
        addPartTranslations();
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
        add(MicrochipCircuitScreen.TITLE_WITH_CIRCUIT, "%s - %s");
        add(MicrochipCircuitScreen.TITLE_WITH_CIRCUIT_DEBUG, "%s - %s (%s)");
    }

    private void addWorkbenchScreenTranslations()
    {
        add(CircuitWorkbenchBlock.MENU_TITLE, "Circuit Workbench");
        add(CircuitWorkbenchScreen.TITLE_CONFIRM_CLOSE, "Confirm Close");
        add(CircuitWorkbenchScreen.MESSAGE_CONFIRM_CLOSE_LINE_ONE, "Are you sure you want to close the %s?");
        add(CircuitWorkbenchScreen.MESSAGE_CONFIRM_CLOSE_LINE_TWO, "All unsaved changes will be lost.");
        add(CircuitCanvas.CELL_COORD_TRANSLATION, "Cell: %s, %s");
        add(PartsList.LABEL_MODE, "Mode: ");
        add(PartsList.LABEL_FILTER, "Filter:");
        add(PartsList.MSG_DROP_TO_DELETE, "Drop here to delete");
        add(PartsList.Mode.GATES.getTitle(), "Gates");
        add(PartsList.Mode.LIBRARY.getTitle(), "Library");
        add(ToolPaneTab.PARTS.getTitle(), "Parts");
        add(ToolPaneTab.TOOLS.getTitle(), "Tools");
        add(ToolPaneTab.LIBRARY.getTitle(), "Library");
        add(LibraryBrowser.LABEL_MODE, "Mode:");
        add(LibraryBrowser.Mode.IMPORT.getTitle(), "Import");
        add(LibraryBrowser.Mode.EXPORT.getTitle(), "Export");
        add(LibraryBrowser.ImportAction.IMPORT_FROM_ITEM.getTitle(), "Import from Item");
        add(LibraryBrowser.ImportAction.IMPORT_FROM_JSON.getTitle(), "Import from Clipboard");
        add(LibraryBrowser.ExportAction.EXPORT_TO_ITEM.getTitle(), "Export to Item");
        add(LibraryBrowser.ExportAction.EXPORT_TO_LIBRARY.getTitle(), "Export to Library");
        add(LibraryBrowser.ExportAction.EXPORT_TO_JSON.getTitle(), "Export to Clipboard");
        add(LibraryBrowser.ExportAction.CLEAR_ERROR_ANNOTATIONS.getTitle(), "Clear Error Annotations");
        add(ImportExportHandler.TITLE_CONFIRM_IMPORT, "Confirm Import");
        add(ImportExportHandler.MESSAGE_CONFIRM_IMPORT_LINE_ONE, "Are you sure you want to overwrite the canvas with the imported circuit?");
        add(ImportExportHandler.MESSAGE_CONFIRM_IMPORT_LINE_TWO, "All unsaved changes will be lost.");
        add(ImportExportHandler.TITLE_IMPORT_BROKEN, "Import Incomplete");
        add(ImportExportHandler.MESSAGE_IMPORT_BROKEN, "The imported design may be incomplete due to issues in the circuit data or a bug in the importer.");
        add(ImportExportHandler.TITLE_CONFIRM_EXPORT, "Confirm Export");
        add(ImportExportHandler.MESSAGE_CONFIRM_EXPORT, "Are you sure you want to overwrite the existing circuit?");
        add(ImportExportHandler.TITLE_IMPORT_ERROR, "Import Failed");
        add(ImportExportHandler.TITLE_EXPORT_SUCCESS, "Export Successful");
        add(ImportExportHandler.TITLE_EXPORT_ERROR, "Export Failed");
        add(ImportExportHandler.ImportError.PARSE_FAILED.getDialogMessage(), "Failed to parse clipboard contents");
        add(ImportExportHandler.ImportError.DECODE_FAILED.getDialogMessage(), "Failed to decode clipboard contents");
        add(ImportExportHandler.ImportError.VALIDATE_FAILED.getDialogMessage(), "The decoded circuit is invalid");
        add(ImportExportHandler.ExportResult.SUCCESS.getDialogMessage(), "Circuit design exported successfully");
        add(ImportExportHandler.ExportResult.ITEM_SERVER_ERROR.getDialogMessage(), "An unknown error occured writing the design to the circuit item");
        add(ImportExportHandler.ExportResult.LIBRARY_SERVER_ERROR.getDialogMessage(), "An unknown error occured storing the design in the library");
        add(ImportExportHandler.ExportResult.JSON_ENCODE_FAILED.getDialogMessage(), "Failed to encode design to JSON");
        add(ShareType.PRIVATE.getTitle(), "Private");
        add(ShareType.SHARED.getTitle(), "Shared");
        add(ShareType.PUBLIC.getTitle(), "Public");
        add(ShareType.BUILTIN.getTitle(), "Built-In");
        add(FilterToggleButton.TOOLTIP_OFF, "%s Circuits: Hidden");
        add(FilterToggleButton.TOOLTIP_ON, "%s Circuits: Shown");
        add(ToolsTab.ToolAction.CREATE_SINGLE_WIRE.getTitle(), "Add Wire");
        add(ToolsTab.ToolAction.CREATE_BUNDLED_WIRE.getTitle(), "Add Bundled Wire");
        add(ToolsTab.ToolAction.LOAD_LOCAL.getTitle(), "Load Circuit Design");
        add(ToolsTab.ToolAction.SAVE_LOCAL.getTitle(), "Save Circuit Design");
        add(ToolsTab.ToolAction.CLEAR_CANVAS.getTitle(), "Clear Canvas");
        add(ToolsTab.TITLE_LOAD_FAILED, "Error loading circuit");
        add(ToolsTab.MESSAGE_LOAD_FAILED, "An error occured while loading the circuit design, this is likely a bug.");
        add(ToolsTab.TITLE_SAVE_FAILED, "Error saving circuit");
        add(ToolsTab.MESSAGE_SAVE_FAILED, "An error occured while saving the circuit design, this is likely a bug.");
        add(ToolsTab.TITLE_CONFIRM_CLEAR, "Confirm Clear Canvas");
        add(ToolsTab.MESSAGE_CONFIRM_CLEAR_LINE_ONE, "Are you sure you want to clear the canvas?");
        add(ToolsTab.MESSAGE_CONFIRM_CLEAR_LINE_TWO, "All unsaved changes will be lost.");
        add(WireToolActionContextMenuProvider.ENTRY_WIRE_COLOR, "Wire Color");
        add(WireToolActionContextMenuProvider.ENTRY_WIRE_ROUTE_PREF, "Long Section First");
        add(ImportEntryContextMenuProvider.ENTRY_IMPORT, "Import to Canvas");
        add(ImportEntryContextMenuProvider.ENTRY_DETAILS, "Show Details");
        add(ImportEntryContextMenuProvider.TITLE_DETAILS, "Circuit Details");
        add(ImportEntryContextMenuProvider.PROP_NAME, "Name:");
        add(ImportEntryContextMenuProvider.PROP_AUTHOR, "Author:");
        add(ImportEntryContextMenuProvider.PROP_TIME_CREATED, "Created:");
        add(ImportEntryContextMenuProvider.PROP_TIME_MODIFIED, "Modified:");
        add(ImportEntryContextMenuProvider.PROP_SHARE_TYPE, "Visibility:");
        add(BaseNodeContextMenuProvider.ENTRY_DELETE_NODE, "Delete Part");
        add(ConnectionNodeContextMenuProvider.ENTRY_PORT_DIR, "Port Direction");
        add(ConnectionNodeContextMenuProvider.ENTRY_NAME, "Set Name");
        add(ConnectionNodeContextMenuProvider.TITLE_SET_NAME, "Set Name");
        add(ConnectionNodeContextMenuProvider.LABEL_SET_NAME, "Name:");
        add(ConverterPartNodeContextMenuProvider.ENTRY_SET_BIT, "Set Bundle Bit");
        add(ConverterPartNodeContextMenuProvider.TITLE_SET_BIT, "Set Bundle Bit");
        add(ConverterPartNodeContextMenuProvider.LABEL_SET_BIT, "Bit:");
        add(ConverterPartNodeContextMenuProvider.TOOLTIP_INVALID_BIT, "Bit index must be between 0 and 15 (inclusive)");
        add(ClockPartNodeContextMenuProvider.ENTRY_SET_PERIOD, "Set Clock Period");
        add(ClockPartNodeContextMenuProvider.TITLE_SET_PERIOD, "Set Clock Period");
        add(ClockPartNodeContextMenuProvider.LABEL_SET_PERIOD, "Period:");
        add(ClockPartNodeContextMenuProvider.TOOLTIP_INVALID_PERIOD, "Period must be higher or equal to 2 and a multiple of 2");
        add(LampPartNodeContextMenuProvider.ENTRY_LAMP_COLOR, "Lamp Color:");
        add(ConstantPartNodeContextMenuProvider.ENTRY_SET_VALUE, "Set Constant Value");
        add(ConstantPartNodeContextMenuProvider.TITLE_SET_VALUE, "Set Constant Value");
        add(ConstantPartNodeContextMenuProvider.LABEL_SET_VALUE, "Value:");
        add(ConstantPartNodeContextMenuProvider.TOOLTIP_INVALID_VALUE_SINGLE, "Value must be binary 0 or 1");
        add(ConstantPartNodeContextMenuProvider.TOOLTIP_INVALID_VALUE_BUNDLED, "Value must be binary or hex between 0 and 65_535 (inclusive)");
        add(DialogScreen.Type.INFO.getDefaultTitle(), "Info");
        add(DialogScreen.Type.WARNING.getDefaultTitle(), "Warning");
        add(DialogScreen.Type.ERROR.getDefaultTitle(), "Error");
        add(DialogScreen.Type.CONFIRM.getDefaultTitle(), "Confirm");
        add(DialogScreen.Type.PROPERTIES.getDefaultTitle(), "Properties");
        add(DialogScreen.Type.QUERY.getDefaultTitle(), "Set Value");
        add(FileBrowserScreen.Type.OPEN.getTitle(), "Open %s");
        add(FileBrowserScreen.Type.SAVE.getTitle(), "Save %s");
        add(FileBrowserScreen.Type.OPEN.getButtonTitle(), "Open");
        add(FileBrowserScreen.Type.SAVE.getButtonTitle(), "Save");
        add(FileBrowserScreen.MSG_DIR_EMPTY, "This directory is empty.");
        add(FileBrowserScreen.MSG_DIR_NO_MATCHING_FILES, "This directory contains no matching files.");
        add(FileBrowserScreen.LABEL_FILE_NAME, "File Name:");
        add(FileBrowserScreen.LABEL_FILE_TYPE, "File Type:");
        add(FileBrowserScreen.VALUE_FILE_TYPE_ALL, "[All Files]");
        add(FileBrowserScreen.MSG_FILE_NOT_FOUND_LINE_ONE, "File could not be found.");
        add(FileBrowserScreen.MSG_FILE_NOT_FOUND_LINE_TWO, "Check the file name and try again.");
        add(FileBrowserScreen.TITLE_SAVE_CONFIRM_OVERWRITE, "Confirm Save %s");
        add(FileBrowserScreen.MSG_SAVE_CONFIRM_OVERWRITE_LINE_ONE, "%s already exists.");
        add(FileBrowserScreen.MSG_SAVE_CONFIRM_OVERWRITE_LINE_TWO, "Are you sure you want to overwrite it?");
        add(KeyHint.WRAPPER, "[%s]");
        add(LocalCircuitStorage.FILE_TYPE_DESCRIPTION, "Circuit Design");
    }

    private void addPartTranslations()
    {
        addPart("connection_single", "Connector", "Single", "");
        addPart("connection_bundled", "Connector", "Bundled", "");
        addPart("constant_single", "Constant Value", "Single", "");
        addPart("constant_bundled", "Constant Value", "Bundled", "");
        addPart("clock", "Clock", null, "");
        addPart("lamp", "Lamp", null, "");
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
        for (DyeColor color : DyeColor.values())
        {
            StringBuilder name = new StringBuilder();
            for (String part : color.getName().split("_"))
            {
                name.append(StringUtils.capitalize(part)).append(" ");
            }
            add(ColorNames.getName(color), name.toString().trim());
        }

        add(PortDir.INPUT.getTitle(), "Input");
        add(PortDir.OUTPUT.getTitle(), "Output");

        add(AtlasViewerCompat.LABEL_MASK_TEXTURE, "Texture");
        add(AtlasViewerCompat.LABEL_MASK_SPRITE, "Sprite");
        add(AtlasViewerCompat.LABEL_MASK_AREA, "Area");
        add(AtlasViewerCompat.VALUE_MASK_AREA, "X: %s Y: %s Width: %s Height: %s");
        add(AtlasViewerCompat.LABEL_PORT_SPRITE, "Sprite");
        add(AtlasViewerCompat.LABEL_PORT_ENTRIES, "Ports");
        add(AtlasViewerCompat.VALUE_PORT_ENTRY, "  - %s = %s");
        add(AtlasViewerCompat.LABEL_PORT_TYPE_PREFIX, "Type Prefix");
        add(AtlasViewerCompat.LABEL_STACKING_SPRITE, "Sprite");
        add(AtlasViewerCompat.LABEL_STACKING_PRIMARY, "Primary Texture");
        add(AtlasViewerCompat.LABEL_STACKING_SECONDARIES, "Secondary Textures");
        add(AtlasViewerCompat.VALUE_STACKING_SECONDARY, "  - %s");
    }

    // TODO: add descriptions
    private void addPart(String partName, String title, @Nullable String subTitle, String description)
    {
        LogicGateList.Entry entry = LogicGateList.getEntryByName(partName);
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
