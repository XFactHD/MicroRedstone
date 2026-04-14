package io.github.xfacthd.microredstone.common.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public sealed class ToggleableSlot extends Slot permits WorkbenchCircuitSlot {
    private boolean active = true;

    public ToggleableSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
