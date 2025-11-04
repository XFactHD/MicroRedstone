package io.github.xfacthd.microredstone.common.circuit.connection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.assembler.WireMapper;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.NodeError;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    private String name = "";

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

    public void setName(String name)
    {
        this.name = name;
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

    public void removeWire(Wire wire)
    {
        if (this.wire == wire)
        {
            this.wire = null;
        }
    }

    public PortDir getPortDir()
    {
        return portDir;
    }

    public String getName()
    {
        return name;
    }

    public WireType getWireType()
    {
        return wireType;
    }

    public Wire getWire()
    {
        return Objects.requireNonNull(wire);
    }

    public boolean validate(CircuitErrorCollector errors)
    {
        if (wire == null)
        {
            errors.submit(new NodeError.DanglingConnection(this));
            return false;
        }
        if (wire.getWireType() != wireType)
        {
            errors.submit(new NodeError.MismatchedConnection(this, Port.RIGHT, wireType, wire.getWireType()));
        }
        return true;
    }

    public Connector toConnector(Port port, WireMapper wireMapper)
    {
        int resolveWire = wireMapper.resolveWire(Objects.requireNonNull(wire));
        return new Connector(pos, port, resolveWire, portDir, wireType, name);
    }

    @Override
    public String toString()
    {
        return "Connector[dir=" + portDir + ",type=" + wireType + ",desc=" + name + "]";
    }

    public Serializable serialize(Port port, List<Wire> wires)
    {
        return new Serializable(port, wireType, portDir, pos, rotation, wire != null ? wires.indexOf(wire) : -1, name);
    }

    public record Serializable(Port port, WireType wireType, PortDir dir, NodePos pos, int rotation, int wireIdx, String name)
    {
        public static final Codec<Serializable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Port.CODEC.fieldOf("port").forGetter(Serializable::port),
                WireType.CODEC.fieldOf("type").forGetter(Serializable::wireType),
                PortDir.CODEC.fieldOf("dir").forGetter(Serializable::dir),
                NodePos.CODEC.fieldOf("pos").forGetter(Serializable::pos),
                Codec.intRange(0, 3).fieldOf("rotation").forGetter(Serializable::rotation),
                Codec.intRange(-1, Integer.MAX_VALUE).fieldOf("wire").forGetter(Serializable::wireIdx),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Serializable::name)
        ).apply(inst, Serializable::new));

        public Connection build(List<Wire> wires)
        {
            Wire wire = Objects.requireNonNull(wires.get(wireIdx));
            Connection connection = new Connection(wireType);
            connection.pos = pos;
            connection.rotation = rotation;
            connection.portDir = dir;
            connection.wire = wire;
            connection.name = name;
            return connection;
        }
    }
}
