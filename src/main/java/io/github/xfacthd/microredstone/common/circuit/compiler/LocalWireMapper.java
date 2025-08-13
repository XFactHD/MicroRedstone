package io.github.xfacthd.microredstone.common.circuit.compiler;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.Arrays;

public final class LocalWireMapper
{
    private final GeneratorAdapter methodGen;
    private final int[] wireParams;
    private final int[] wireLocals;

    LocalWireMapper(GeneratorAdapter methodGen, int wireCount)
    {
        this.methodGen = methodGen;
        this.wireParams = new int[wireCount];
        Arrays.fill(wireParams, -1);
        this.wireLocals = new int[wireCount];
        Arrays.fill(wireLocals, -1);
    }

    void captureParam(int wire, int param)
    {
        wireParams[wire] = param;
    }

    public boolean hasLocalOrParam(int wire)
    {
        return wireLocals[wire] != -1 || wireParams[wire] != -1;
    }

    private int getLocal(int wire)
    {
        int local = wireLocals[wire];
        if (local == -1)
        {
            local = wireLocals[wire] = methodGen.newLocal(Type.SHORT_TYPE);
        }
        return local;
    }

    public void generateLoad(int inputWire)
    {
        int param = wireParams[inputWire];
        if (param != -1)
        {
            methodGen.loadArg(param);
        }
        else
        {
            methodGen.loadLocal(getLocal(inputWire));
        }
    }

    public void generateStore(int outputWire)
    {
        int param = wireParams[outputWire];
        if (param != -1)
        {
            // TODO: verify that bundle packers cannot write into wires driven by other non-packer sources
            throw new IllegalStateException("Cannot store into input parameter");
        }
        methodGen.storeLocal(getLocal(outputWire));
    }
}
