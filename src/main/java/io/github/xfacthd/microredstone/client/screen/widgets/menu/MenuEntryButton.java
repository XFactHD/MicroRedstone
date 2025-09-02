package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.DropFocusAfterClick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

import java.util.Optional;
import java.util.function.BooleanSupplier;

final class MenuEntryButton extends AbstractButton implements MenuEntry, DropFocusAfterClick
{
    private static final int HEIGHT = 14;
    private static final long HOVER_OPEN_DELAY = 500;
    private static final int HOR_PADDING = 3;
    private static final int MARKER_PADDING = 10;
    private static final int HIGHLIGHT_ALPHA = 0x77;
    private static final int HIGHLIGHT_COLOR = ARGB.color(HIGHLIGHT_ALPHA, ContextMenu.HIGHLIGHT_COLOR);
    private static final Component SUB_MENU_ARROW = Component.literal(">");
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final Component SELECTION_MARKER = Component.literal("\u2714");

    final ContextMenu owner;
    private final FormattedCharSequence text;
    private final Action action;
    private final int requiredWidth;
    private final int markerWidth;
    private final Font font;
    private boolean wasHovered = false;
    private long hoverStart = -1;

    static MenuEntryButton create(ContextMenu owner, FormattedText text, Runnable task, Optional<BooleanSupplier> stateSupplier)
    {
        return create(owner, text, new TaskAction(task, stateSupplier));
    }

    static MenuEntryButton create(ContextMenu owner, FormattedText text, SubMenuKey subMenuKey)
    {
        return create(owner, text, new SubMenuAction(subMenuKey));
    }

    private static MenuEntryButton create(ContextMenu owner, FormattedText text, Action action)
    {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(text);
        int markerWidth = 0;
        int paddedMarkerWidth = 0;
        int availableWidth = ContextMenu.MAX_WIDTH - HOR_PADDING * 2;
        if (action.hasMarker())
        {
            markerWidth = font.width(action.getMarker());
            paddedMarkerWidth = markerWidth + MARKER_PADDING;
            availableWidth -= paddedMarkerWidth;
        }
        if (textWidth > availableWidth)
        {
            text = font.ellipsize(text, availableWidth);
        }
        int requiredWidth = Math.min(textWidth + paddedMarkerWidth + HOR_PADDING * 2, ContextMenu.MAX_WIDTH);
        return new MenuEntryButton(owner, text, action, requiredWidth, markerWidth, font);
    }

    private MenuEntryButton(ContextMenu owner, FormattedText text, Action action, int requiredWidth, int markerWidth, Font font)
    {
        super(0, 0, 0, HEIGHT, Component.empty());
        this.owner = owner;
        this.text = Language.getInstance().getVisualOrder(text);
        this.action = action;
        this.requiredWidth = requiredWidth;
        this.markerWidth = markerWidth;
        this.font = font;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        if (isHoveredOrFocused())
        {
            graphics.fill(getX(), getY(), getRight(), getBottom(), HIGHLIGHT_COLOR);
        }
        renderString(graphics, font, ARGB.color(alpha, active ? 0xFFFFFFFF : 0xFFA0A0A0));
        handleSubMenuState(mouseX, mouseY);
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color)
    {
        int textY = getY() + 3;
        graphics.drawString(font, text, getX() + HOR_PADDING, textY, color);
        if (action.isMarkerVisible())
        {
            int textX = getRight() - HOR_PADDING - markerWidth;
            graphics.drawString(font, action.getMarker(), textX, textY, color);
        }
    }

    private void handleSubMenuState(int mouseX, int mouseY)
    {
        if (!(action instanceof SubMenuAction(SubMenuKey key))) return;

        ContextMenu openMenu = owner.getOpenSubMenu(key);
        if (isHovered && !wasHovered && openMenu == null)
        {
            wasHovered = true;
            hoverStart = System.currentTimeMillis();
        }
        else if (!isHovered || openMenu != null)
        {
            wasHovered = false;
            hoverStart = -1;
        }
        if (hoverStart != -1 && System.currentTimeMillis() - hoverStart > HOVER_OPEN_DELAY)
        {
            openSubMenu(key);
        }
        else if (!isCursorCloseEnough(mouseX, mouseY) && openMenu != null && !openMenu.shouldKeepMenuOpen(mouseX, mouseY, false))
        {
            openMenu.close();
        }
    }

    private boolean isCursorCloseEnough(int mouseX, int mouseY)
    {
        return isHovered || (owner.isMouseOver(mouseX, mouseY) && isMouseOver(getX(), mouseY));
    }

    @Override
    public void onPress()
    {
        action.execute(this);
    }

    private void openSubMenu(SubMenuKey subMenuKey)
    {
        owner.openSubMenu(subMenuKey, getX() - 1, getRight() + 1, getY() - 1);
    }

    boolean opensSubMenuOn(ContextMenu menu)
    {
        return owner == menu && action instanceof SubMenuAction;
    }

    boolean opensSubMenu(SubMenuKey subMenuKey)
    {
        return action instanceof SubMenuAction(SubMenuKey key) && key == subMenuKey;
    }

    @Override
    public void setLayout(int x, int y, int width)
    {
        setPosition(x, y);
        setWidth(width);
    }

    @Override
    public int getRequiredWidth()
    {
        return requiredWidth;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output)
    {
        defaultButtonNarrationText(output);
    }

    private sealed interface Action permits TaskAction, SubMenuAction
    {
        void execute(MenuEntryButton button);

        boolean isMarkerVisible();

        boolean hasMarker();

        Component getMarker();
    }

    private record TaskAction(Runnable task, Optional<BooleanSupplier> stateSupplier) implements Action
    {
        @Override
        public void execute(MenuEntryButton button)
        {
            task.run();
            button.owner.getRoot().close();
        }

        @Override
        public boolean isMarkerVisible()
        {
            return stateSupplier.isPresent() && stateSupplier.get().getAsBoolean();
        }

        @Override
        public boolean hasMarker()
        {
            return stateSupplier.isPresent();
        }

        @Override
        public Component getMarker()
        {
            return SELECTION_MARKER;
        }
    }

    private record SubMenuAction(SubMenuKey key) implements Action
    {
        @Override
        public void execute(MenuEntryButton button)
        {
            button.openSubMenu(key);
        }

        @Override
        public boolean isMarkerVisible()
        {
            return true;
        }

        @Override
        public boolean hasMarker()
        {
            return true;
        }

        @Override
        public Component getMarker()
        {
            return SUB_MENU_ARROW;
        }
    }
}
