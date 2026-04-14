package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import io.github.xfacthd.microredstone.client.screen.widgets.ScrollableWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuProvider;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.tab.ToolPaneTabWidget;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class NodeListWidget extends ScrollableWidget {
    private static final Identifier BACKGROUND = Utils.rl("node_list_background");
    private static final Identifier BUTTON = Utils.rl("minecraft", "widget/button");
    private static final Identifier BUTTON_HOVER = Utils.rl("minecraft", "widget/button_highlighted");
    private static final Identifier SCROLLER_HANDLE = Utils.rl("minecraft", "container/villager/scroller");

    private static final int PADDING = 5;
    private static final int BORDER = 5;
    private static final int SCROLLER_HANDLE_WIDTH = 6;
    private static final int SCROLLER_BG_WIDTH = SCROLLER_HANDLE_WIDTH + 2;
    private static final int WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH - SCROLLER_BG_WIDTH;
    private static final int INNER_WIDTH = WIDTH - (BORDER * 2);
    private static final int ENTRY_WIDTH = WIDTH - (BORDER * 2);
    private static final int ENTRY_ICON_SIZE = 16;
    private static final int ENTRY_HEIGHT = 30;
    private static final int ICON_OFF_Y = (ENTRY_HEIGHT / 2) - (ENTRY_ICON_SIZE / 2);
    private static final int ENTRY_NAME_OFF_X = ENTRY_ICON_SIZE + (PADDING * 2);
    private static final int ENTRY_NAME_OFF_Y = 11;
    private static final int ENTRY_NAME_BORDER_RIGHT = 3;
    private static final int FULL_INNER_WIDTH = ToolPaneTabWidget.TOOL_PANE_WIDTH - (BORDER * 2);

    private final CircuitWorkbenchScreen owner;
    private final IntSupplier entryCount;
    private final IntFunction<Entry> entryGetter;
    private int listX;
    private int scrollbarX;
    private int innerScrollbarX;
    private int innerY;
    private int innerHeight;

    public NodeListWidget(CircuitWorkbenchScreen owner, IntSupplier entryCount, IntFunction<Entry> entryGetter) {
        this.owner = owner;
        this.entryCount = entryCount;
        this.entryGetter = entryGetter;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int minX = listX;
        int maxX = minX + ENTRY_WIDTH;
        int minY = innerY;
        int maxY = minY + innerHeight;
        int entriesHeight = getEntriesHeight();
        boolean mouseOverX = mouseX >= minX && mouseX < maxX;
        boolean mouseOverY = mouseY >= minY && mouseY < maxY;
        boolean canScroll = entriesHeight > innerHeight;

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, minX - 1, minY - 1, ENTRY_WIDTH + 2, innerHeight + 2);

        graphics.enableScissor(minX, minY, maxX, maxY);

        int count = entryCount.getAsInt();
        for (int i = 0; i < count; i++) {
            int y = minY + i * ENTRY_HEIGHT - getScrollOffset();
            if (y + ENTRY_HEIGHT < minY || y > maxY) {
                continue;
            }

            Entry entry = entryGetter.apply(i);

            boolean hovered = mouseOverX && mouseOverY && mouseY >= y && mouseY < y + ENTRY_HEIGHT;
            Identifier sprite = hovered ? BUTTON_HOVER : BUTTON;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, minX, y, ENTRY_WIDTH, ENTRY_HEIGHT);

            CircuitCanvas.drawPartNode(graphics, entry.icon(), minX + CircuitWorkbenchScreen.PADDING, y + ICON_OFF_Y, 0, ENTRY_ICON_SIZE);

            int nameX = minX + ENTRY_NAME_OFF_X;
            int nameY = y + ENTRY_NAME_OFF_Y;
            Component subTitle = entry.subTitle();
            ActiveTextCollector textCollector = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
            if (subTitle != null) {
                graphics.drawScrollingString(textCollector, owner.getFont(), entry.title(), nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY - 6);
                graphics.drawScrollingString(textCollector, owner.getFont(), subTitle, nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY + 5);
            } else {
                graphics.drawScrollingString(textCollector, owner.getFont(), entry.title(), nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY);
            }

            if (hovered) {
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        }

        graphics.disableScissor();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, scrollbarX, minY - 1, SCROLLER_BG_WIDTH, innerHeight + 2);
        if (canScroll) {
            float scrollFactor = (float) getScrollOffset() / (entriesHeight - innerHeight);
            int scrollerY = minY + (int) (scrollFactor * (innerHeight - SCROLLER_HANDLE_HEIGHT));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_HANDLE, innerScrollbarX, scrollerY, SCROLLER_HANDLE_WIDTH, SCROLLER_HANDLE_HEIGHT);
        }

        if (isDragging()) {
            graphics.requestCursor(CursorTypes.RESIZE_NS);
        } else if (isMouseOverScrollBar(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) {
            return false;
        }
        return mouseX >= listX && mouseX < (listX + FULL_INNER_WIDTH);
    }

    @Override
    public boolean isMouseOverList(double mouseX, double mouseY) {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) {
            return false;
        }
        return mouseX >= listX && mouseX < (listX + NodeListWidget.INNER_WIDTH);
    }

    @Override
    public boolean isMouseOverScrollBar(double mouseX, double mouseY) {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) {
            return false;
        }
        return mouseX >= innerScrollbarX && mouseX < (innerScrollbarX + SCROLLER_HANDLE_WIDTH);
    }

    @Override
    public int getInnerX() {
        return listX;
    }

    @Override
    public int getInnerY() {
        return innerY;
    }

    @Override
    public int getInnerWidth() {
        return FULL_INNER_WIDTH;
    }

    @Override
    public int getInnerHeight() {
        return innerHeight;
    }

    @Override
    protected int getEntriesHeight() {
        return entryCount.getAsInt() * ENTRY_HEIGHT;
    }

    @Override
    public @Nullable ContextMenuProvider getContextMenuProvider(double mouseX, double mouseY) {
        return getClickedEntryIdx(mouseY, idx -> entryGetter.apply(idx).getContextMenuProvider(owner));
    }

    public <T> @Nullable T getClickedEntryIdx(double mouseY, IntFunction<@Nullable T> resultFactory) {
        int relY = (int) (mouseY - innerY + getScrollOffset());
        if (relY < 0) {
            return null;
        }

        int idx = relY / NodeListWidget.ENTRY_HEIGHT;
        int count = entryCount.getAsInt();
        return idx < count ? resultFactory.apply(idx) : null;
    }

    public void computeLayout(int height, int paneX, int paneY) {
        this.innerHeight = height - (BORDER * 2);
        scrollbarX = paneX + WIDTH - BORDER + 1;
        listX = paneX + BORDER;
        innerScrollbarX = scrollbarX + 1;
        innerY = paneY + BORDER;
    }

    public interface Entry {
        IconConfig icon();

        Component title();

        @Nullable Component subTitle();

        PlaceableNode instantiate();

        default @Nullable ContextMenuProvider getContextMenuProvider(CircuitWorkbenchScreen owner) {
            return null;
        }
    }
}
