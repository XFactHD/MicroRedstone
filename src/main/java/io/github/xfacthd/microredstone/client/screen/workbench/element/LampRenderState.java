package io.github.xfacthd.microredstone.client.screen.workbench.element;

import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeColor;

public record LampRenderState(NodePos pos, int rotation, int inputWire, DyeColor color, boolean chainedToNeighbor, boolean powered, int packedColor) {
    private static final int UNPOWERED_ALPHA = 0xAA;

    public LampRenderState(NodePos pos, int rotation, int inputWire, DyeColor color, boolean chainedToNeighbor) {
        this(pos, rotation, inputWire, color, chainedToNeighbor, false, computeColor(color, false));
    }

    public LampRenderState(LampPrototypeNode node, boolean chainedToNeighbor) {
        this(node.getPos(), node.getRotation(), -1, node.getColor(), chainedToNeighbor);
    }

    public LampRenderState withPowered(boolean powered) {
        if (powered == this.powered) {
            return this;
        }
        return new LampRenderState(pos, rotation, inputWire, color, chainedToNeighbor, powered, computeColor(color, powered));
    }

    private static int computeColor(DyeColor color, boolean powered) {
        if (powered) {
            return color.getTextColor();
        }
        return ARGB.color(UNPOWERED_ALPHA, color.getTextureDiffuseColor());
    }
}
