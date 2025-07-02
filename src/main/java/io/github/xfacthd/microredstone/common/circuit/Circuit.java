package io.github.xfacthd.microredstone.common.circuit;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;

import java.util.Arrays;

public final class Circuit
{
    public static final Codec<Circuit> CODEC = CircuitNode.CODEC.xmap(Circuit::new, Circuit::getRootNodeForSerialization);

    private final CircuitNode rootNode;
    private final WirePair[] inputs;
    private final WirePair[] outputs;
    private final EvalContext.Root evalContext;

    public Circuit(CircuitNode rootNode)
    {
        Preconditions.checkArgument(
                rootNode instanceof CompoundCircuitNode || rootNode instanceof CompiledCircuitNode,
                "Circuit can only contain compound and compiled circuit nodes"
        );
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

    public CompoundCircuitNode getRootNodeForSerialization()
    {
        if (rootNode instanceof CompiledCircuitNode compiled)
        {
            return compiled.getOriginalNode();
        }
        return (CompoundCircuitNode) rootNode;
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
