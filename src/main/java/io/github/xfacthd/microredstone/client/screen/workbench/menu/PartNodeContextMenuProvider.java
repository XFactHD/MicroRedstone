package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;

public sealed class PartNodeContextMenuProvider<T extends PrototypeNode> extends BaseNodeContextMenuProvider<T>
        permits ClockPartNodeContextMenuProvider, ConverterPartNodeContextMenuProvider
{
    public PartNodeContextMenuProvider(CircuitCanvas canvas, T node)
    {
        super(canvas, node);
    }
}
