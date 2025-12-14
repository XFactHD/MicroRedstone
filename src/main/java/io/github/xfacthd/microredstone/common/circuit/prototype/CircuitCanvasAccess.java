package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.client.screen.workbench.ExactNodePos;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import org.jspecify.annotations.Nullable;

public interface CircuitCanvasAccess
{
    @Nullable
    NodePos getNodePos(int mouseX, int mouseY);

    @Nullable
    ExactNodePos getExactNodePos(double mouseX, double mouseY);

    @Nullable
    PlaceableNode getPartNode(NodePos pos);

    boolean isValidPos(NodePos pos);
}
