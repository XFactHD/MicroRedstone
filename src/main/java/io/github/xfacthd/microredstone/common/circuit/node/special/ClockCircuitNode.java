package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.FieldAppender;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.Objects;

public final class ClockCircuitNode extends CircuitNode
{
    public static final MapCodec<ClockCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("half_period").forGetter(node -> node.halfPeriodLength),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, ClockCircuitNode::new));
    public static final StreamCodec<ByteBuf, ClockCircuitNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            node -> node.halfPeriodLength,
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            ClockCircuitNode::new
    );

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

    public void compile(GeneratorAdapter methodGen, FieldAppender fieldAppender, Type selfType, LocalWireMapper localWires)
    {
        boolean needCounter = halfPeriodLength > 1;
        Fields clockFields = fieldAppender.addClockField(needCounter, halfPeriodLength - 1);
        String stateField = clockFields.stateField;
        Label avoidToggleLabel = new Label();
        // Preload state field onto stack
        methodGen.loadThis();
        methodGen.getField(selfType, stateField, Type.INT_TYPE);
        int counterLocal = -1;
        if (needCounter)
        {
            String counterField = Objects.requireNonNull(clockFields.counterField);
            counterLocal = methodGen.newLocal(Type.INT_TYPE);
            methodGen.loadThis();
            // periodCounter++
            methodGen.getField(selfType, counterField, Type.INT_TYPE);
            methodGen.push(1);
            methodGen.math(GeneratorAdapter.ADD, Type.INT_TYPE);
            methodGen.dup();
            methodGen.storeLocal(counterLocal);
            // periodCounter >= halfPeriodMax
            methodGen.push(halfPeriodLength);
            methodGen.ifICmp(GeneratorAdapter.LT, avoidToggleLabel);
            // periodCounter = 0
            methodGen.push(0);
            methodGen.storeLocal(counterLocal);
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
            String counterField = Objects.requireNonNull(clockFields.counterField);
            methodGen.mark(avoidToggleLabel);
            methodGen.loadThis();
            methodGen.loadLocal(counterLocal);
            methodGen.putField(selfType, counterField, Type.INT_TYPE);
        }
        // wireLocal/context[wire] = state
        localWires.generateStore(outputWire);
    }

    public int getCounter()
    {
        return periodCounter;
    }

    public short getState()
    {
        return state;
    }

    public void applyState(int counter, int state)
    {
        this.periodCounter = counter;
        this.state = (short) state;
    }

    @Override
    public PrototypeNode disassemble()
    {
        ClockPrototypeNode node = new ClockPrototypeNode();
        node.setHalfPeriodLength(halfPeriodLength);
        return node;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_CLOCK.value();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof ClockCircuitNode other)) return false;
        return other.halfPeriodLength == halfPeriodLength && other.outputWire == outputWire;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(halfPeriodLength, outputWire);
    }

    public record Fields(@Nullable String counterField, String stateField) { }
}
