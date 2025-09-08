package io.github.xfacthd.microredstone.common.circuit.node.special;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.compiler.FieldAppender;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.compiler.LocalWireMapper;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.Objects;

/**
 * Special buffer node to use for safely evaluating circular dependencies
 */
public final class BufferCircuitNode extends CircuitNode
{
    public static final MapCodec<BufferCircuitNode> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Connector.CODEC.fieldOf("input").forGetter(node -> node.getInputs()[0]),
            Connector.CODEC.fieldOf("output").forGetter(node -> node.getOutputs()[0])
    ).apply(inst, BufferCircuitNode::new));
    public static final StreamCodec<ByteBuf, BufferCircuitNode> STREAM_CODEC = StreamCodec.composite(
            Connector.STREAM_CODEC,
            node -> node.getInputs()[0],
            Connector.STREAM_CODEC,
            node -> node.getOutputs()[0],
            BufferCircuitNode::new
    );

    private final int inputWire;
    private final int outputWire;

    public BufferCircuitNode(Connector input, Connector output)
    {
        super(List.of(input), List.of(output));
        this.inputWire = input.wire();
        this.outputWire = output.wire();
    }

    @Override
    public void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs)
    {
        context.copyState(inputWire, outputWire);
    }

    public void compileReadBack(GeneratorAdapter methodGen, Type selfType, FieldAppender fieldAppender, LocalWireMapper localWires)
    {
        String stateField = fieldAppender.getOrAddBufferField(this);

        methodGen.loadThis();
        methodGen.getField(selfType, stateField, Type.SHORT_TYPE);
        localWires.generateStore(outputWire);
    }

    public void compileCapture(GeneratorAdapter methodGen, Type selfType, FieldAppender fieldAppender, LocalWireMapper localWires)
    {
        String stateField = fieldAppender.getOrAddBufferField(this);

        methodGen.loadThis();
        localWires.generateLoad(inputWire);
        methodGen.putField(selfType, stateField, Type.SHORT_TYPE);
    }

    public int getInputWire()
    {
        return inputWire;
    }

    public int getOutputWire()
    {
        return outputWire;
    }

    @Override
    public CircuitNodeType<? extends CircuitNode> type()
    {
        return MRContent.NODE_TYPE_BUFFER.value();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof BufferCircuitNode other)) return false;
        return other.inputWire == inputWire && other.outputWire == outputWire;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(inputWire, outputWire);
    }
}
