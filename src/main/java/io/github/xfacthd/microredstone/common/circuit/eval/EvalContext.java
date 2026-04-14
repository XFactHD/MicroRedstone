package io.github.xfacthd.microredstone.common.circuit.eval;

import io.github.xfacthd.microredstone.common.circuit.ExternalInterfaceAdapter;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;

import java.util.Arrays;

public sealed class EvalContext {
    protected final short[] wireStates;

    private EvalContext(int wireCount) {
        this.wireStates = new short[wireCount];
    }

    public short loadInput(int wire) {
        return wireStates[wire];
    }

    public void storeOutput(int wire, short value) {
        wireStates[wire] = value;
    }

    public void copyState(int src, int dest) {
        wireStates[dest] = wireStates[src];
    }

    public static final class Root extends EvalContext {
        private final int[] inputs;
        private final int[] outputs;

        public Root(WirePair[] inputs, WirePair[] outputs) {
            super(4);
            this.inputs = Arrays.stream(inputs).mapToInt(WirePair::external).toArray();
            this.outputs = Arrays.stream(outputs).mapToInt(WirePair::external).toArray();
        }

        public void prepare(ExternalInterfaceAdapter adapter) {
            for (int input : inputs) {
                wireStates[input] = adapter.read(input);
            }
        }

        public void flush(ExternalInterfaceAdapter adapter) {
            for (int output : outputs) {
                adapter.write(output, wireStates[output]);
            }
        }
    }

    public static final class Nested extends EvalContext {
        public Nested(int wireCount) {
            super(wireCount);
        }

        public void prepare(EvalContext outer, WirePair[] inputs) {
            for (WirePair input : inputs) {
                wireStates[input.internal()] = outer.wireStates[input.external()];
            }
        }

        public void flush(EvalContext outer, WirePair[] outputs) {
            for (WirePair output : outputs) {
                outer.wireStates[output.external()] = wireStates[output.internal()];
            }
        }
    }
}
