package io.github.xfacthd.microredstone.common.circuit;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;

import java.util.Arrays;

// TODO: serialize retained state (clock counter and state, buffer state)
public final class Circuit
{
    public static final Codec<Circuit> CODEC = CompoundCircuitNode.CODEC.codec().xmap(Circuit::new, Circuit::getSerializableRootNode);

    private final RootCircuitNode rootNode;
    private final WirePair[] inputs;
    private final WirePair[] outputs;
    private final EvalContext.Root evalContext;

    public Circuit(RootCircuitNode rootNode)
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

    public CircuitNode getRootNode()
    {
        return rootNode;
    }

    public CompoundCircuitNode getSerializableRootNode()
    {
        return rootNode.serializable();
    }

    public Connector[] getInputs()
    {
        return rootNode.getInputs();
    }

    public Connector[] getOutputs()
    {
        return rootNode.getOutputs();
    }
}
