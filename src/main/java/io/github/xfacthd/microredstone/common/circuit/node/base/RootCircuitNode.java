package io.github.xfacthd.microredstone.common.circuit.node.base;

import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.WireStates;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public non-sealed abstract class RootCircuitNode extends CircuitNode
{
    protected RootCircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        super(inputs, outputs);
    }

    protected RootCircuitNode(Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
    }

    @Override
    public final void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        evaluate(context, inputs, outputs, null);
    }

    public abstract void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs, @Nullable WireStates wireStates);

    public abstract int getWireCount();

    public abstract CompoundCircuitNode serializable();

    public abstract CircuitState serializeState();

    public abstract void applyState(CircuitState state);

    public abstract void release();
}
