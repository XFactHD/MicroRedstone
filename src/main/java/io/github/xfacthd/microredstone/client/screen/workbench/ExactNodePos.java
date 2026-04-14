package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import net.minecraft.util.Mth;

public record ExactNodePos(NodePos pos, double fracX, double fracY) {
    public ExactNodePos(double x, double y) {
        this(new NodePos((int) x, (int) y), Mth.frac(x), Mth.frac(y));
    }
}
