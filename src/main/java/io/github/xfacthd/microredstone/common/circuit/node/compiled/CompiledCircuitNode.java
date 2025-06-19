package io.github.xfacthd.microredstone.common.circuit.node.compiled;

import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;

public abstract class CompiledCircuitNode extends CircuitNode
{
    protected final EvalContext.Nested nestedContext;

    CompiledCircuitNode(int wireCount, Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
        this.nestedContext = new EvalContext.Nested(wireCount);
    }
}
