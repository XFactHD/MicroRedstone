package io.github.xfacthd.microredstone.common.circuit.node.special;

import io.github.xfacthd.microredstone.common.circuit.compiler.ClockFieldAppender;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.Objects;

public final class ClockCircuitNode extends CircuitNode
{
    private final int halfPeriodLength;
    private final int outputWire;
    private int periodCounter;
    private short state = 0;

    public ClockCircuitNode(int halfPeriodLength, Connector output)
    {
        super(List.of(), List.of(output));
        this.halfPeriodLength = halfPeriodLength;
        this.outputWire = output.wire();
        this.periodCounter = halfPeriodLength - 1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        periodCounter++;
        if (periodCounter >= halfPeriodLength)
        {
            periodCounter = 0;
            state ^= 1;
        }
        context.storeOutput(outputWire, state);
    }

    public void compile(GeneratorAdapter methodGen, ClockFieldAppender clockFieldAppender, Type selfType, LocalWireMapper localWires)
    {
        boolean needCounter = halfPeriodLength > 1;
        Fields clockFields = clockFieldAppender.addNewField(needCounter, halfPeriodLength - 1);
        String stateField = clockFields.stateField;
        Label avoidToggleLabel = new Label();
        // Preload state field onto stack
        methodGen.loadThis();
        methodGen.getField(selfType, stateField, Type.INT_TYPE);
        if (needCounter)
        {
            String counterField = Objects.requireNonNull(clockFields.counterField);
            methodGen.loadThis();
            methodGen.dup();
            methodGen.dup();
            // periodCounter++
            methodGen.getField(selfType, counterField, Type.INT_TYPE);
            methodGen.push(1);
            methodGen.math(GeneratorAdapter.ADD, Type.INT_TYPE);
            methodGen.putField(selfType, counterField, Type.INT_TYPE);
            // periodCounter >= halfPeriodMax
            methodGen.getField(selfType, counterField, Type.INT_TYPE);
            methodGen.push(halfPeriodLength);
            methodGen.ifICmp(GeneratorAdapter.LT, avoidToggleLabel);
            // periodCounter = 0
            methodGen.loadThis();
            methodGen.push(0);
            methodGen.putField(selfType, counterField, Type.INT_TYPE);
        }
        // state ^= 1
        methodGen.push(1);
        methodGen.math(GeneratorAdapter.XOR, Type.INT_TYPE);
        methodGen.dup();
        methodGen.loadThis();
        methodGen.swap();
        methodGen.putField(selfType, stateField, Type.INT_TYPE);
        if (needCounter)
        {
            methodGen.mark(avoidToggleLabel);
        }
        // wireLocal/context[wire] = state
        localWires.generateStore(outputWire);
    }

    public record Fields(@Nullable String counterField, String stateField) { }
}
