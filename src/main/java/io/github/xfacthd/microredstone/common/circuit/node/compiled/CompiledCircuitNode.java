package io.github.xfacthd.microredstone.common.circuit.node.compiled;

import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;

public abstract class CompiledCircuitNode extends CircuitNode
{
    private final CompoundCircuitNode originalNode;
    protected final EvalContext.Nested nestedContext;

    CompiledCircuitNode(CompoundCircuitNode originalNode, int wireCount, Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
        this.originalNode = originalNode;
        this.nestedContext = new EvalContext.Nested(wireCount);
    }

    public CompoundCircuitNode getOriginalNode()
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
