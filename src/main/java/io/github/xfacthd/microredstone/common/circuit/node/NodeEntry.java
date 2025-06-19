package io.github.xfacthd.microredstone.common.circuit.node;

import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;

/**
 * @param node    The circuit node
 * @param inputs  The mapping between the internal ioPorts of the node and the ioPorts of the surrounding node for inputs
 * @param outputs The mapping between the internal ioPorts of the node and the ioPorts of the surrounding node for outputs
 */
public record NodeEntry<T extends CircuitNode>(T node, WirePair[] inputs, WirePair[] outputs)
{
    public void evaluate(EvalContext context)
    {
        node.evaluate(context, inputs, outputs);
    }
}
