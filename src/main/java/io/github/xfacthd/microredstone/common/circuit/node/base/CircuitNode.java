package io.github.xfacthd.microredstone.common.circuit.node.base;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.eval.EvalContext;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNodeType;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.data.MRRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public abstract sealed class CircuitNode permits RootCircuitNode, LeafCircuitNode {
    public static final Codec<CircuitNode> CODEC = MRRegistries.CIRCUIT_NODE_TYPES.byNameCodec()
            .dispatch(CircuitNode::type, CircuitNodeType::codec);
    public static final StreamCodec<ByteBuf, CircuitNode> STREAM_CODEC = ByteBufCodecs.idMapper(MRRegistries.CIRCUIT_NODE_TYPES)
            .dispatch(CircuitNode::type, CircuitNodeType::streamCodec);

    protected final Connector[] inputs;
    protected final Connector[] outputs;

    protected CircuitNode(List<Connector> inputs, List<Connector> outputs) {
        this.inputs = inputs.toArray(Connector[]::new);
        this.outputs = outputs.toArray(Connector[]::new);
    }

    protected CircuitNode(Connector[] inputs, Connector[] outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public abstract void evaluate(EvalContext context, WirePair[] inputs, WirePair[] outputs);

    public abstract PrototypeNode disassemble();

    /**
     * {@return the internal indices of ioPorts used as inputs}
     */
    public final Connector[] getInputs() {
        return inputs;
    }

    /**
     * {@return the internal indices of ioPorts used as outputs}
     */
    public final Connector[] getOutputs() {
        return outputs;
    }

    public abstract CircuitNodeType<? extends CircuitNode> type();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
