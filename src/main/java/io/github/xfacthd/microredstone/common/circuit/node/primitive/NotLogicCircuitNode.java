package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.classfile.CodeBuilder;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public final class NotLogicCircuitNode extends PrimitiveCircuitNode {
    public static final MapCodec<NotLogicCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.BOOL.fieldOf("multi_bit").forGetter(NotLogicCircuitNode::isMultiBit),
            Connector.CODEC.fieldOf("input").forGetter(node -> node.getInputs()[0]),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, NotLogicCircuitNode::new));
    public static final StreamCodec<ByteBuf, NotLogicCircuitNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            node -> node.inversionMask != 0x1,
            Connector.STREAM_CODEC,
            node -> node.getInputs()[0],
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            NotLogicCircuitNode::new
    );

    private final int inputWire;
    private final int outputWire;
    private final int inversionMask;

    public NotLogicCircuitNode(boolean multiBit, Connector input, Connector output) {
        super(List.of(input), List.of(output));
        this.inputWire = input.wire();
        this.outputWire = output.wire();
        this.inversionMask = multiBit ? 0xFFFF : 0x1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs) {
        short input = context.loadInput(inputWire);
        context.storeOutput(outputWire, (short) (input ^ inversionMask));
    }

    @Override
    public void compile(CodeBuilder mthBody, LocalWireMapper localWires) {
        localWires.generateLoad(inputWire);
        mthBody.loadConstant(inversionMask)
                .ixor();
        localWires.generateStore(outputWire);
    }

    @Override
    public boolean validate(NodeEntry<?> entry, BitSet wires, int wireCount) {
        return true;
    }

    @Override
    public PrototypeNode disassemble() {
        return new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOT, 1, isMultiBit() ? WireType.BUNDLED : WireType.SINGLE);
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type() {
        return MRContent.NODE_TYPE_LOGIC_NOT.value();
    }

    private boolean isMultiBit() {
        return inversionMask != 0x1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NotLogicCircuitNode other)) {
            return false;
        }
        return other.inputWire == inputWire && other.outputWire == outputWire && other.inversionMask == inversionMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputWire, outputWire, inversionMask);
    }
}
