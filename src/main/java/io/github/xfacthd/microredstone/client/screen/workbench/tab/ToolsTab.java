package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.data.LocalCircuitStorage;
import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.WorkbenchConfig;
import io.github.xfacthd.microredstone.client.screen.workbench.menu.WireToolActionContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.ToolPane;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ToolActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireInProgress;
import io.github.xfacthd.microredstone.client.util.Icon;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ToolsTab extends ToolPaneTabWidget
{
    public static final Component TITLE_LOAD_FAILED = Utils.translate("title", "circuit_workbench.tools_tab.tool_action.load.failed");
    public static final Component MESSAGE_LOAD_FAILED = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.load.failed");
    public static final Component TITLE_SAVE_FAILED = Utils.translate("title", "circuit_workbench.tools_tab.tool_action.save.failed");
    public static final Component MESSAGE_SAVE_FAILED = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.save.failed");
    public static final Component TITLE_CONFIRM_CLEAR = Utils.translate("title", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm");
    public static final Component MESSAGE_CONFIRM_CLEAR_LINE_ONE = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm_line_one");
    public static final Component MESSAGE_CONFIRM_CLEAR_LINE_TWO = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm_line_two");
    private static final int TOOL_BTN_X = 5;
    private static final int TOOL_BTN_Y = 5;
    private static final int TOOL_BTN_PADDING = 2;

    private final List<ToolActionButton> actionButtons;

    public ToolsTab(CircuitWorkbenchScreen owner, ToolPane toolPane)
    {
        super(owner, toolPane);
        this.actionButtons = makeActionButtons(this, ToolAction.ACTIONS, ToolActionButton::new);
    }

    @Override
    protected void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        actionButtons.forEach((button) -> button.active = button.getAction().isActive(this));
    }

    @Override
    public void initContent(Consumer<AbstractWidget> widgetAdder)
    {
        actionButtons.forEach(widgetAdder);
    }

    @Override
    public void computeLayout(int screenX, int screenY, int screenWidth, int screenHeight, int toolPaneX, int toolPaneY, int windowHeight)
    {
        super.computeLayout(screenX, screenY, screenWidth, screenHeight, toolPaneX, toolPaneY, windowHeight);

        actionButtons.forEach((button) ->
        {
            int btnY = button.getAction().computeButtonY(paneY, height);
            button.setPosition(paneX + TOOL_BTN_X, btnY);
        });
    }

    @Override
    public void updateWidgetVisibility(boolean active)
    {
        actionButtons.forEach(btn -> btn.visible = active);
    }

    @Override
    public ToolPaneTab getType()
    {
        return ToolPaneTab.TOOLS;
    }

    // TODO: make save/load icons
    public enum ToolAction
    {
        CREATE_SINGLE_WIRE(WireType.SINGLE.getIcon(), WireToolActionContextMenuProvider.INSTANCE_SINGLE, Alignment.TOP)
        {
            @Override
            public Icon getIcon()
            {
                return super.getIcon().withColor(WorkbenchConfig.INSTANCE.getWireColor());
            }
        },
        CREATE_BUNDLED_WIRE(WireType.BUNDLED.getIcon(), WireToolActionContextMenuProvider.INSTANCE_BUNDLED, Alignment.TOP),
        LOAD_LOCAL(new Icon(Utils.rl("button/load_circuit")), null, Alignment.BOTTOM),
        SAVE_LOCAL(new Icon(Utils.rl("button/save_circuit")), null, Alignment.BOTTOM),
        CLEAR_CANVAS(new Icon(Utils.rl("minecraft", "spectator/close")), null, Alignment.BOTTOM),
        ;

        private static final ToolAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.tools_tab.tool_action." + name);
        private final Icon icon;
        @Nullable
        private final ContextMenuProvider menuProvider;
        private final Alignment alignment;

        ToolAction(Icon icon, @Nullable ContextMenuProvider menuProvider, Alignment alignment)
        {
            this.icon = icon;
            this.menuProvider = menuProvider;
            this.alignment = alignment;
        }

        int computeButtonY(int paneY, int paneHeight)
        {
            return switch (alignment)
            {
                case TOP -> paneY + TOOL_BTN_Y + (ToolActionButton.HEIGHT + TOOL_BTN_PADDING) * ordinal();
                case BOTTOM ->
                {
                    int idx = ACTIONS.length - 1 - ordinal();
                    yield paneY + paneHeight - TOOL_BTN_Y - ToolActionButton.HEIGHT * (idx + 1) - TOOL_BTN_PADDING * idx;
                }
            };
        }

        private boolean isActive(ToolsTab tab)
        {
            if (tab.owner.hasActiveEditAction())
            {
                WireInProgress wire = tab.owner.getCanvas().getWireInProgress();
                return switch (this)
                {
                    case CREATE_SINGLE_WIRE -> wire != null && (wire.getType() == WireType.SINGLE || wire.isEmpty());
                    case CREATE_BUNDLED_WIRE -> wire != null && (wire.getType() == WireType.BUNDLED || wire.isEmpty());
                    case LOAD_LOCAL, CLEAR_CANVAS -> true;
                    case SAVE_LOCAL -> !tab.owner.getCanvas().isEmpty();
                };
            }
            return true;
        }

        public boolean isInUse(ToolsTab tab)
        {
            WireInProgress wire = tab.owner.getCanvas().getWireInProgress();
            return switch (this)
            {
                case CREATE_SINGLE_WIRE -> wire != null && wire.getType() == WireType.SINGLE;
                case CREATE_BUNDLED_WIRE -> wire != null && wire.getType() == WireType.BUNDLED;
                case LOAD_LOCAL, SAVE_LOCAL, CLEAR_CANVAS -> false;
            };
        }

        public void execute(ToolsTab tab)
        {
            WireInProgress wire = tab.owner.getCanvas().getWireInProgress();
            switch (this)
            {
                case CREATE_SINGLE_WIRE ->
                {
                    if (wire == null || (wire.getType() == WireType.BUNDLED && wire.isEmpty()))
                    {
                        tab.owner.getCanvas().startWirePull(WireType.SINGLE);
                    }
                }
                case CREATE_BUNDLED_WIRE ->
                {
                    if (wire == null || (wire.getType() == WireType.SINGLE && wire.isEmpty()))
                    {
                        tab.owner.getCanvas().startWirePull(WireType.BUNDLED);
                    }
                }
                case LOAD_LOCAL -> LocalCircuitStorage.load(result ->
                {
                    switch (result)
                    {
                        case LocalCircuitStorage.LoadResult.Success(CompoundPrototypeNode circuit) -> tab.owner.getCanvas().importPrototype(circuit);
                        case LocalCircuitStorage.LoadResult.Canceled ignored -> { }
                        case LocalCircuitStorage.LoadResult.Error(Throwable error) -> openErrorDialog(TITLE_SAVE_FAILED, MESSAGE_SAVE_FAILED, error);
                    }
                });
                case SAVE_LOCAL -> LocalCircuitStorage.store(tab.owner.getCanvas().getRootNode(), result ->
                {
                    if (result instanceof LocalCircuitStorage.StoreResult.Error(Throwable error))
                    {
                        openErrorDialog(TITLE_LOAD_FAILED, MESSAGE_LOAD_FAILED, error);
                    }
                });
                case CLEAR_CANVAS -> DialogScreen.builder(DialogScreen.Type.CONFIRM)
                        .withTitle(TITLE_CONFIRM_CLEAR)
                        .withMessage(MESSAGE_CONFIRM_CLEAR_LINE_ONE)
                        .withMessage(MESSAGE_CONFIRM_CLEAR_LINE_TWO)
                        .withOkCallback(tab.owner.getCanvas()::clear)
                        .show();
            }
        }

        private static void openErrorDialog(Component title, Component message, Throwable error)
        {
            Component errorMessage = Component.literal(error.getClass().getName())
                    .append(": ")
                    .append(error.getMessage());
            DialogScreen.builder(DialogScreen.Type.ERROR)
                    .withTitle(title)
                    .withMessage(message)
                    .withMessage(errorMessage)
                    .show();
        }

        public Icon getIcon()
        {
            return icon;
        }

        public Component getTitle()
        {
            return title;
        }

        @Nullable
        public ContextMenuProvider getContextMenuProvider()
        {
            return menuProvider;
        }

        private enum Alignment
        {
            TOP,
            BOTTOM
        }
    }
}
