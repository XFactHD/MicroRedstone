package io.github.xfacthd.microredstone.common.circuit.node.primitive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;

public final class NotLogicCircuitNode extends PrimitiveCircuitNode
{
    public static final MapCodec<NotLogicCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.BOOL.fieldOf("multi_bit").forGetter(node -> node.inversionMask != 0x1),
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

    public NotLogicCircuitNode(boolean multiBit, Connector input, Connector output)
    {
        super(List.of(input), List.of(output));
        this.inputWire = input.wire();
        this.outputWire = output.wire();
        this.inversionMask = multiBit ? 0xFFFF : 0x1;
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        short input = context.loadInput(inputWire);
        context.storeOutput(outputWire, (short) (input ^ inversionMask));
    }

    @Override
    public void compile(GeneratorAdapter methodGen, LocalWireMapper localWires)
    {
        int inputLocal = localWires.getLocal(inputWire);

        methodGen.loadLocal(inputLocal);
        methodGen.push(inversionMask);
        methodGen.math(GeneratorAdapter.XOR, Type.SHORT_TYPE);
        localWires.generateStore(outputWire);
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_LOGIC_NOT.value();
    }
}
