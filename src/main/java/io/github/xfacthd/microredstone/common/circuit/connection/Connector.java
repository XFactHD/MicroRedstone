package io.github.xfacthd.microredstone.common.circuit.connection;

public record Connector(Port port, int wire, PortDir dir, WireType type) { }
