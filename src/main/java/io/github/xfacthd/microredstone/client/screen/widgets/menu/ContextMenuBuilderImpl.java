package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class ContextMenuBuilderImpl implements ContextMenuBuilder
{
    private final ContextMenu owner;
    private final int desiredX;
    private final OptionalInt fallbackX;
    private final int desiredY;
    private final Set<SubMenuKey> encounteredSubMenus = new HashSet<>();

    ContextMenuBuilderImpl(ContextMenu owner, int desiredX, OptionalInt fallbackX, int desiredY)
    {
        this.owner = owner;
        this.desiredX = desiredX;
        this.fallbackX = fallbackX;
        this.desiredY = desiredY;
    }

    @Override
    public ContextMenuBuilder addActionEntry(Component text, Runnable action)
    {
        return addActionEntry(text, action, Optional.empty());
    }

    @Override
    public ContextMenuBuilder addActionEntry(Component text, Runnable action, BooleanSupplier stateSupplier)
    {
        return addActionEntry(text, action, Optional.of(stateSupplier));
    }

    private ContextMenuBuilder addActionEntry(Component text, Runnable action, Optional<BooleanSupplier> stateSupplier)
    {
        addEntry(MenuEntryButton.create(owner, text, action, stateSupplier));
        return this;
    }

    @Override
    public ContextMenuBuilder addSubMenuEntry(Component text, SubMenuKey subMenuKey)
    {
        if (!encounteredSubMenus.add(subMenuKey))
        {
            throw new IllegalStateException("Duplicate sub-menu: " + subMenuKey);
        }
        addEntry(MenuEntryButton.create(owner, text, subMenuKey));
        return this;
    }

    @Override
    public ContextMenuBuilder addSeparator()
    {
        addEntry(new Separator());
        return this;
    }

    private void addEntry(MenuEntry entry)
    {
        owner.renderables.add(entry);
        if (entry instanceof MenuEntryButton button)
        {
            owner.entries.add(button);
        }
    }

    void build()
    {
        int maxWidth = ContextMenu.MIN_WIDTH;
        int totalHeight = 0;
        for (MenuEntry entry : owner.renderables)
        {
            totalHeight += entry.getHeight();
            maxWidth = Math.max(maxWidth, entry.getRequiredWidth());
        }

        int menuWidth = maxWidth + 2;
        int menuHeight = totalHeight + 2;
        int menuX = computeMenuX(menuWidth);
        int menuY = computeMenuY(menuHeight);
        owner.setLayout(menuX, menuY, menuWidth, menuHeight);

        int lastBottomY = menuY + 1;
        for (MenuEntry entry : owner.renderables)
        {
            entry.setLayout(menuX + 1, lastBottomY, maxWidth);
            lastBottomY += entry.getHeight();
        }
    }

    private int computeMenuX(int maxWidth)
    {
        if (desiredX + maxWidth <= owner.owner.width)
        {
            return desiredX;
        }
        if (fallbackX.isPresent())
        {
            return fallbackX.getAsInt() - maxWidth;
        }
        return owner.owner.width - maxWidth;
    }

    private int computeMenuY(int totalHeight)
    {
        if (desiredY + totalHeight <= owner.owner.height)
        {
            return desiredY;
        }
        return owner.owner.height - totalHeight;
    }
}
