package io.github.xfacthd.microredstone.client.screen.widgets;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.Collection;
import java.util.List;

public abstract class SimpleTransientContainerWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    protected int x = 0;
    protected int y = 0;
    protected int width = 0;
    protected int height = 0;
    protected boolean open = false;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public final boolean isOpen() {
        return open;
    }

    @Override
    public final ScreenRectangle getRectangle() {
        return open ? new ScreenRectangle(x, y, width, height) : ScreenRectangle.empty();
    }

    @Override
    public final boolean isActive() {
        return open;
    }

    @Override
    public final NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public final Collection<? extends NarratableEntry> getNarratables() {
        return List.of();
    }

    @Override
    public final void updateNarration(NarrationElementOutput output) { }
}
