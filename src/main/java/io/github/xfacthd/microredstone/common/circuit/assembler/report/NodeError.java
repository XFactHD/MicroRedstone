package io.github.xfacthd.microredstone.common.circuit.assembler.report;

import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import net.minecraft.network.chat.Component;

import java.util.Set;

public sealed interface NodeError extends CircuitError
{
    PlaceableNode node();

    sealed interface PortError extends NodeError
    {
        Port port();
    }

    non-sealed interface TypeSpecificError extends NodeError {}

    record DirectCycle(PrototypeNode node) implements NodeError
    {
        @Override
        public Component description()
        {
            return Component.literal("Immediate cyclic connection on " + node);
        }
    }

    record IndirectCycle(Set<PrototypeNode> nodes) implements NodeError
    {
        @Override
        public PrototypeNode node()
        {
            return nodes.iterator().next();
        }

        @Override
        public Component description()
        {
            StringBuilder builder = new StringBuilder();
            for (PrototypeNode node : nodes)
            {
                builder.append(node).append("->");
            }
            builder.append(nodes.iterator().next());
            return Component.literal("Indirect cyclic connection: " + builder);
        }
    }

    record UnknownWires(PrototypeNode node, Set<Wire> wires) implements NodeError
    {
        @Override
        public Component description()
        {
            return Component.literal("PrototypeNode " + node + " is connected to unknown wires " + wires);
        }
    }

    record UnexpectedConnection(PrototypeNode node, Port port) implements PortError
    {
        @Override
        public Component description()
        {
            return Component.literal("PrototypeNode " + node + " has unexpected connection on port " + port);
        }
    }

    record MissingConnection(PrototypeNode node, Port port, PortDir dir) implements PortError
    {
        @Override
        public Component description()
        {
            return Component.literal("PrototypeNode " + node + " is missing " + dir + " connection on port " + port);
        }
    }

    record MismatchedConnection(PlaceableNode node, Port port, WireType portType, WireType wireType) implements PortError
    {
        @Override
        public Component description()
        {
            return Component.literal("PrototypeNode " + node + " has wire with incorrect type " + wireType + " connected on port " + port + " , expected type " + portType);
        }
    }

    record DanglingConnection(Connection node) implements NodeError
    {
        @Override
        public Component description()
        {
            return Component.literal("Connection " + node + " is unconnected");
        }
    }

    record InvalidClockPeriod(ClockPrototypeNode node, int halfPeriod) implements TypeSpecificError
    {
        @Override
        public Component description()
        {
            return Component.literal("ClockPrototypeNode " + node + " has invalid period: " + (halfPeriod * 2));
        }
    }

    record InvalidConstantValue(ConstantPrototypeNode node, int value) implements TypeSpecificError
    {
        @Override
        public Component description()
        {
            return Component.literal("ConstantPrototypeNode " + node + " has invalid value: " + value);
        }
    }

    record InvalidConverterBit(ConverterPrototypeNode node, int bitIndex) implements TypeSpecificError
    {
        @Override
        public Component description()
        {
            return Component.literal("ConverterPrototypeNode " + node + " has invalid bit index: " + bitIndex);
        }
    }
}
