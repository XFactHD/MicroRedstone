package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExportTarget;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.LibraryActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ModeButton;
import io.github.xfacthd.microredstone.client.util.ScreenUtils;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class LibraryBrowser extends ToolPaneTabWidget implements MultiModeTab<LibraryBrowser.Mode>
{
    public static final Component LABEL_MODE = Utils.translate("label", "circuit_workbench.library_browser.mode");
    private static final ResourceLocation INVENTORY = Utils.rl("workbench_inventory");
    private static final ResourceLocation SLOT = Utils.rl("minecraft", "container/slot");
    private static final int INVENTORY_WIDTH = 172;
    private static final int INVENTORY_HEIGHT = 94;
    private static final int LABEL_X = 5;
    private static final int MODE_LABEL_Y = 10;
    private static final int MODE_BTN_IMPORT_X = 35;
    private static final int MODE_BTN_EXPORT_X = MODE_BTN_IMPORT_X + ModeButton.WIDTH;
    private static final int MODE_BTN_Y = MODE_LABEL_Y - 4;
    private static final int SLOT_X = 5;
    private static final int SLOT_Y = 28;
    private static final int SLOT_SIZE = 18;
    private static final int NAME_EDIT_X = SLOT_X + SLOT_SIZE + CircuitWorkbenchScreen.PADDING;
    private static final int NAME_EDIT_Y = 26;
    private static final int NAME_EDIT_WIDTH = TOOL_PANE_WIDTH - NAME_EDIT_X - CircuitWorkbenchScreen.PADDING;
    private static final int NAME_EDIT_HEIGHT = 22;
    private static final int ACTION_BTN_X = 5;
    private static final int ACTION_BTN_Y = 50;
    private static final int ACTION_BTN_PADDING = 2;

    private final ModeButton<Mode> modeBtnImport;
    private final ModeButton<Mode> modeBtnExport;
    private final List<LibraryActionButton<ImportAction>> importActionButtons;
    private final List<LibraryActionButton<ExportAction>> exportActionButtons;
    private final EditBox exportNameEditBox;
    private final ItemStack dummyCircuit = MRContent.ITEM_INTEGRATED_CIRCUIT.value().getDefaultInstance();
    private int invX;
    private int invY;
    private int slotX;
    private int slotY;
    private Mode mode = Mode.IMPORT;
    private boolean wasImport = true;
    private boolean wasExport = true;

    public LibraryBrowser(CircuitWorkbenchScreen owner)
    {
        super(owner);
        this.modeBtnImport = new ModeButton<>(this, Mode.IMPORT, 0, 0);
        this.modeBtnExport = new ModeButton<>(this, Mode.EXPORT, 0, 0);
        this.importActionButtons = makeActionButtons(this, ImportAction.ACTIONS, LibraryActionButton::new);
        this.exportActionButtons = makeActionButtons(this, ExportAction.ACTIONS, LibraryActionButton::new);
        // TODO: add name validation to edit box (don't use existing "filter" predicate, it annoyingly prevents typing invalid entries)
        this.exportNameEditBox = new EditBox(Minecraft.getInstance().font, NAME_EDIT_WIDTH, NAME_EDIT_HEIGHT, Component.empty());
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(owner.getFont(), LABEL_MODE, paneX + LABEL_X, paneY + MODE_LABEL_Y, 0xFF404040, false);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVENTORY, invX, invY, INVENTORY_WIDTH, INVENTORY_HEIGHT);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        if (!owner.getMenu().getCircuitSlot().hasItem())
        {
            ScreenUtils.renderTransparentFakeItem(graphics, dummyCircuit, slotX + 1, slotY + 1);
        }
    }

    @Override
    public Mode getMode()
    {
        return mode;
    }

    @Override
    public void setMode(Mode mode)
    {
        if (this.mode != mode)
        {
            this.mode = mode;
            updateWidgetVisibility(true);
        }
    }

    public boolean isCoveredByInventory(double mouseX, double mouseY)
    {
        return owner.getToolPaneTab() == getType() && mode == Mode.EXPORT && mouseX >= invX && mouseY >= invY;
    }

    @Override
    public void init(Consumer<AbstractWidget> widgetAdder)
    {
        widgetAdder.accept(modeBtnImport);
        widgetAdder.accept(modeBtnExport);
        importActionButtons.forEach(widgetAdder);
        exportActionButtons.forEach(widgetAdder);
        widgetAdder.accept(exportNameEditBox);
    }

    @Override
    public void computeLayout(int screenX, int screenY, int screenWidth, int screenHeight, int toolPaneX, int toolPaneY, int windowHeight)
    {
        super.computeLayout(screenX, screenY, screenWidth, screenHeight, toolPaneX, toolPaneY, windowHeight);

        invX = paneX - INVENTORY_WIDTH - CircuitWorkbenchScreen.PADDING;
        invY = paneY + height - INVENTORY_HEIGHT;
        slotX = paneX + SLOT_X;
        slotY = paneY + SLOT_Y;

        modeBtnImport.setPosition(paneX + MODE_BTN_IMPORT_X, paneY + MODE_BTN_Y);
        modeBtnExport.setPosition(paneX + MODE_BTN_EXPORT_X, paneY + MODE_BTN_Y);
        exportNameEditBox.setPosition(paneX + NAME_EDIT_X, paneY + NAME_EDIT_Y);
        importActionButtons.forEach((button) ->
        {
            int btnY = paneY + ACTION_BTN_Y + (LibraryActionButton.HEIGHT + ACTION_BTN_PADDING) * button.getAction().ordinal();
            button.setPosition(paneX + ACTION_BTN_X, btnY);
        });
        exportActionButtons.forEach((button) ->
        {
            int btnY = paneY + ACTION_BTN_Y + (LibraryActionButton.HEIGHT + ACTION_BTN_PADDING) * button.getAction().ordinal();
            button.setPosition(paneX + ACTION_BTN_X, btnY);
        });

        owner.getSlots().forEach(slot ->
        {
            // Circuit slot
            if (slot.index == 0)
            {
                slot.x = slotX - screenX + 1;
                slot.y = slotY - screenY + 1;
                return;
            }

            // Inventory slots
            int slotIdx = slot.getContainerSlot();
            slot.x = invX - screenX + 11 + (slotIdx % 9 * 18);
            slot.y = invY - screenY + 19 + (slotIdx >= 9 ? ((slotIdx - 9) / 9 * 18) : 58);
        });
    }

    @Override
    public void updateWidgetVisibility(boolean active)
    {
        modeBtnImport.visible = active;
        modeBtnExport.visible = active;
        owner.getSlots().forEach(slot -> slot.setActive(active));

        boolean isImport = active && mode == Mode.IMPORT;
        if (wasImport != isImport)
        {
            importActionButtons.forEach(btn -> btn.visible = isImport);
            wasImport = isImport;
        }
        boolean isExport = active && mode == Mode.EXPORT;
        if (wasExport != isExport)
        {
            exportNameEditBox.visible = isExport;
            exportActionButtons.forEach(btn -> btn.visible = isExport);
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

    public enum Mode implements MultiModeTab.Mode
    {
        IMPORT,
        EXPORT;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.library_browser.mode." + name);

        @Override
        public Component getTitle()
        {
            return title;
        }
    }

    public enum ImportAction implements LibraryActionButton.Action<LibraryBrowser>
    {
        IMPORT_FROM_ITEM,
        IMPORT_FROM_JSON,
        ;

        private static final ImportAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.library_browser.import_action." + name);

        @Override
        public boolean isActive(LibraryBrowser browser)
        {
            return !browser.owner.hasActiveEditAction() && switch (this)
            {
                case IMPORT_FROM_ITEM -> browser.owner.getMenu().getCircuitSlot().hasItem();
                case IMPORT_FROM_JSON -> true;
            };
        }

        @Override
        public void execute(LibraryBrowser browser)
        {
            switch (this)
            {
                case IMPORT_FROM_ITEM -> browser.owner.importCircuitFromItem(
                        browser.owner.getMenu().getCircuitSlot().getItem()
                );
                case IMPORT_FROM_JSON -> browser.owner.importCircuitFromClipboard();
            }
        }

        @Override
        public Component getTitle()
        {
            return title;
        }
    }

    public enum ExportAction implements LibraryActionButton.Action<LibraryBrowser>
    {
        EXPORT_TO_LIBRARY,
        EXPORT_TO_ITEM,
        EXPORT_TO_JSON,
        CLEAR_ERROR_ANNOTATIONS,
        ;

        private static final ExportAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.library_browser.export_action." + name);

        @Override
        public boolean isActive(LibraryBrowser browser)
        {
            return !browser.owner.hasActiveEditAction() && switch (this)
            {
                case EXPORT_TO_LIBRARY, EXPORT_TO_JSON -> !browser.exportNameEditBox.getValue().isEmpty();
                case EXPORT_TO_ITEM -> !browser.exportNameEditBox.getValue().isEmpty() && browser.owner.getMenu().getCircuitSlot().hasItem();
                case CLEAR_ERROR_ANNOTATIONS -> !browser.owner.getCanvas().getErrorAnnotations().isEmpty();
            };
        }

        @Override
        public void execute(LibraryBrowser browser)
        {
            String circuitName = browser.exportNameEditBox.getValue();
            switch (this)
            {
                case EXPORT_TO_LIBRARY -> browser.owner.assembleAndExport(circuitName, ExportTarget.LIBRARY);
                case EXPORT_TO_ITEM -> browser.owner.assembleAndExport(circuitName, ExportTarget.CIRCUIT_ITEM);
                case EXPORT_TO_JSON -> browser.owner.assembleAndExport(circuitName, ExportTarget.JSON_IN_CLIPBOARD);
                case CLEAR_ERROR_ANNOTATIONS -> browser.owner.getCanvas().getErrorAnnotations().clear();
            }
        }

        @Override
        public Component getTitle()
        {
            return title;
        }
    }
}
