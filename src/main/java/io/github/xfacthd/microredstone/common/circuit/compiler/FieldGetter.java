package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;

public interface FieldGetter {
    String buffer(BufferCircuitNode buffer);

    ClockCircuitNode.Fields clock(ClockCircuitNode clock);
}
