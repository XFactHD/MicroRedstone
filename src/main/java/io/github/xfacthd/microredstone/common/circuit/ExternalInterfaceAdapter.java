package io.github.xfacthd.microredstone.common.circuit;

public interface ExternalInterfaceAdapter {
    short read(int input);

    void write(int output, short value);
}
