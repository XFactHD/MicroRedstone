package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import net.minecraft.client.gui.components.Renderable;

interface MenuEntry extends Renderable
{
    void setLayout(int x, int y, int width);

    int getHeight();

    int getRequiredWidth();
}
