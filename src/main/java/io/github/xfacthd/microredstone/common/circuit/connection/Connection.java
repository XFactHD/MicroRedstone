package io.github.xfacthd.microredstone.common.circuit.connection;

import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Connection implements PlaceableNode
{
    public static final IconConfig ICON_SINGLE_IN = new IconConfig(Utils.rl("part/connection_in"), Utils.rl("port/right_single"), false);
    private static final IconConfig ICON_SINGLE_OUT = new IconConfig(Utils.rl("part/connection_out"), Utils.rl("port/right_single"), false);
    public static final IconConfig ICON_BUNDLED_IN = new IconConfig(Utils.rl("part/connection_in"), Utils.rl("port/right_bundled"), false);
    private static final IconConfig ICON_BUNDLED_OUT = new IconConfig(Utils.rl("part/connection_out"), Utils.rl("port/right_bundled"), false);

    private final WireType wireType;
    private NodePos pos = new NodePos(0, 0);
    private int rotation = 0;
    private PortDir portDir = PortDir.INPUT;
    @Nullable
    private Wire wire;

    public Connection(WireType wireType)
    {
        this.wireType = wireType;
    }

    public void connect(@Nullable Wire wire)
    {
        this.wire = wire;
    }

    public void setPos(NodePos pos, int rotation)
    {
        this.pos = pos;
        this.rotation = rotation;
    }

    public void setPortDir(PortDir portDir)
    {
        this.portDir = portDir;
    }

    @Override
    public NodePos getPos()
    {
        return pos;
    }

    @Override
    public int getRotation()
    {
        return rotation;
    }

    @Override
    public IconConfig getIcon()
    {
        return switch (portDir)
        {
            case INPUT -> wireType.select(ICON_SINGLE_IN, ICON_BUNDLED_IN);
            case OUTPUT -> wireType.select(ICON_SINGLE_OUT, ICON_BUNDLED_OUT);
        };
    }

    @Override
    public boolean hasPort(Port port, @Nullable WireType wireType)
    {
        return port.rotate(-rotation) == Port.RIGHT && (wireType == null || wireType == this.wireType);
    }

    @Override
    public boolean isConnected(Port port)
    {
        return hasPort(port, wireType) && wire != null;
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
        return new Connector(pos, port, resolveWire, portDir, wireType);
    }

    @Override
    public String toString()
    {
        return "Connector[dir=" + portDir + ",type=" + wireType + "]";
    }
}
