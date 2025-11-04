package io.github.xfacthd.microredstone.common.circuit.assembler.report;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import net.minecraft.network.chat.Component;

public sealed interface WireError extends CircuitError
{
    Wire wire();

    record DriverCount(Wire wire, int drivers) implements WireError
    {
        @Override
        public Component description()
        {
            return Component.literal("Wire " + wire + " has incorrect amount of driving outputs: " + drivers);
        }
    }

    record MultipleBundlePackers(Wire wire, int bitIndex) implements WireError
    {
        @Override
        public Component description()
        {
            return Component.literal("Wire " + wire + " has multiple packers driving bit " + bitIndex);
        }
    }
}
