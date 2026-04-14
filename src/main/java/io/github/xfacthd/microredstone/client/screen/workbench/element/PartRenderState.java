package io.github.xfacthd.microredstone.client.screen.workbench.element;

import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;

public record PartRenderState(NodePos pos, IconConfig icon, int rotation) {
    public PartRenderState(PlaceableNode node) {
        this(node.getPos(), node.getIcon(), node.getRotation());
    }
}
