package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ToolPaneTab;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.ToolActionButton;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireInProgress;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.util.Utils;
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
            int btnY = paneY + TOOL_BTN_Y + (ToolActionButton.HEIGHT + TOOL_BTN_PADDING) * button.getAction().ordinal();
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
        ;

        private static final ToolAction[] ACTIONS = values();

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component title = Utils.translate("label", "circuit_workbench.tools_tab.tool_action." + name);
        private final ToolIcon icon;

        ToolAction(ToolIcon icon)
        {
            this.icon = icon;
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
            }
        }

        public ToolIcon getIcon(ToolsTab tab)
        {
            return switch (this)
            {
                case CREATE_SINGLE_WIRE -> icon.withColor(tab.wireColor.getTextureDiffuseColor());
                case CREATE_BUNDLED_WIRE -> icon;
            };
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
