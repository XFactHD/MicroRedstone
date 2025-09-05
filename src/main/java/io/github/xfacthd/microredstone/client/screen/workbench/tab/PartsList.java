package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.DragStart;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.WorkbenchConfig;
import io.github.xfacthd.microredstone.client.screen.workbench.part.FloatingNode;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.menu.ImportEntryContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.NodeListWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.FilterToggleButton;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ModeButton;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.data.library.ClientCircuitLibrary;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public final class PartsList extends ToolPaneTabWidget implements MultiModeTab<PartsList.Mode>
{
    public static final Component LABEL_MODE = Utils.translate("label", "circuit_workbench.parts_list.mode");
    public static final Component LABEL_FILTER = Utils.translate("label", "circuit_workbench.parts_list.filter");
    public static final Component MSG_DROP_TO_DELETE = Utils.translate("msg", "circuit_workbench.parts_list.drop_to_delete");
    private static final ResourceLocation ICON_DELETE = Utils.rl("delete");
    private static final ShareType[] SHARE_TYPES = ShareType.values();
    private static final int LABEL_X = 5;
    private static final int MODE_LABEL_Y = 10;
    private static final int MODE_BTN_GATES_X = 35;
    private static final int MODE_BTN_LIBRARY_X = MODE_BTN_GATES_X + ModeButton.WIDTH;
    private static final int MODE_BTN_Y = MODE_LABEL_Y - 4;
    private static final int FILTER_LABEL_Y = 28;
    private static final int FILTER_BTN_X = 35;
    private static final int FILTER_BTN_Y = FILTER_LABEL_Y - 4;
    private static final int GATE_LIST_Y = 20;
    private static final int IMPORT_LIST_Y = 38;
    private static final int ICON_DELETE_SIZE = 16;
    private static final int DELETE_OVERLAY_TINT = 0x77FF0000;
    private static final int DELETE_ICON_TINT = 0xFF880000;

    private final ModeButton<Mode> modeBtnGates;
    private final ModeButton<Mode> modeBtnLibrary;
    private final NodeListWidget partListWidget;
    private final List<LibraryEntry> importEntries;
    private final NodeListWidget importListWidget;
    private final List<FilterToggleButton> filterButtons;
    private final EnumSet<ShareType> filters = EnumSet.allOf(ShareType.class);
    private Mode mode = Mode.GATES;
    private boolean wasModeLibrary = true;

    public PartsList(CircuitWorkbenchScreen owner)
    {
        super(owner);
        this.modeBtnGates = new ModeButton<>(this, Mode.GATES, 0, 0);
        this.modeBtnLibrary = new ModeButton<>(this, Mode.LIBRARY, 0, 0);
        this.partListWidget = new NodeListWidget(owner, () -> LogicGateList.ENTRY_COUNT, idx -> LogicGateList.ENTRIES[idx]);
        this.importEntries = new ArrayList<>();
        this.importListWidget = new NodeListWidget(owner, importEntries::size, importEntries::get);
        this.filterButtons = makeActionButtons(this, SHARE_TYPES, FilterToggleButton::new);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(owner.getFont(), LABEL_MODE, paneX + LABEL_X, paneY + MODE_LABEL_Y, 0xFF404040, false);

        switch (mode)
        {
            case GATES -> partListWidget.render(graphics, mouseX, mouseY);
            case LIBRARY ->
            {
                graphics.drawString(owner.getFont(), LABEL_FILTER, paneX + LABEL_X, paneY + FILTER_LABEL_Y, 0xFF404040, false);
                importListWidget.render(graphics, mouseX, mouseY);
            }
        }

        FloatingNode floating = owner.getFloatingNode();
        if (floating != null && floating.lastPos() != null)
        {
            ScrollableWidget scrollable = getScrollableWidget();
            int minX = scrollable.getInnerX();
            int minY = scrollable.getInnerY();
            int width = scrollable.getInnerWidth();
            int height = scrollable.getInnerHeight();

            graphics.fill(minX, minY, minX + width, minY + height, DELETE_OVERLAY_TINT);

            int iconX = minX + (width / 2) - (ICON_DELETE_SIZE / 2);
            int iconY = minY + (height / 2) - (ICON_DELETE_SIZE / 2);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ICON_DELETE, iconX, iconY, ICON_DELETE_SIZE, ICON_DELETE_SIZE, DELETE_ICON_TINT);

            if (scrollable.isMouseOver(mouseX, mouseY))
            {
                graphics.setTooltipForNextFrame(MSG_DROP_TO_DELETE, mouseX, mouseY);
            }
        }
    }

    @Nullable
    public DragStart getClickedPartIdx(double mouseY)
    {
        return switch (mode)
        {
            case GATES -> partListWidget.getClickedEntryIdx(mouseY, DragStart::partsList);
            case LIBRARY -> importListWidget.getClickedEntryIdx(mouseY, DragStart::library);
        };
    }

    @Override
    public ScrollableWidget getScrollableWidget()
    {
        return switch (mode)
        {
            case GATES -> partListWidget;
            case LIBRARY -> importListWidget;
        };
    }

    @Nullable
    public PlaceableNode instantiateLibraryNode(int slot)
    {
        return slot >= 0 && slot < importEntries.size() ? importEntries.get(slot).instantiate() : null;
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

    public void setFilter(ShareType filter, boolean exclusive)
    {
        if (WorkbenchConfig.INSTANCE.setFilter(filter, exclusive))
        {
            filterButtons.forEach(FilterToggleButton::updateTooltip);
            updateImportList();
        }
    }

    public void updateImportList()
    {
        ClientCircuitLibrary.getFilteredEntries(filters, entry -> importEntries.add(LibraryEntry.create(entry)));
    }

    @Override
    public void init(Consumer<AbstractWidget> widgetAdder)
    {
        widgetAdder.accept(modeBtnGates);
        widgetAdder.accept(modeBtnLibrary);
        filterButtons.forEach(widgetAdder);
    }

    @Override
    public void computeLayout(int screenX, int screenY, int screenWidth, int screenHeight, int toolPaneX, int toolPaneY, int windowHeight)
    {
        super.computeLayout(screenX, screenY, screenWidth, screenHeight, toolPaneX, toolPaneY, windowHeight);

        modeBtnGates.setPosition(paneX + MODE_BTN_GATES_X, paneY + MODE_BTN_Y);
        modeBtnLibrary.setPosition(paneX + MODE_BTN_LIBRARY_X, paneY + MODE_BTN_Y);
        partListWidget.computeLayout(height - GATE_LIST_Y, paneX, paneY + GATE_LIST_Y);
        importListWidget.computeLayout(height - IMPORT_LIST_Y, paneX, paneY + IMPORT_LIST_Y);

        for (int i = 0; i < filterButtons.size(); i++)
        {
            int btnX = paneX + FILTER_BTN_X + FilterToggleButton.SIZE * i;
            filterButtons.get(i).setPosition(btnX, paneY + FILTER_BTN_Y);
        }
    }

    @Override
    public void updateWidgetVisibility(boolean active)
    {
        modeBtnGates.visible = active;
        modeBtnLibrary.visible = active;

        boolean isModeLibrary = active && mode == Mode.LIBRARY;
        if (isModeLibrary != wasModeLibrary)
        {
            filterButtons.forEach(btn -> btn.visible = isModeLibrary);
            wasModeLibrary = isModeLibrary;
        }
    }

    @Override
    public ToolPaneTab getType()
    {
        return ToolPaneTab.PARTS;
    }

    public enum Mode implements MultiModeTab.Mode
    {
        GATES,
        LIBRARY;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.parts_list.mode." + name);

        @Override
        public Component getTitle()
        {
            return title;
        }
    }

    public record LibraryEntry(UUID id, CompoundCircuitNode node, IconConfig icon, Component title) implements NodeListWidget.Entry
    {
        private static LibraryEntry create(CircuitLibraryEntry entry)
        {
            IconConfig icon = ReferencePrototypeNode.makeIconConfig(entry.circuitNode());
            return new LibraryEntry(entry.id(), entry.circuitNode(), icon, Component.literal(entry.name()));
        }

        @Nullable
        @Override
        public Component subTitle()
        {
            return null;
        }

        @Override
        public PlaceableNode instantiate()
        {
            return ReferencePrototypeNode.create(node, icon);
        }

        @Override
        public ContextMenuProvider getContextMenuProvider(CircuitWorkbenchScreen owner)
        {
            return new ImportEntryContextMenuProvider(owner, this);
        }
    }
}
