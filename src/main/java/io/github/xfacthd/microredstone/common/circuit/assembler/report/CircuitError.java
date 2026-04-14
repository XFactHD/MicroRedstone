package io.github.xfacthd.microredstone.common.circuit.assembler.report;

import net.minecraft.network.chat.Component;

sealed interface CircuitError permits RootError, NodeError, WireError {
    Component description();
}
