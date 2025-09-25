package io.github.xfacthd.microredstone.client.screen.widgets;

import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntSupplier;

public final class Scrollbar implements GuiEventListener, Renderable
{
    private static final ResourceLocation BACKGROUND = Utils.rl("node_list_background");
    private static final ResourceLocation SCROLLER = Utils.rl("minecraft", "container/villager/scroller");
    public static final int BACKGROUND_WIDTH = 8;
    public static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_HEIGHT = 27;
    private static final int SCROLL_SPEED = 10;

    private int x;
    private int y;
    private final int barWidth;
    private final int barHeight;
    private final int listHeight;
    private final boolean withBackground;
    private final IntSupplier contentHeightGetter;
    private final boolean hideWhenSmaller;
    private int contentHeight;
    private final HoverTest listHoverCheck;
    private int offset;
    private boolean active = false;
    private boolean visible = false;
    private boolean dragging = false;

    public Scrollbar(
            int x,
            int y,
            int barHeight,
            int listHeight,
            boolean hideWhenSmaller,
            boolean withBackground,
            IntSupplier contentHeightGetter,
            HoverTest listHoverCheck,
            @Nullable Scrollbar oldScrollbar
    )
    {
        this.x = withBackground ? (x + 1) : x;
        this.y = withBackground ? (y + 1) : y;
        this.barWidth = withBackground ? BACKGROUND_WIDTH : SCROLLER_WIDTH;
        this.barHeight = withBackground ? (barHeight - 2) : barHeight;
        this.listHeight = listHeight;
        this.withBackground = withBackground;
        this.contentHeightGetter = contentHeightGetter;
        this.hideWhenSmaller = hideWhenSmaller;
        this.listHoverCheck = listHoverCheck;
        this.offset = oldScrollbar != null ? oldScrollbar.offset : 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (!visible) return;

        if (withBackground)
        {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x - 1, y - 1, BACKGROUND_WIDTH, barHeight + 2);
        }
        if (active)
        {
            float scrollFactor = (float) offset / (contentHeight - listHeight);
            int scrollerY = y + (int) (scrollFactor * (barHeight - SCROLLER_HEIGHT));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, x, scrollerY, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        }
    }

    public void update()
    {
        contentHeight = contentHeightGetter.getAsInt();
        offset = Math.clamp(offset, 0, Math.max(contentHeight - listHeight, 0));
        active = contentHeight > listHeight;
        visible = !hideWhenSmaller || active;
    }

    public boolean isVisible()
    {
        return visible;
    }

    public int getWidth()
    {
        return barWidth;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setPosition(int x, int y)
    {
        this.x = withBackground ? (x + 1) : x;
        this.y = withBackground ? (y + 1) : y;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (active && button == GLFW.GLFW_MOUSE_BUTTON_1 && (dragging || isMouseOver(mouseX, mouseY)))
        {
            int maxOffset = contentHeight - listHeight;
            double offset = (mouseY - y - (SCROLLER_HEIGHT / 2F)) / (barHeight - SCROLLER_HEIGHT);
            this.offset = (int) Mth.clamp(offset * maxOffset, 0, maxOffset);
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (active && dragging && button == GLFW.GLFW_MOUSE_BUTTON_1)
        {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (active && listHoverCheck.test(mouseX, mouseY))
        {
            offset = Math.clamp((int)(offset - (scrollY * SCROLL_SPEED)), 0, contentHeight - listHeight);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        return mouseX >= x && mouseX < x + SCROLLER_WIDTH && mouseY >= y && mouseY < y + barHeight;
    }

    @Override
    public void setFocused(boolean focused) { }

    @Override
    public boolean isFocused()
    {
        return false;
    }

    @FunctionalInterface
    public interface HoverTest
    {
        boolean test(double mouseX, double mouseY);
    }
}
