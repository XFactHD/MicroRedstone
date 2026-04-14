package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;

public interface FieldCollector {
    void buffer(BufferCircuitNode buffer);

    void clock(ClockCircuitNode clock);
}
