package io.github.xfacthd.microredstone.common.circuit.prototype;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import org.jetbrains.annotations.Nullable;

public interface PlaceableNode
{
    NodePos getPos();

    int getRotation();

    IconConfig getIcon();

    boolean hasPort(Port port, @Nullable WireType wireType);

    boolean isConnected(Port port);
}
