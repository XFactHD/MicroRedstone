package io.github.xfacthd.microredstone.common.circuit.node.compiled;

import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;

public abstract class CompiledCircuitNode extends RootCircuitNode
{
    private final CompoundCircuitNode originalNode;
    private final Runnable releaser;
    private final int wireCount;

    CompiledCircuitNode(CompoundCircuitNode originalNode, Runnable releaser, Connector[] inputs, Connector[] outputs)
    {
        super(inputs, outputs);
        this.originalNode = originalNode;
        this.releaser = releaser;
        this.wireCount = originalNode.getWireCount();
    }

    @Override
    public final int getWireCount()
    {
        return wireCount;
    }

    @Override
    public final CompoundCircuitNode serializable()
    {
        return originalNode;
    }

    @Override
    public final void release()
    {
        releaser.run();
    }

    @Override
    public final CircuitNodeType<? extends CircuitNode> type()
    {
        // CompiledCircuitNodes can't be serialized directly, they must be unwrapped first
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj;
    }

    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }
}
