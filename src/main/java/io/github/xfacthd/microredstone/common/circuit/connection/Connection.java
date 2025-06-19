package io.github.xfacthd.microredstone.common.circuit.connection;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Connection
{
    private PortDir portDir = PortDir.INPUT;
    private WireType wireType = WireType.SINGLE;
    @Nullable
    private Wire wire;

    public void connect(@Nullable Wire wire)
    {
        this.wire = wire;
    }

    public void setPortDir(PortDir portDir)
    {
        this.portDir = portDir;
    }

    public void setWireType(WireType wireType)
    {
        this.wireType = wireType;
    }

    public PortDir getPortDir()
    {
        return portDir;
    }

    public WireType getWireType()
    {
        return wireType;
    }

    public Wire getWire()
    {
        return Objects.requireNonNull(wire);
    }

    public void validate(ProblemReporter conReporter)
    {
        if (wire == null) conReporter.report(() -> "Connection unspecified");
    }

    public Connector toConnector(Port port, WireMapper wireMapper)
    {
        int resolveWire = wireMapper.resolveWire(Objects.requireNonNull(wire));
        return new Connector(port, resolveWire, portDir, wireType);
    }

    @Override
    public String toString()
    {
        return "Connector[dir=" + portDir + ",type=" + wireType + "]";
    }
}
