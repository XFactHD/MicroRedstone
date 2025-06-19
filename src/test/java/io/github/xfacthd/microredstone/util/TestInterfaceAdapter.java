package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.ExternalInterfaceAdapter;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;

public final class TestInterfaceAdapter implements ExternalInterfaceAdapter
{
    private final short[] values = new short[4];

    @Override
    public short read(int input)
    {
        return values[input];
    }

    @Override
    public void write(int output, short value)
    {
        values[output] = value;
    }

    public int setValue(Port port, int value)
    {
        values[port.ordinal()] = (short) value;
        return value;
    }

    public int getValue(Port port)
    {
        return values[port.ordinal()] & 0xFFFF;
    }
}
