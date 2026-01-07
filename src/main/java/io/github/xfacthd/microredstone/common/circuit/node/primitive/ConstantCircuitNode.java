package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.classfile.CodeBuilder;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public final class ConstantCircuitNode extends PrimitiveCircuitNode
{
    public static final MapCodec<ConstantCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.SHORT.fieldOf("value").forGetter(node -> node.value),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, ConstantCircuitNode::new));
    public static final StreamCodec<ByteBuf, ConstantCircuitNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT,
            node -> node.value,
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            ConstantCircuitNode::new
    );

    private final short value;
    private final int outputWire;

    public ConstantCircuitNode(short value, Connector output)
    {
        super(List.of(), List.of(output));
        this.value = output.type().select((short)(value & 0x1), value);
        this.outputWire = output.wire();
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        context.storeOutput(outputWire, value);
    }

    @Override
    public void compile(CodeBuilder mthBody, LocalWireMapper localWires)
    {
        mthBody.loadConstant(value);
        localWires.generateStore(outputWire);
    }

    @Override
    public boolean validate(NodeEntry<?> entry, BitSet wires, int wireCount)
    {
        int maxValue = getOutputs()[0].type().select(
                ConstantPrototypeNode.MAX_VAL_SINGLE,
                ConstantPrototypeNode.MAX_VAL_BUNDLED
        );
        return (value & 0xFFFF) <= maxValue;
    }

    @Override
    public PrototypeNode disassemble()
    {
        ConstantPrototypeNode node = new ConstantPrototypeNode(getOutputs()[0].type());
        node.setValue(((int) value) & 0xFFFF);
        return node;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_CONSTANT.value();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof ConstantCircuitNode other)) return false;
        return other.value == value && other.outputWire == outputWire;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value, outputWire);
    }
}
