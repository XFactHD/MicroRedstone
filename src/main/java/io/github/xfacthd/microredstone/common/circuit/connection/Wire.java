package io.github.xfacthd.microredstone.common.circuit.connection;

public final class Wire
{
    private final WireType wireType;

    public Wire(WireType wireType)
    {
        this.wireType = wireType;
    }

    public WireType getWireType()
    {
        return wireType;
    }

    @Override
    public String toString()
    {
        return "Wire[type=" + wireType + "]";
    }
}
