package io.github.xfacthd.microredstone.common.circuit;

@FunctionalInterface
public interface WireStateListener {
    void handleWireStates(WireStates wireStates);
}
