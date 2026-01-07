package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.base.LeafCircuitNode;

import java.lang.classfile.CodeBuilder;
import java.util.List;

public abstract class PrimitiveCircuitNode extends LeafCircuitNode
{
    protected PrimitiveCircuitNode(List<Connector> inputs, List<Connector> outputs)
    {
        super(inputs, outputs);
    }

    public abstract void compile(CodeBuilder mthBody, LocalWireMapper localWires);
}
