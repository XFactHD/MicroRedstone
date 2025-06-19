package io.github.xfacthd.microredstone.common.circuit.node;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;

import java.util.List;

public abstract class CircuitNode
{
    protected final Connector[] inputs;
    protected final Connector[] outputs;

    protected CircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        this.inputs = inputs.toArray(Connector[]::new);
        this.outputs = outputs.toArray(Connector[]::new);
    }

    protected CircuitNode(Connector[] inputs, Connector[] outputs)
    {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public abstract void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs);

    /**
     * {@return the internal indices of ioPorts used as inputs}
     */
    public final Connector[] getInputs()
    {
        return inputs;
    }

    /**
     * {@return the internal indices of ioPorts used as outputs}
     */
    public final Connector[] getOutputs()
    {
        return outputs;
    }
}
