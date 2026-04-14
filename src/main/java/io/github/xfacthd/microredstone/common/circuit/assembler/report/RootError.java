package io.github.xfacthd.microredstone.common.circuit.assembler.report;

import net.minecraft.network.chat.Component;

public sealed interface RootError extends CircuitError {
    record UnexpectedError(Throwable error) implements RootError {
        @Override
        public Component description() {
            return Component.literal("Unexpected error: " + error.getMessage());
        }
    }

    record WireCountMismatch(int knownCount, int mappedCount) implements RootError {
        @Override
        public Component description() {
            return Component.literal("Wire count mismatch (known: " + knownCount + ", mapped: " + mappedCount + ")");
        }
    }
}
