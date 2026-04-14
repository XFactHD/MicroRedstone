package io.github.xfacthd.microredstone.client.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public record Icon(Identifier texture, int color) {
    public Icon(Identifier texture) {
        this(texture, 0xFFFFFFFF);
    }

    public Icon withColor(@Nullable DyeColor color) {
        return withColor(color != null ? color.getTextureDiffuseColor() : 0xFFFFFFFF);
    }

    public Icon withColor(int color) {
        return color == this.color ? this : new Icon(texture, color);
    }
}
