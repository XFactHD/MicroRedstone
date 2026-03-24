package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import com.google.common.base.Preconditions;
import io.github.xfacthd.microredstone.client.screen.widgets.SimpleTransientContainerWidget;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.button.DropFocusAfterClick;
import io.github.xfacthd.microredstone.client.util.ArrowKey;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class ContextMenu extends SimpleTransientContainerWidget
{
    private static final Identifier BACKGROUND = Utils.rl("context_menu_background");
    static final int HIGHLIGHT_COLOR = CommonColors.LIGHT_GRAY;
    static final int MIN_WIDTH = 70;
    static final int MAX_WIDTH = 160;

    final Screen owner;
    @Nullable
    private final ContextMenu superMenu;
    final List<MenuEntryButton> entries = new ArrayList<>();
    final List<MenuEntry> renderables = new ArrayList<>();
    private ContextMenuProvider activeProvider = DummyProvider.INSTANCE;
    @Nullable
    private OpenSubMenu openSubMenu = null;
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    public ContextMenu(Screen owner)
    {
        this.owner = owner;
        this.superMenu = null;
    }

    private ContextMenu(ContextMenu superMenu)
    {
        this.owner = superMenu.owner;
        this.superMenu = superMenu;
        this.activeProvider = superMenu.activeProvider;
        this.open = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        if (!open) return;

        if (superMenu == null && (mouseX != lastMouseX || mouseY != lastMouseY))
        {
            if (isMouseOver(mouseX, mouseY))
            {
                resetFocusOnMouseMove(mouseX, mouseY);
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, width, height);

        for (Renderable renderable : renderables)
        {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (openSubMenu != null)
        {
            openSubMenu.menu.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void resetFocusOnMouseMove(int mouseX, int mouseY)
    {
        if (getFocused() instanceof ContextMenu menu)
        {
            menu.resetFocusOnMouseMove(mouseX, mouseY);
        }
        else if (getFocused() instanceof MenuEntryButton button && !button.isMouseOver(mouseX, mouseY))
        {
            setFocused(null);
        }
    }

    public boolean open(int mouseX, int mouseY, ContextMenuProvider provider)
    {
        ContextMenuBuilderImpl menuBuilder = new ContextMenuBuilderImpl(this, mouseX, OptionalInt.empty(), mouseY);
        provider.fillRootMenu(menuBuilder);
        menuBuilder.build();
        if (!entries.isEmpty())
        {
            activeProvider = provider;
            open = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        reset();
        return false;
    }

    void openSubMenu(SubMenuKey subMenuKey, int leftX, int rightX, int y)
    {
        if (openSubMenu != null)
        {
            openSubMenu.menu.close();
        }

        ContextMenu menu = new ContextMenu(this);
        ContextMenuBuilderImpl menuBuilder = new ContextMenuBuilderImpl(menu, rightX, OptionalInt.of(leftX), y);
        activeProvider.fillSubMenu(menuBuilder, subMenuKey);
        menuBuilder.build();
        if (!menu.entries.isEmpty())
        {
            openSubMenu = new OpenSubMenu(subMenuKey, menu);
            setFocused(menu);
        }
    }

    public void close()
    {
        if (openSubMenu != null)
        {
            openSubMenu.menu.close();
        }
        reset();
        if (superMenu != null)
        {
            OpenSubMenu subMenu = superMenu.openSubMenu;
            Preconditions.checkState(subMenu != null && subMenu.menu == this);
            superMenu.openSubMenu = null;
            superMenu.setFocused(null);
        }
    }

    private void reset()
    {
        open = false;
        activeProvider = DummyProvider.INSTANCE;
        entries.clear();
        renderables.clear();
        lastMouseX = -1;
        lastMouseY = -1;
    }

    ContextMenu getRoot()
    {
        return superMenu != null ? superMenu.getRoot() : this;
    }

    @Nullable
    ContextMenu getOpenSubMenu(SubMenuKey key)
    {
        return openSubMenu != null && openSubMenu.subMenuKey == key ? openSubMenu.menu : null;
    }

    public boolean shouldKeepMenuOpen(int mouseX, int mouseY, boolean fromMouseClick)
    {
        if (superMenu == null && !fromMouseClick) return true;
        if (superMenu != null && getFocused() != null && !fromMouseClick) return true;
        if (isMouseOver(mouseX, mouseY)) return true;
        return openSubMenu != null && openSubMenu.menu.shouldKeepMenuOpen(mouseX, mouseY, fromMouseClick);
    }

    void setLayout(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public List<? extends GuiEventListener> children()
    {
        return entries;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (super.mouseClicked(event, doubleClick))
        {
            if (getFocused() instanceof DropFocusAfterClick)
            {
                setFocused(null);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (getFocused() instanceof ContextMenu menu)
        {
            return menu.keyPressed(event);
        }

        ArrowKey.Direction arrow = ArrowKey.Direction.of(event.key());
        return arrow != null ? handleArrowKey(arrow) : super.keyPressed(event);
    }

    private boolean handleArrowKey(ArrowKey.Direction arrow)
    {
        return switch (arrow)
        {
            case UP -> focusNextButton(-1);
            case DOWN -> focusNextButton(1);
            case LEFT -> tryCloseSubMenu();
            case RIGHT -> tryOpenSubMenu();
        };
    }

    private boolean focusNextButton(int offset)
    {
        if (getFocused() instanceof MenuEntryButton button && button.owner == this)
        {
            int idx = entries.indexOf(button);
            int nextIdx = Mth.positiveModulo(idx + offset, entries.size());
            setFocused(entries.get(nextIdx));
            return true;
        }
        else if (getFocused() == null)
        {
            setFocused(offset > 0 ? entries.getFirst() : entries.getLast());
            return true;
        }
        return false;
    }

    private boolean tryOpenSubMenu()
    {
        if (getFocused() instanceof MenuEntryButton button && button.opensSubMenuOn(this) && openSubMenu == null)
        {
            button.onPress(new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_1, 0));
            if (openSubMenu != null)
            {
                MenuEntryButton first = openSubMenu.menu.entries.getFirst();
                openSubMenu.menu.setFocused(first);
            }
            return true;
        }
        return false;
    }

    private boolean tryCloseSubMenu()
    {
        if (superMenu != null && superMenu.openSubMenu != null)
        {
            SubMenuKey subMenuKey = superMenu.openSubMenu.subMenuKey;
            MenuEntryButton button = superMenu.entries
                    .stream()
                    .filter(btn -> btn.opensSubMenu(subMenuKey))
                    .findFirst()
                    .orElseThrow();
            close();
            superMenu.setFocused(button);
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY)
    {
        if (!open)
        {
            return false;
        }
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height)
        {
            return true;
        }
        return openSubMenu != null && openSubMenu.menu.isMouseOver(mouseX, mouseY);
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY)
    {
        Optional<GuiEventListener> child = super.getChildAt(mouseX, mouseY);
        if (child.isEmpty() && openSubMenu != null)
        {
            child = openSubMenu.menu.getChildAt(mouseX, mouseY);
        }
        return child;
    }

    private record OpenSubMenu(SubMenuKey subMenuKey, ContextMenu menu) { }
}
