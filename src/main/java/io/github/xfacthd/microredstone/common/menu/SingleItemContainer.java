package io.github.xfacthd.microredstone.common.menu;

import net.minecraft.world.SimpleContainer;

final class SingleItemContainer extends SimpleContainer {
    public SingleItemContainer() {
        super(1);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
