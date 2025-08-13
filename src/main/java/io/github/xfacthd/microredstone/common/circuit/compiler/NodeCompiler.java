package io.github.xfacthd.microredstone.common.circuit.compiler;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

public interface NodeCompiler
{
    void compile(GeneratorAdapter generator, Type selfType, FieldAppender fieldAppender, LocalWireMapper localWires);
}
