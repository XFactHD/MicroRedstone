package io.github.xfacthd.microredstone.client.util;

import com.mojang.datafixers.util.Pair;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ColorNames
{
    private static final Map<DyeColor, Component> COLOR_NAMES = Arrays.stream(DyeColor.values())
            .map(color -> Pair.of(color, Utils.translate("label", "dye_color." + color.getName())))
            .collect(Collectors.toMap(
                    Pair::getFirst,
                    Pair::getSecond,
                    (a, b) -> { throw new UnsupportedOperationException(); },
                    () -> new EnumMap<>(DyeColor.class)
            ));

    public static Component getName(DyeColor color)
    {
        return COLOR_NAMES.get(color);
    }

    private ColorNames() {}
}
