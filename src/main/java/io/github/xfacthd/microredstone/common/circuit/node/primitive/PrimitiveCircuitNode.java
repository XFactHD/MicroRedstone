package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public abstract class PrimitiveCircuitNode extends CircuitNode
{
    protected PrimitiveCircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        super(inputs, outputs);
    }

    public abstract void compile(GeneratorAdapter methodGen, LocalWireMapper localWires);
}
