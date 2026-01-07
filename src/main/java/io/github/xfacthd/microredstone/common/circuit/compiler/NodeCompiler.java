package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;

public interface NodeCompiler
{
    void compile(EvalMethodCompiler compiler, CodeBuilder mthBody, ClassDesc selfType, FieldGetter fieldGetter, LocalWireMapper localWires, CompoundCircuitNode node);
}
