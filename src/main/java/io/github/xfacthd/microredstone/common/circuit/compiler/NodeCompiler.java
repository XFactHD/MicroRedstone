package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public interface NodeCompiler
{
    void compile(EvalMethodCompiler compiler, GeneratorAdapter generator, Type selfType, FieldAppender fieldAppender, LocalWireMapper localWires, CompoundCircuitNode node);
}
