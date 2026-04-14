package io.github.xfacthd.microredstone.common.circuit.prototype.spec;

import io.github.xfacthd.microredstone.client.util.PortOverlays;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record IconConfig(Identifier icon, @Nullable Identifier portOverlay, boolean rotateTexture) {
    public static IconConfig of(Identifier icon, PortConfig portConfig) {
        return of(icon, portConfig, true);
    }

    public static IconConfig of(Identifier icon, PortConfig portConfig, boolean rotateTexture) {
        return new IconConfig(icon, PortOverlays.get(portConfig.getPortMask()), rotateTexture);
    }
}
