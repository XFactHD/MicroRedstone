package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;

public interface FieldAppender
{
    String getOrAddBufferField(BufferCircuitNode buffer);

    ClockCircuitNode.Fields addClockField(boolean needCounter, int counterInit);
}
