package io.github.xfacthd.microredstone.common.circuit.prototype.spec;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import net.minecraft.Optionull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class PortConfig
{
    private final Map<Port, PortEntry> ports;
    private final int portMask;

    private PortConfig(Map<Port, PortEntry> ports, int portMask)
    {
        this.ports = ports;
        this.portMask = portMask;
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

    public boolean isRequired(PrototypeNode node, Port port)
    {
        PortEntry entry = ports.get(port);
        return entry != null && entry.required.test(node);
    }

    @Nullable
    public WireType getPortType(Port port)
    {
        return Optionull.map(ports.get(port), PortEntry::type);
    }

    @Nullable
    public PortDir getPortDir(Port port)
    {
        return Optionull.map(ports.get(port), PortEntry::dir);
    }

    public Stream<Port> getPortsWithDir(PortDir dir)
    {
        return ports.entrySet()
                .stream()
                .filter(e -> e.getValue().dir == dir)
                .map(Map.Entry::getKey);
    }

    public int getPortMask()
    {
        return portMask;
    }

    public static <T extends PrototypeNode> Builder<T> builder()
    {
        return new Builder<>();
    }

    private record PortEntry(WireType type, PortDir dir, Predicate<PrototypeNode> required) {}

    public static final class Builder<T extends PrototypeNode>
    {
        private final Map<Port, PortEntry> ports = new EnumMap<>(Port.class);
        private int portMask = 0;

        private Builder() {}

        public Builder<T> addPort(Port port, WireType type, PortDir dir)
        {
            return addPort(port, type, dir, node -> true);
        }

        public Builder<T> addOptionalPort(Port port, WireType type, PortDir dir)
        {
            return addPort(port, type, dir, node -> false);
        }

        @SuppressWarnings("unchecked")
        public Builder<T> addPredicatedPort(Port port, WireType type, PortDir dir, Predicate<T> required)
        {
            return addPort(port, type, dir, (Predicate<PrototypeNode>) required);
        }

        private Builder<T> addPort(Port port, WireType type, PortDir dir, Predicate<PrototypeNode> required)
        {
            if (ports.putIfAbsent(port, new PortEntry(type, dir, required)) != null)
            {
                throw new IllegalStateException("Duplicate port declaration: " + port);
            }
            portMask = port.appendMask(portMask, type);
            return this;
        }

        public PortConfig build()
        {
            return new PortConfig(ports, portMask);
        }
    }
}
