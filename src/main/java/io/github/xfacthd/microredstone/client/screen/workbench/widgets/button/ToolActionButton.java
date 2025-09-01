package io.github.xfacthd.microredstone.client.screen.workbench.widgets.button;

import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolsTab;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.glfw.GLFW;

public final class ToolActionButton extends SimpleButton implements DropFocusAfterClick
{
    private static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH - 10;
    public static final int HEIGHT = 20;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = (HEIGHT - ICON_SIZE) / 2;
    private static final int TEXT_OFFSET = ICON_SIZE + ICON_PADDING * 2;

    private final ToolsTab owner;
    private final ToolsTab.ToolAction action;

    public ToolActionButton(ToolsTab owner, ToolsTab.ToolAction action, int x, int y)
    {
        super(x, y, WIDTH, HEIGHT, action.getTitle());
        this.owner = owner;
        this.action = action;
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int color)
    {
        int minX = getX() + TEXT_OFFSET;
        int maxX = getX() + getWidth() - 2;
        renderScrollingString(guiGraphics, font, getMessage(), minX, getY(), maxX, getY() + getHeight(), color);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (action.isInUse(owner))
        {
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xAA169C9C);
        }

        super.renderForeground(graphics, mouseX, mouseY);

        int iconX = getX() + ICON_PADDING;
        int iconY = getY() + ICON_PADDING;
        ToolsTab.ToolIcon icon = action.getIcon(owner);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon.texture(), iconX, iconY, ICON_SIZE, ICON_SIZE, icon.color());
    }

    @Override
    protected WidgetSprites getSprites()
    {
        return SPRITES;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button)
    {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            onClick(mouseX, mouseY);
        }
    }

    @Override
    public void onPress()
    {
        action.execute(owner);
    }

    public ToolsTab.ToolAction getAction()
    {
        return action;
    }
}
