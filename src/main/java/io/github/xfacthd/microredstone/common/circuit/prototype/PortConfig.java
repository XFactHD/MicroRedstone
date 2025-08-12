package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import net.minecraft.Optionull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

public final class PortConfig
{
    private final Map<Port, PortEntry> ports;

    private PortConfig(Map<Port, PortEntry> ports)
    {
        this.ports = ports;
    }

    public boolean hasPort(Port port)
    {
        return ports.containsKey(port);
    }

    public boolean hasPort(Port port, @Nullable WireType type)
    {
        PortEntry entry = ports.get(port);
        return entry != null && (type == null || entry.type == type);
    }

    @Nullable
    public WireType getPortType(Port port)
    {
        return Optionull.map(ports.get(port), PortEntry::type);
    }

    public Stream<Port> getPortsWithDir(PortDir dir)
    {
        return ports.entrySet()
                .stream()
                .filter(e -> e.getValue().dir == dir)
                .map(Map.Entry::getKey);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private record PortEntry(WireType type, PortDir dir, boolean optional) {}

    public static final class Builder
    {
        private final Map<Port, PortEntry> ports = new EnumMap<>(Port.class);

        private Builder() {}

        public Builder addPort(Port port, WireType type, PortDir dir)
        {
            return addPort(port, type, dir, false);
        }

        public Builder addOptionalPort(Port port, WireType type, PortDir dir)
        {
            return addPort(port, type, dir, true);
        }

        private Builder addPort(Port port, WireType type, PortDir dir, boolean optional)
        {
            if (ports.putIfAbsent(port, new PortEntry(type, dir, optional)) != null)
            {
                throw new IllegalStateException("Duplicate port declaration: " + port);
            }
            return this;
        }

        public PortConfig build()
        {
            return new PortConfig(ports);
        }
    }
}
