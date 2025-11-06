package io.github.xfacthd.microredstone.common.circuit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class Circuit
{
    public static final Codec<Circuit> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            CompoundCircuitNode.CODEC.codec().fieldOf("root_node").forGetter(Circuit::getSerializableRootNode),
            CircuitState.CODEC.fieldOf("state").forGetter(Circuit::serializeState)
    ).apply(inst, Circuit::new));

    private final RootCircuitNode rootNode;
    private final WirePair[] inputs;
    private final WirePair[] outputs;
    private final EvalContext.Root evalContext;

    private Circuit(RootCircuitNode rootNode, CircuitState state)
    {
        this(rootNode);
        rootNode.applyState(state);
    }

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

    public void evaluate(ExternalInterfaceAdapter adapter, @Nullable WireStates wireStates)
    {
        evalContext.prepare(adapter);
        rootNode.evaluate(evalContext, inputs, outputs, wireStates);
        evalContext.flush(adapter);
    }

    public Circuit replaceRootNode(CompiledCircuitNode compiled)
    {
        return new Circuit(compiled, serializeState());
    }

    private CircuitState serializeState()
    {
        return rootNode.serializeState();
    }

    public RootCircuitNode getRootNode()
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

    public void release()
    {
        rootNode.release();
    }
}
