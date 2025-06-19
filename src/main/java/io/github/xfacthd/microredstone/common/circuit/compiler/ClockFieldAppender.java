package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;

public interface ClockFieldAppender
{
    ClockCircuitNode.Fields addNewField(boolean needCounter, int counterInit);
}
