package io.github.xfacthd.microredstone.common.circuit.node.compiled;

import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;

public abstract class CompiledCircuitNode extends RootCircuitNode
{
    private final CompoundCircuitNode originalNode;

    CompiledCircuitNode(CompoundCircuitNode originalNode, Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
        this.originalNode = originalNode;
    }

    @Override
    public CompoundCircuitNode serializable()
    {
        return originalNode;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        // CompiledCircuitNodes can't be serialized directly, they must be unwrapped first
        throw new UnsupportedOperationException();
    }
}
