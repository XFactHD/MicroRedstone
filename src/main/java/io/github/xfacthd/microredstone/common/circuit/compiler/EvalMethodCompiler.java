package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.Label;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class EvalMethodCompiler {
    private final Map<BufferCircuitNode, CircuitCompiler.BufferFieldSpec> bufferFields = new Reference2ObjectLinkedOpenHashMap<>();
    private final Map<ClockCircuitNode, CircuitCompiler.ClockFieldSpec> clockFields = new Reference2ObjectLinkedOpenHashMap<>();
    private final FieldGetter fieldGetter = makeFieldGetter();
    private final ClassBuilder clsBuilder;
    private final ClassDesc selfType;
    private final Deque<Runnable> compileQueue = new ArrayDeque<>();
    private int nestedNodeCounter = 0;

    EvalMethodCompiler(ClassBuilder clsBuilder, ClassDesc selfType) {
        this.clsBuilder = clsBuilder;
        this.selfType = selfType;
    }

    void executeCompileQueue() {
        while (!compileQueue.isEmpty()) {
            compileQueue.removeFirst().run();
        }
    }

    public void compileRootEval(CompoundCircuitNode node, NodeCompiler compiler) {
        // Read inputs from incoming EvalContext
        InputOutputCompiler inputCompiler = (mthBody, localWires, inputCons) ->
                compileContextReadWrite(mthBody, 1, inputCons, input -> {
                    mthBody.invokevirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, "loadInput", CircuitCompiler.EVAL_CONTEXT_LOAD_MTH);
                    localWires.generateStore(input.wire());
                });
        // Write outputs to incoming EvalContext
        InputOutputCompiler outputCompiler = (mthBody, localWires, outputCons) ->
                compileContextReadWrite(mthBody, 2, outputCons, output -> {
                    localWires.generateLoad(output.wire());
                    mthBody.invokevirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, "storeOutput", CircuitCompiler.EVAL_CONTEXT_STORE_MTH);
                });
        // Build root eval method
        compileEval("evaluate", CircuitCompiler.NODE_EVAL_MTH, true, node, compiler, inputCompiler, outputCompiler);
    }

    private static void compileContextReadWrite(CodeBuilder mthBody, int wirePairParam, Connector[] connectors, Consumer<Connector> compiler) {
        for (int i = 0; i < connectors.length; i++) {
            Connector con = connectors[i];

            mthBody.aload(1) // EvalContext
                    .aload(wirePairParam + 1) // WirePair[]
                    .loadConstant(i)
                    .aaload() // WirePair[i]
                    .invokevirtual(CircuitCompiler.WIRE_PAIR_TYPE, "external", CircuitCompiler.WIRE_PAIR_EXTERNAL_MTH); // WirePair#external()
            compiler.accept(con);
        }
    }

    public void compileNestedEval(
            CodeBuilder outerMthBody,
            LocalWireMapper outerLocalWires,
            CompoundCircuitNode node,
            WirePair[] inputMappings,
            WirePair[] outputMappings,
            NodeCompiler compiler
    ) {
        String evalMthName = "evaluate$nested$" + node.getName().replace(" ", "_") + "$" + nestedNodeCounter;
        nestedNodeCounter++;
        MethodTypeDesc evalMthDesc = makeNestedEvalMethod(node.getInputs().length, node.getOutputs().length);

        // Load input params onto stack and call nested eval method from outer method
        outerMthBody.aload(0); // this
        for (int i = 0; i < node.getInputs().length; i++) {
            outerLocalWires.generateLoad(inputMappings[i].external());
        }
        outerMthBody.invokevirtual(selfType, evalMthName, evalMthDesc);

        // Capture input params in nested method's LocalWireMapper
        InputOutputCompiler inputCompiler = (_, localWires, inputCons) -> {
            for (int i = 0; i < inputCons.length; i++) {
                Connector con = inputCons[i];
                localWires.captureParam(con.wire(), i);
            }
        };
        // Pack nested eval results into return value
        InputOutputCompiler outputCompiler = (mthBody, localWires, outputCons) -> {
            switch (outputCons.length) {
                case 0 -> { }
                case 1 ->
                    // return outputLocal0;
                        localWires.generateLoad(outputCons[0].wire());
                case 2 -> {
                    // return (outputLocal1 << 16) | outputLocal0;
                    localWires.generateLoad(outputCons[0].wire());
                    localWires.generateLoad(outputCons[1].wire());
                    mthBody.loadConstant(16)
                            .ishl()
                            .ior();
                }
                default -> {
                    // return (outputLocal1 << 48) | (outputLocal1 << 32) | (outputLocal1 << 16) | outputLocal0;
                    for (int i = 0; i < outputCons.length; i++) {
                        localWires.generateLoad(outputCons[i].wire());
                        mthBody.i2l();
                        if (i > 0) {
                            mthBody.loadConstant(16 * i)
                                    .lshl()
                                    .lor();
                        }
                    }
                }
            }
        };
        // Build nested eval method
        compileEval(evalMthName, evalMthDesc, false, node, compiler, inputCompiler, outputCompiler);

        // Unpack results from nested eval call and store back into locals in outer eval method
        Connector[] outputs = node.getOutputs();
        switch (outputs.length) {
            case 0 -> { }
            case 1 ->
                // short local = retVal;
                    outerLocalWires.generateStore(outputMappings[0].external());
            case 2 -> {
                // short local1 = retVal & 0xFFFF;
                // short local2 = (retVal >>> 16) & 0xFFFF;
                outerMthBody.dup()
                        .loadConstant(0xFFFF)
                        .iand()
                        .i2s();
                outerLocalWires.generateStore(outputMappings[0].external());
                outerMthBody.loadConstant(16)
                        .iushr()
                        .loadConstant(0xFFFF)
                        .iand()
                        .i2s();
                outerLocalWires.generateStore(outputMappings[1].external());
            }
            default -> {
                // short local1 = retVal & 0xFFFF;
                // short local2 = (retVal >>> 16) & 0xFFFF;
                // short local2 = (retVal >>> 32) & 0xFFFF;
                // short local3 = (retVal >>> 48) & 0xFFFF;
                for (int i = 0; i < outputs.length; i++) {
                    if (i < outputs.length - 1) {
                        outerMthBody.dup2();
                    }
                    if (i > 0) {
                        outerMthBody.loadConstant(16 * i)
                                .lushr();
                    }
                    outerMthBody.loadConstant(0xFFFFL)
                            .land()
                            .l2i()
                            .i2s();
                    outerLocalWires.generateStore(outputMappings[i].external());
                }
            }
        }
    }

    private void compileEval(
            String evalMthName,
            MethodTypeDesc evalMthDesc,
            boolean rootMth,
            CompoundCircuitNode node,
            NodeCompiler compiler,
            InputOutputCompiler inputCompiler,
            InputOutputCompiler outputCompiler
    ) {
        compileQueue.addLast(() -> compileEval0(evalMthName, evalMthDesc, rootMth, node, compiler, inputCompiler, outputCompiler));
    }

    private void compileEval0(
            String evalMthName,
            MethodTypeDesc evalMthDesc,
            boolean rootMth,
            CompoundCircuitNode node,
            NodeCompiler compiler,
            InputOutputCompiler inputCompiler,
            InputOutputCompiler outputCompiler
    ) {
        clsBuilder.withMethodBody(evalMthName, evalMthDesc, rootMth ? ClassFile.ACC_PUBLIC : ClassFile.ACC_PRIVATE, mthBody -> {
            LocalWireMapper localWires = new LocalWireMapper(mthBody, node.getWireCount());

            if (rootMth) {
                mthBody.localVariable(1, "context", CircuitCompiler.EVAL_CONTEXT_TYPE, mthBody.startLabel(), mthBody.endLabel());
                mthBody.localVariable(2, "inputs", CircuitCompiler.WIRE_PAIR_ARR_TYPE, mthBody.startLabel(), mthBody.endLabel());
                mthBody.localVariable(3, "outputs", CircuitCompiler.WIRE_PAIR_ARR_TYPE, mthBody.startLabel(), mthBody.endLabel());
                mthBody.localVariable(4, "wireStates", CircuitCompiler.WIRE_STATES_TYPE, mthBody.startLabel(), mthBody.endLabel());
            }

            inputCompiler.compile(mthBody, localWires, node.getInputs());
            compiler.compile(this, mthBody, selfType, fieldGetter, localWires, node);
            outputCompiler.compile(mthBody, localWires, node.getOutputs());

            List<Wire> wires = node.getWires();
            if (rootMth && wires.stream().anyMatch(wire -> wire.getWireType() == WireType.SINGLE)) {
                Label skipCaptureLabel = mthBody.newLabel();

                mthBody.aload(4); // WireStates
                mthBody.ifnull(skipCaptureLabel);

                for (int i = 0; i < wires.size(); i++) {
                    if (wires.get(i).getWireType() == WireType.BUNDLED) {
                        continue;
                    }

                    mthBody.aload(4); // WireStates
                    mthBody.loadConstant(i);
                    localWires.generateLoad(i);
                    mthBody.invokevirtual(CircuitCompiler.WIRE_STATES_TYPE, "set", CircuitCompiler.WIRE_STATES_SET_MTH);
                }

                mthBody.labelBinding(skipCaptureLabel);
            }

            switch (evalMthDesc.returnType().descriptorString()) {
                case "V" -> mthBody.return_();
                case "S" -> mthBody.return_(TypeKind.SHORT);
                case "I" -> mthBody.return_(TypeKind.INT);
                case "J" -> mthBody.return_(TypeKind.LONG);
            }
        });
    }

    private static MethodTypeDesc makeNestedEvalMethod(int inputCount, int outputCount) {
        ClassDesc[] paramTypes = Utils.fillArray(new ClassDesc[inputCount], _ -> ConstantDescs.CD_short);
        ClassDesc retType = switch (outputCount) {
            case 0 -> ConstantDescs.CD_void;
            case 1 -> ConstantDescs.CD_short;
            case 2 -> ConstantDescs.CD_int;
            default -> ConstantDescs.CD_long;
        };
        return MethodTypeDesc.of(retType, paramTypes);
    }

    List<CircuitCompiler.BufferFieldSpec> getBufferFields() {
        return List.copyOf(bufferFields.values());
    }

    List<CircuitCompiler.ClockFieldSpec> getClockFields() {
        return List.copyOf(clockFields.values());
    }

    FieldCollector makeFieldCollector() {
        return new FieldCollector() {
            @Override
            public void buffer(BufferCircuitNode buffer) {
                CircuitCompiler.BufferFieldSpec fieldSpec = bufferFields.get(buffer);
                if (fieldSpec != null) {
                    throw new IllegalStateException("Duplicate buffer field: " + buffer);
                }

                String fieldName = "bufferState" + bufferFields.size();
                clsBuilder.withField(fieldName, ConstantDescs.CD_short, fieldBuilder ->
                        fieldBuilder.withFlags(AccessFlag.PRIVATE)
                );
                fieldSpec = new CircuitCompiler.BufferFieldSpec(fieldName);
                bufferFields.put(buffer, fieldSpec);
            }

            @Override
            public void clock(ClockCircuitNode clock) {
                CircuitCompiler.ClockFieldSpec fieldSpec = clockFields.get(clock);
                if (fieldSpec != null) {
                    throw new IllegalStateException("Duplicate clock field: " + clock);
                }

                int index = clockFields.size();
                int halfPeriodLength = clock.getHalfPeriodLength();
                String counterName = null;
                if (halfPeriodLength > 1) {
                    counterName = "clockCounter" + index;
                    clsBuilder.withField(counterName, ConstantDescs.CD_int, fieldBuilder ->
                            fieldBuilder.withFlags(AccessFlag.PRIVATE)
                    );
                }
                String stateName = "clockState" + index;
                clsBuilder.withField(stateName, ConstantDescs.CD_int, fieldBuilder ->
                        fieldBuilder.withFlags(AccessFlag.PRIVATE)
                );
                fieldSpec = new CircuitCompiler.ClockFieldSpec(counterName, stateName, halfPeriodLength - 1);
                clockFields.put(clock, fieldSpec);
            }
        };
    }

    private FieldGetter makeFieldGetter() {
        return new FieldGetter() {
            @Override
            public String buffer(BufferCircuitNode buffer) {
                return Objects.requireNonNull(bufferFields.get(buffer)).name();
            }

            @Override
            public ClockCircuitNode.Fields clock(ClockCircuitNode clock) {
                return Objects.requireNonNull(clockFields.get(clock));
            }
        };
    }

    @FunctionalInterface
    private interface InputOutputCompiler {
        void compile(CodeBuilder mthBody, LocalWireMapper localWires, Connector[] connectors);
    }
}
