package io.github.xfacthd.microredstone.common.circuit.node.compiled;

import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;

public abstract class CompiledCircuitNode extends RootCircuitNode
{
    private final CompoundCircuitNode originalNode;
    protected final EvalContext.Nested nestedContext;

    CompiledCircuitNode(CompoundCircuitNode originalNode, int wireCount, Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
        this.originalNode = originalNode;
        this.nestedContext = new EvalContext.Nested(wireCount);
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
