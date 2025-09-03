package io.github.xfacthd.microredstone.client.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

public record Icon(ResourceLocation texture, int color)
{
    public Icon(ResourceLocation texture)
    {
        this(texture, 0xFFFFFFFF);
    }

    public Icon withColor(@Nullable DyeColor color)
    {
        return withColor(color != null ? color.getTextureDiffuseColor() : 0xFFFFFFFF);
    }

    public Icon withColor(int color)
    {
        return color == this.color ? this : new Icon(texture, color);
    }
}
