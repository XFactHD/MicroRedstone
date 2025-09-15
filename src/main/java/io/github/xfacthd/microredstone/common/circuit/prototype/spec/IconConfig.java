package io.github.xfacthd.microredstone.common.circuit.prototype.spec;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record IconConfig(ResourceLocation icon, @Nullable ResourceLocation portOverlay, boolean rotateTexture)
{
    public IconConfig(ResourceLocation icon, @Nullable ResourceLocation portOverlay)
    {
        this(icon, portOverlay, true);
    }
}
