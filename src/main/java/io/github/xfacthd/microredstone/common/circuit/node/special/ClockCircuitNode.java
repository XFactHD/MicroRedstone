package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.FieldGetter;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.base.LeafCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClockCircuitNode extends LeafCircuitNode {
    public static final MapCodec<ClockCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("half_period").forGetter(node -> node.halfPeriodLength),
            Connector.CODEC.optionalFieldOf("inhibit_input").forGetter(ClockCircuitNode::getInhibitInput),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, ClockCircuitNode::new));
    public static final StreamCodec<ByteBuf, ClockCircuitNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            node -> node.halfPeriodLength,
            ByteBufCodecs.optional(Connector.STREAM_CODEC),
            ClockCircuitNode::getInhibitInput,
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            ClockCircuitNode::new
    );

    private final int halfPeriodLength;
    private final int inhibitInputWire;
    private final int outputWire;
    private int periodCounter;
    private short state = 0;

    public ClockCircuitNode(int halfPeriodLength, Optional<Connector> inhibitInput, Connector output) {
        super(inhibitInput.map(List::of).orElse(List.of()), List.of(output));
        this.halfPeriodLength = halfPeriodLength;
        this.inhibitInputWire = inhibitInput.map(Connector::wire).orElse(-1);
        this.outputWire = output.wire();
        this.periodCounter = halfPeriodLength - 1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs) {
        if (inhibitInputWire == -1 || context.loadInput(inhibitInputWire) == 0) {
            periodCounter++;
            if (periodCounter >= halfPeriodLength) {
                periodCounter = 0;
                state ^= 1;
            }
        }
        context.storeOutput(outputWire, state);
    }

    public void compile(CodeBuilder mthBody, FieldGetter fieldGetter, ClassDesc selfType, LocalWireMapper localWires) {
        boolean needCounter = halfPeriodLength > 1;
        boolean needInhibit = inhibitInputWire != -1;
        Fields clockFields = fieldGetter.clock(this);
        String stateField = clockFields.stateField();
        Label avoidToggleLabel = mthBody.newLabel();
        Label avoidFullyLabel = mthBody.newLabel();
        // Preload state field onto stack
        mthBody.aload(0) // this
                .getfield(selfType, stateField, ConstantDescs.CD_int);
        if (needInhibit) {
            localWires.generateLoad(inhibitInputWire);
            mthBody.loadConstant(0)
                    .if_icmpne(avoidFullyLabel);
        }
        int counterLocal = -1;
        if (needCounter) {
            String counterField = Objects.requireNonNull(clockFields.counterField());
            counterLocal = mthBody.allocateLocal(TypeKind.INT);
            mthBody.localVariable(counterLocal, "local$" + counterField, ConstantDescs.CD_int, mthBody.startLabel(), mthBody.endLabel());

            mthBody.aload(0) // this
                    // periodCounter++
                    .getfield(selfType, counterField, ConstantDescs.CD_int)
                    .loadConstant(1)
                    .iadd()
                    .dup()
                    .istore(counterLocal)
                    // periodCounter >= halfPeriodMax
                    .loadConstant(halfPeriodLength)
                    .if_icmplt(avoidToggleLabel)
                    // periodCounter = 0
                    .loadConstant(0)
                    .istore(counterLocal);
        }
        // state ^= 1
        mthBody.loadConstant(1)
                .ixor()
                .dup()
                .aload(0) // this
                .swap()
                .putfield(selfType, stateField, ConstantDescs.CD_int);
        if (needCounter) {
            String counterField = Objects.requireNonNull(clockFields.counterField());
            mthBody.labelBinding(avoidToggleLabel)
                    .aload(0) // this
                    .iload(counterLocal)
                    .putfield(selfType, counterField, ConstantDescs.CD_int);
        }
        if (needInhibit) {
            mthBody.labelBinding(avoidFullyLabel);
        }
        // wireLocal = state
        localWires.generateStore(outputWire);
    }

    private Optional<Connector> getInhibitInput() {
        return inputs.length > 0 ? Optional.of(inputs[0]) : Optional.empty();
    }

    public int getHalfPeriodLength() {
        return halfPeriodLength;
    }

    public int getCounter() {
        return periodCounter;
    }

    public short getState() {
        return state;
    }

    public void applyState(int counter, int state) {
        this.periodCounter = counter;
        this.state = (short) state;
    }

    @Override
    public boolean validate(NodeEntry<?> entry, BitSet wires, int wireCount) {
        return halfPeriodLength >= 1;
    }

    @Override
    public PrototypeNode disassemble() {
        ClockPrototypeNode node = new ClockPrototypeNode();
        node.setHalfPeriodLength(halfPeriodLength);
        return node;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type() {
        return MRContent.NODE_TYPE_CLOCK.value();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ClockCircuitNode other)) {
            return false;
        }
        return other.halfPeriodLength == halfPeriodLength && other.inhibitInputWire == inhibitInputWire && other.outputWire == outputWire;
    }

    @Override
    public int hashCode() {
        return Objects.hash(halfPeriodLength, inhibitInputWire, outputWire);
    }

    public interface Fields {
        @Nullable String counterField();

        String stateField();
    }
}
