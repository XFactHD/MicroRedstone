package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ToolActionButton;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireInProgress;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ToolsTab extends ToolPaneTabWidget
{
    public static final Component TITLE_CONFIRM_CLEAR = Utils.translate("title", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm");
    public static final Component MESSAGE_CONFIRM_CLEAR_LINE_ONE = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm_line_one");
    public static final Component MESSAGE_CONFIRM_CLEAR_LINE_TWO = Utils.translate("msg", "circuit_workbench.tools_tab.tool_action.clear_canvas.confirm_line_two");
    private static final int TOOL_BTN_X = 5;
    private static final int TOOL_BTN_Y = 5;
    private static final int TOOL_BTN_PADDING = 2;

    private final List<ToolActionButton> actionButtons;
    private DyeColor wireColor = DyeColor.RED; // TODO: make configurable via context menu

    public ToolsTab(CircuitWorkbenchScreen owner)
    {
        super(owner);
        this.actionButtons = makeActionButtons(this, ToolAction.ACTIONS, ToolActionButton::new);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY)
    {
        actionButtons.forEach((button) -> button.active = button.getAction().isActive(this));
    }

    @Override
    public void init(Consumer<AbstractWidget> widgetAdder)
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

    public enum ToolAction
    {
        CREATE_SINGLE_WIRE(new ToolIcon(WireType.SINGLE.getIcon().texture())),
        CREATE_BUNDLED_WIRE(new ToolIcon(WireType.BUNDLED.getIcon().texture())),
        CLEAR_CANVAS(new ToolIcon(Utils.rl("minecraft", "spectator/close")))
        {
            @Override
            int computeButtonY(int paneY, int paneHeight)
            {
                return paneY + paneHeight - TOOL_BTN_Y - ToolActionButton.HEIGHT;
            }
        };

        private static final ToolAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.tools_tab.tool_action." + name);
        private final ToolIcon icon;

        ToolAction(ToolIcon icon)
        {
            this.icon = icon;
        }

        int computeButtonY(int paneY, int paneHeight)
        {
            return paneY + TOOL_BTN_Y + (ToolActionButton.HEIGHT + TOOL_BTN_PADDING) * ordinal();
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
                    case CLEAR_CANVAS -> true;
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
                case CLEAR_CANVAS -> false;
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
                        tab.owner.getCanvas().startWirePull(WireType.SINGLE, tab.wireColor);
                    }
                }
                case CREATE_BUNDLED_WIRE ->
                {
                    if (wire == null || (wire.getType() == WireType.SINGLE && wire.isEmpty()))
                    {
                        tab.owner.getCanvas().startWirePull(WireType.BUNDLED, null);
                    }
                }
                case CLEAR_CANVAS ->
                {
                    DialogScreen dialog = DialogScreen.builder(DialogScreen.Type.CONFIRM)
                            .withTitle(TITLE_CONFIRM_CLEAR)
                            .withMessage(MESSAGE_CONFIRM_CLEAR_LINE_ONE)
                            .withMessage(MESSAGE_CONFIRM_CLEAR_LINE_TWO)
                            .withOkCallback(tab.owner.getCanvas()::clear)
                            .build();
                    Minecraft.getInstance().pushGuiLayer(dialog);
                }
            }
        }

        public ToolIcon getIcon(ToolsTab tab)
        {
            ToolIcon toolIcon = icon;
            if (this == CREATE_SINGLE_WIRE)
            {
                toolIcon = toolIcon.withColor(tab.wireColor.getTextureDiffuseColor());
            }
            return toolIcon;
        }

        public Component getTitle()
        {
            return title;
        }
    }

    public record ToolIcon(ResourceLocation texture, int color)
    {
        public ToolIcon(ResourceLocation texture)
        {
            this(texture, 0xFFFFFFFF);
        }

        public ToolIcon withColor(int color)
        {
            return new ToolIcon(texture, color);
        }
    }
}
