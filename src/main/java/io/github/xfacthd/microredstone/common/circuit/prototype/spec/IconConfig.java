package io.github.xfacthd.microredstone.common.circuit.prototype.spec;

import io.github.xfacthd.microredstone.client.util.PortOverlays;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record IconConfig(ResourceLocation icon, @Nullable ResourceLocation portOverlay, boolean rotateTexture)
{
    public static IconConfig of(ResourceLocation icon, PortConfig portConfig)
    {
        return of(icon, portConfig, true);
    }

    public static IconConfig of(ResourceLocation icon, PortConfig portConfig, boolean rotateTexture)
    {
        return new IconConfig(icon, PortOverlays.get(portConfig.getPortMask()), rotateTexture);
    }
}
