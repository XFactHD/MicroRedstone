package io.github.xfacthd.microredstone.common.menu.slot;

import io.github.xfacthd.microredstone.common.MRContent;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class WorkbenchCircuitSlot extends ToggleableSlot
{
    public WorkbenchCircuitSlot(Container container, int slot, int x, int y)
    {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return stack.is(MRContent.ITEM_INTEGRATED_CIRCUIT);
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }
}
