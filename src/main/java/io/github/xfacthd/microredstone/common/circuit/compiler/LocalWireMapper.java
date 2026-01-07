package io.github.xfacthd.microredstone.common.circuit.compiler;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ConstantDescs;
import java.util.Arrays;

public final class LocalWireMapper
{
    private final CodeBuilder mthBody;
    private final int[] wireParams;
    private final int[] wireLocals;

    LocalWireMapper(CodeBuilder mthBody, int wireCount)
    {
        this.mthBody = mthBody;
        this.wireParams = new int[wireCount];
        Arrays.fill(wireParams, -1);
        this.wireLocals = new int[wireCount];
        Arrays.fill(wireLocals, -1);
    }

    void captureParam(int wire, int param)
    {
        // Add one to "jump" over "this"
        wireParams[wire] = param + 1;
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
            local = wireLocals[wire] = mthBody.allocateLocal(TypeKind.SHORT);
            mthBody.localVariable(local, "wire" + wire, ConstantDescs.CD_short, mthBody.startLabel(), mthBody.endLabel());
        }
        return local;
    }

    public void generateLoad(int inputWire)
    {
        int param = wireParams[inputWire];
        mthBody.iload(param != -1 ? param : getLocal(inputWire));
    }

    public void generateStore(int outputWire)
    {
        int param = wireParams[outputWire];
        if (param != -1)
        {
            throw new IllegalStateException("Cannot store into input parameter");
        }
        mthBody.istore(getLocal(outputWire));
    }
}
