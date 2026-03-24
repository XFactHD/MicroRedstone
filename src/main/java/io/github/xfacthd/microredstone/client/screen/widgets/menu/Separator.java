package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class Separator implements MenuEntry
{
    private static final int HEIGHT = 3;
    private static final int LINE_OFF_X = 3;
    private static final int LINE_OFF_Y = (HEIGHT - 1) / 2;

    private int x;
    private int y;
    private int width;

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.horizontalLine(x + LINE_OFF_X, x + width - 1 - LINE_OFF_X, y + LINE_OFF_Y, ContextMenu.HIGHLIGHT_COLOR);
    }

    @Override
    public void setLayout(int x, int y, int width)
    {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    @Override
    public int getHeight()
    {
        return HEIGHT;
    }

    @Override
    public int getRequiredWidth()
    {
        return 0;
    }
}
