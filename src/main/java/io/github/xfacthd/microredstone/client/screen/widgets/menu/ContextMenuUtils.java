package io.github.xfacthd.microredstone.client.screen.widgets.menu;

import io.github.xfacthd.microredstone.client.util.ColorNames;
import net.minecraft.world.item.DyeColor;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ContextMenuUtils {
    private static final DyeColor[] COLORS = DyeColor.values();

    public static void makeDyeColorMenu(ContextMenuBuilder menuBuilder, Consumer<DyeColor> setter, Supplier<DyeColor> getter) {
        makeDyeColorMenu(menuBuilder, setter, getter, Set.of());
    }

    public static void makeDyeColorMenu(ContextMenuBuilder menuBuilder, Consumer<DyeColor> setter, Supplier<DyeColor> getter, Set<DyeColor> excluded) {
        for (DyeColor color : COLORS) {
            if (excluded.contains(color)) {
                continue;
            }

            menuBuilder.addActionEntry(ColorNames.getName(color), entry -> entry
                    .withAction(() -> setter.accept(color))
                    .withStateSupplier(() -> getter.get() == color)
            );
        }
    }

    private ContextMenuUtils() { }
}
