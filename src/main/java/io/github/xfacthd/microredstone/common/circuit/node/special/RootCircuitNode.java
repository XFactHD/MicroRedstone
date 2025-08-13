package io.github.xfacthd.microredstone.common.circuit.node.special;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;

import java.util.List;

public abstract class RootCircuitNode extends CircuitNode
{
    protected RootCircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        super(inputs, outputs);
    }

    protected RootCircuitNode(Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
    }

    public abstract CompoundCircuitNode serializable();
}
