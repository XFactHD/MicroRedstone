package io.github.xfacthd.microredstone.client.screen.widgets;

import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public abstract class ScrollableWidget {
    private static final int SCROLL_SPEED = 10;
    protected static final int SCROLLER_HANDLE_HEIGHT = 27;

    private int scrollOffset = 0;
    private boolean dragging = false;

    public final void scroll(double yDiff) {
        int innerHeight = getInnerHeight();
        int entriesHeight = getEntriesHeight();
        if (entriesHeight <= innerHeight) {
            scrollOffset = 0;
            return;
        }

        int offset = (int) (yDiff * SCROLL_SPEED);
        scrollOffset = Mth.clamp(scrollOffset + offset, 0, entriesHeight - innerHeight);
    }

    public final void dragScrollBar(double mouseY) {
        int innerHeight = getInnerHeight();
        int entriesHeight = getEntriesHeight();
        if (entriesHeight <= innerHeight) {
            scrollOffset = 0;
            return;
        }

        int maxOffset = entriesHeight - innerHeight;
        double offset = (mouseY - getInnerY() - (SCROLLER_HANDLE_HEIGHT / 2F)) / (innerHeight - SCROLLER_HANDLE_HEIGHT);
        scrollOffset = (int) Mth.clamp(offset * maxOffset, 0, maxOffset);
    }

    public final int getScrollOffset() {
        return scrollOffset;
    }

    public final void resetScrollOffset() {
        this.scrollOffset = 0;
    }

    public final void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public final boolean isDragging() {
        return dragging;
    }

    public abstract int getInnerX();

    public abstract int getInnerY();

    public abstract int getInnerWidth();

    public abstract int getInnerHeight();

    protected abstract int getEntriesHeight();

    public abstract boolean isMouseOver(double mouseX, double mouseY);

    public abstract boolean isMouseOverList(double mouseX, double mouseY);

    public abstract boolean isMouseOverScrollBar(double mouseX, double mouseY);

    public @Nullable ContextMenuProvider getContextMenuProvider(double mouseX, double mouseY) {
        return null;
    }
}
