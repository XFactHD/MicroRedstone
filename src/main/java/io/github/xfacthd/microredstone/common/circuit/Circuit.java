package io.github.xfacthd.microredstone.common.circuit;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;

import java.util.Arrays;

public final class Circuit
{
    private final CircuitNode rootNode;
    private final WirePair[] inputs;
    private final WirePair[] outputs;
    private final EvalContext.Root evalContext;

    public Circuit(CircuitNode rootNode)
    {
        this.rootNode = rootNode;
        this.inputs = Arrays.stream(rootNode.getInputs())
                .map(con -> new WirePair(con.port().ordinal(), con.wire()))
                .toArray(WirePair[]::new);
        this.outputs = Arrays.stream(rootNode.getOutputs())
                .map(con -> new WirePair(con.port().ordinal(), con.wire()))
                .toArray(WirePair[]::new);
        this.evalContext = new EvalContext.Root(inputs, outputs);
    }

    public void evaluate(ExternalInterfaceAdapter adapter)
    {
        evalContext.prepare(adapter);
        rootNode.evaluate(evalContext, inputs, outputs);
        evalContext.flush(adapter);
    }
}
