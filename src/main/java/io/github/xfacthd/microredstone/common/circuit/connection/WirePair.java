package io.github.xfacthd.microredstone.common.circuit.connection;

/**
 * Defines a connection crossing the boundary from one circuit node into a nested circuit node
 *
 * @param external The wire index in the surrounding circuit node
 * @param internal The wire index in the nested circuit node
 */
public record WirePair(int external, int internal)
{
    public WirePair(int wire)
    {
        this(wire, wire);
    }
}
