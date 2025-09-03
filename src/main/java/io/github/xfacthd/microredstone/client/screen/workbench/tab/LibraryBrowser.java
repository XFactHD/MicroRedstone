package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.DragStart;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ExportActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.FilterToggleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.LibraryModeButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.NodeListWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.util.ScreenUtils;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class LibraryBrowser extends ToolPaneTabWidget
{
    public static final Component LABEL_MODE = Utils.translate("label", "circuit_workbench.library_browser.mode");
    public static final Component LABEL_FILTER = Utils.translate("label", "circuit_workbench.library_browser.filter");
    private static final ResourceLocation INVENTORY = Utils.rl("workbench_inventory");
    private static final ResourceLocation SLOT = Utils.rl("minecraft", "container/slot");
    private static final ShareType[] SHARE_TYPES = ShareType.values();
    private static final int INVENTORY_WIDTH = 172;
    private static final int INVENTORY_HEIGHT = 94;
    private static final int LABEL_X = 5;
    private static final int MODE_LABEL_Y = 10;
    private static final int FILTER_LABEL_Y = 28;
    private static final int MODE_BTN_IMPORT_X = 35;
    private static final int MODE_BTN_EXPORT_X = MODE_BTN_IMPORT_X + LibraryModeButton.WIDTH;
    private static final int MODE_BTN_Y = MODE_LABEL_Y - 4;
    private static final int FILTER_BTN_X = 35;
    private static final int FILTER_BTN_Y = FILTER_LABEL_Y - 4;
    private static final int EXPORT_NAME_EDIT_X = 5;
    private static final int EXPORT_NAME_EDIT_Y = 26;
    private static final int EXPORT_NAME_EDIT_WIDTH = TOOL_PANE_WIDTH - 10;
    private static final int EXPORT_NAME_EDIT_HEIGHT = 22;
    private static final int EXPORT_SLOT_X = 5;
    private static final int EXPORT_SLOT_Y = 50;
    private static final int EXPORT_SLOT_SIZE = 18;
    private static final int EXPORT_BTN_X = 5;
    private static final int EXPORT_BTN_Y = 72;
    private static final int EXPORT_BTN_PADDING = 2;

    private final LibraryModeButton modeBtnImport;
    private final LibraryModeButton modeBtnExport;
    private final List<FilterToggleButton> filterButtons;
    private final List<ExportActionButton> actionButtons;
    private final EditBox exportNameEditBox;
    private final List<Entry> importEntries;
    private final NodeListWidget importListWidget;
    private final EnumSet<ShareType> filters = EnumSet.allOf(ShareType.class);
    private final ItemStack dummyCircuit = MRContent.ITEM_INTEGRATED_CIRCUIT.value().getDefaultInstance();
    private int invX;
    private int invY;
    private int exportSlotX;
    private int exportSlotY;
    private Mode mode = Mode.IMPORT;
    private boolean wasImport = true;
    private boolean wasExport = true;

    public LibraryBrowser(CircuitWorkbenchScreen owner)
    {
        super(owner);
        this.modeBtnImport = new LibraryModeButton(this, Mode.IMPORT, 0, 0);
        this.modeBtnExport = new LibraryModeButton(this, Mode.EXPORT, 0, 0);
        this.filterButtons = makeActionButtons(this, SHARE_TYPES, FilterToggleButton::new);
        this.actionButtons = makeActionButtons(this, ExportAction.ACTIONS, ExportActionButton::new);
        this.exportNameEditBox = new EditBox(Minecraft.getInstance().font, EXPORT_NAME_EDIT_WIDTH, EXPORT_NAME_EDIT_HEIGHT, Component.empty());
        this.importEntries = new ArrayList<>();
        this.importListWidget = new NodeListWidget(owner, importEntries::size, importEntries::get);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(owner.getFont(), LABEL_MODE, paneX + LABEL_X, paneY + MODE_LABEL_Y, 0xFF404040, false);

        if (isInExportMode())
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVENTORY, invX, invY, INVENTORY_WIDTH, INVENTORY_HEIGHT);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, exportSlotX, exportSlotY, EXPORT_SLOT_SIZE, EXPORT_SLOT_SIZE);
            if (!owner.getMenu().getCircuitSlot().hasItem())
            {
                ScreenUtils.renderTransparentFakeItem(graphics, dummyCircuit, exportSlotX + 1, exportSlotY + 1);
            }
        }
        else
        {
            graphics.drawString(owner.getFont(), LABEL_FILTER, paneX + LABEL_X, paneY + FILTER_LABEL_Y, 0xFF404040, false);

            importListWidget.render(graphics, mouseX, mouseY);
        }

        actionButtons.forEach((button) -> button.active = button.getAction().isActive(this));
    }

    public boolean isMouseOverImportList(double mouseX, double mouseY)
    {
        return mode == Mode.IMPORT && importListWidget.isMouseOverList(mouseX, mouseY);
    }

    @Nullable
    public DragStart getClickedEntryIdx(double mouseY)
    {
        return importListWidget.getClickedEntryIdx(mouseY, DragStart::library);
    }

    @Nullable
    public PlaceableNode createNode(int slot)
    {
        return slot >= 0 && slot < importEntries.size() ? importEntries.get(slot).instantiate() : null;
    }

    public boolean isInExportMode()
    {
        return mode == Mode.EXPORT;
    }

    public void setMode(Mode mode)
    {
        if (this.mode != mode)
        {
            this.mode = mode;
            updateWidgetVisibility(true);
        }
    }

    public void setFilter(ShareType filter, boolean exclusive)
    {
        boolean changed = false;
        if (exclusive)
        {
            if (!filters.contains(filter) || filters.size() > 1)
            {
                filters.clear();
                filters.add(filter);
                changed = true;
            }
        }
        else if (filters.contains(filter))
        {
            filters.remove(filter);
            changed = true;
        }
        else
        {
            filters.add(filter);
            changed = true;
        }
        if (changed)
        {
            filterButtons.forEach(FilterToggleButton::updateTooltip);
            // TODO: implement library backend
            //ClientLibraryManager.collectFiltered(importEntries, filters);
        }
    }

    public boolean isFilterEnabled(ShareType filter)
    {
        return filters.contains(filter);
    }

    public boolean isCoveredByInventory(double mouseX, double mouseY)
    {
        return owner.getToolPaneTab() == getType() && mode == Mode.EXPORT && mouseX >= invX && mouseY >= invY;
    }

    public Mode getMode()
    {
        return mode;
    }

    @Override
    public ScrollableWidget getScrollableWidget()
    {
        return importListWidget;
    }

    @Override
    public void init(Consumer<AbstractWidget> widgetAdder)
    {
        widgetAdder.accept(modeBtnImport);
        widgetAdder.accept(modeBtnExport);
        filterButtons.forEach(widgetAdder);
        actionButtons.forEach(widgetAdder);
        widgetAdder.accept(exportNameEditBox);
    }

    @Override
    public void computeLayout(int screenX, int screenY, int screenWidth, int screenHeight, int toolPaneX, int toolPaneY, int windowHeight)
    {
        super.computeLayout(screenX, screenY, screenWidth, screenHeight, toolPaneX, toolPaneY, windowHeight);

        invX = paneX - INVENTORY_WIDTH - CircuitWorkbenchScreen.PADDING;
        invY = paneY + height - INVENTORY_HEIGHT;
        exportSlotX = paneX + EXPORT_SLOT_X;
        exportSlotY = paneY + EXPORT_SLOT_Y;

        modeBtnImport.setPosition(paneX + MODE_BTN_IMPORT_X, paneY + MODE_BTN_Y);
        modeBtnExport.setPosition(paneX + MODE_BTN_EXPORT_X, paneY + MODE_BTN_Y);
        exportNameEditBox.setPosition(paneX + EXPORT_NAME_EDIT_X, paneY + EXPORT_NAME_EDIT_Y);
        for (int i = 0; i < filterButtons.size(); i++)
        {
            int btnX = paneX + FILTER_BTN_X + FilterToggleButton.SIZE * i;
            filterButtons.get(i).setPosition(btnX, paneY + FILTER_BTN_Y);
        }
        actionButtons.forEach((button) ->
        {
            int btnY = paneY + EXPORT_BTN_Y + (ExportActionButton.HEIGHT + EXPORT_BTN_PADDING) * button.getAction().ordinal();
            button.setPosition(paneX + EXPORT_BTN_X, btnY);
        });

        importListWidget.computeLayout(height - 40, paneX, paneY + 40);

        owner.getSlots().forEach(slot ->
        {
            // Circuit slot
            if (slot.index == 0)
            {
                slot.x = exportSlotX - screenX + 1;
                slot.y = exportSlotY - screenY + 1;
                return;
            }

            // Inventory slots
            int slotIdx = slot.getContainerSlot();
            slot.x = invX - screenX + 11 + (slotIdx % 9 * 18);
            slot.y = invY - screenY + 19 + (slotIdx >= 9 ? ((slotIdx - 1) / 9 * 18) : 58);
        });
    }

    @Override
    public void updateWidgetVisibility(boolean active)
    {
        modeBtnImport.visible = active;
        modeBtnExport.visible = active;

        boolean isImport = active && mode == Mode.IMPORT;
        if (wasImport != isImport)
        {
            filterButtons.forEach(btn -> btn.visible = isImport);
            wasImport = isImport;
        }
        boolean isExport = active && isInExportMode();
        if (wasExport != isExport)
        {
            exportNameEditBox.visible = isExport;
            actionButtons.forEach(btn -> btn.visible = isExport);
            owner.getSlots().forEach(slot -> slot.setActive(isExport));
            wasExport = isExport;
        }
    }

    public int getInvLabelX()
    {
        return invX + 11;
    }

    public int getInvLabelY()
    {
        return invY + 8;
    }

    @Override
    public ToolPaneTab getType()
    {
        return ToolPaneTab.LIBRARY;
    }

    public enum Mode
    {
        IMPORT,
        EXPORT;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.library_browser.mode." + name);

        public Component getTitle()
        {
            return title;
        }
    }

    public enum ExportAction
    {
        EXPORT_TO_LIBRARY,
        EXPORT_TO_ITEM,
        CLEAR_ERROR_ANNOTATIONS,
        ;

        private static final ExportAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.library_browser.export_action." + name);

        private boolean isActive(LibraryBrowser browser)
        {
            return !browser.owner.hasActiveEditAction() && switch (this)
            {
                case EXPORT_TO_LIBRARY -> !browser.exportNameEditBox.getValue().isEmpty();
                case EXPORT_TO_ITEM -> !browser.exportNameEditBox.getValue().isEmpty() && browser.owner.getMenu().getCircuitSlot().hasItem();
                case CLEAR_ERROR_ANNOTATIONS -> !browser.owner.getCanvas().getErrorAnnotations().isEmpty();
            };
        }

        public void execute(LibraryBrowser browser)
        {
            switch (this)
            {
                case EXPORT_TO_LIBRARY -> browser.owner.assembleAndExport(CircuitWorkbenchScreen.ExportTarget.LIBRARY);
                case EXPORT_TO_ITEM -> browser.owner.assembleAndExport(CircuitWorkbenchScreen.ExportTarget.CIRCUIT_ITEM);
                case CLEAR_ERROR_ANNOTATIONS -> browser.owner.getCanvas().getErrorAnnotations().clear();
            }
        }

        public Component getTitle()
        {
            return title;
        }
    }

    public record Entry(CompoundCircuitNode node, IconConfig icon, Component title) implements NodeListWidget.Entry
    {
        @Nullable
        @Override
        public Component subTitle()
        {
            return null;
        }

        @Override
        public PlaceableNode instantiate()
        {
            return ReferencePrototypeNode.create(node);
        }
    }
}
