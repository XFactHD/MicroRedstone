package io.github.xfacthd.microredstone.common.circuit.prototype;

import net.minecraft.resources.ResourceLocation;

public record IconConfig(ResourceLocation icon, ResourceLocation portOverlay, boolean rotateTexture)
{
    public IconConfig(ResourceLocation icon, ResourceLocation portOverlay)
    {
        this(icon, portOverlay, true);
    }
}
