package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;

public final class TestBuilder
{
    private final CompoundPrototypeNode cmpNode = new CompoundPrototypeNode();

    public Wire addWire(WireType wireType)
    {
        Wire wire = new Wire(wireType);
        cmpNode.addWire(wire);
        return wire;
    }

    public Connection addConnection(Port port, WireType wireType, PortDir portDir)
    {
        Connection connection = new Connection(wireType);
        connection.setPortDir(portDir);
        cmpNode.setConnection(port, connection);
        return connection;
    }

    public <T extends PrototypeNode> T addNode(T node)
    {
        cmpNode.addChild(node);
        return node;
    }

    public CompoundPrototypeNode build()
    {
        return cmpNode;
    }
}
