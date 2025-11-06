package io.github.xfacthd.microredstone.common.circuit.node.base;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;

import java.util.BitSet;
import java.util.List;

public non-sealed abstract class LeafCircuitNode extends CircuitNode
{
    protected LeafCircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        super(inputs, outputs);
    }

    public abstract boolean validate(NodeEntry<?> entry, BitSet wires, int wireCount);
}
