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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EvalMethodCompiler
{
    private final Map<BufferCircuitNode, CircuitCompiler.BufferFieldSpec> bufferFields = new Reference2ObjectLinkedOpenHashMap<>();
    private final List<CircuitCompiler.ClockFieldSpec> clockFields = new ArrayList<>();
    private final FieldAppender fieldAppender;
    private final ClassWriter classWriter;
    private final Type selfType;
    private int nestedNodeCounter = 0;

    EvalMethodCompiler(ClassWriter classWriter, Type selfType)
    {
        this.classWriter = classWriter;
        this.selfType = selfType;
        this.fieldAppender = makeFieldAppender();
    }

    public void compileRootEval(CompoundCircuitNode node, NodeCompiler compiler)
    {
        // Read inputs from incoming EvalContext
        InputOutputCompiler inputCompiler = (generator, localWires, inputCons) ->
                compileContextReadWrite(generator, 1, inputCons, input ->
                {
                    generator.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_LOAD_MTH);
                    localWires.generateStore(input.wire());
                });
        // Write outputs to incoming EvalContext
        InputOutputCompiler outputCompiler = (generator, localWires, outputCons) ->
                compileContextReadWrite(generator, 2, outputCons, output ->
                {
                    localWires.generateLoad(output.wire());
                    generator.invokeVirtual(CircuitCompiler.EVAL_CONTEXT_TYPE, CircuitCompiler.EVAL_CONTEXT_STORE_MTH);
                });
        // Build root eval method
        compileEval(CircuitCompiler.NODE_EVAL_MTH, true, node, compiler, inputCompiler, outputCompiler);
    }

    private static void compileContextReadWrite(GeneratorAdapter generator, int wirePairParam, Connector[] connectors, Consumer<Connector> compiler)
    {
        for (int i = 0; i < connectors.length; i++)
        {
            Connector con = connectors[i];

            generator.loadArg(0); // EvalContext
            generator.loadArg(wirePairParam); // WirePair[]
            generator.push(i);
            generator.arrayLoad(CircuitCompiler.WIRE_PAIR_TYPE); // WirePair[i]
            generator.invokeVirtual(CircuitCompiler.WIRE_PAIR_TYPE, CircuitCompiler.WIRE_PAIR_EXTERNAL_MTH); // WirePair#external()
            compiler.accept(con);
        }
    }

    public void compileNestedEval(
            GeneratorAdapter outerGenerator,
            LocalWireMapper outerLocalWires,
            CompoundCircuitNode node,
            WirePair[] inputMappings,
            WirePair[] outputMappings,
            NodeCompiler compiler
    )
    {
        Method evalMth = makeNestedEvalMethod(node.getName(), node.getInputs().length, node.getOutputs().length);

        // Load input params onto stack and call nested eval method from outer method
        outerGenerator.loadThis();
        for (int i = 0; i < node.getInputs().length; i++)
        {
            outerLocalWires.generateLoad(inputMappings[i].external());
        }
        outerGenerator.invokeVirtual(selfType, evalMth);

        // Capture input params in nested method's LocalWireMapper
        InputOutputCompiler inputCompiler = (generator, localWires, inputCons) ->
        {
            for (int i = 0; i < inputCons.length; i++)
            {
                Connector con = inputCons[i];
                localWires.captureParam(con.wire(), i);
            }
        };
        // Pack nested eval results into return value
        InputOutputCompiler outputCompiler = (generator, localWires, outputCons) ->
        {
            switch (outputCons.length)
            {
                case 0 -> {}
                case 1 -> localWires.generateLoad(outputCons[0].wire());
                case 2 ->
                {
                    localWires.generateLoad(outputCons[0].wire());
                    localWires.generateLoad(outputCons[1].wire());
                    generator.push(16);
                    generator.math(GeneratorAdapter.SHL, Type.INT_TYPE);
                    generator.math(GeneratorAdapter.OR, Type.INT_TYPE);
                }
                default ->
                {
                    for (int i = 0; i < outputCons.length; i++)
                    {
                        localWires.generateLoad(outputCons[i].wire());
                        generator.cast(Type.SHORT_TYPE, Type.LONG_TYPE);
                        if (i > 0)
                        {
                            generator.push(16 * i);
                            generator.math(GeneratorAdapter.SHL, Type.LONG_TYPE);
                            generator.math(GeneratorAdapter.OR, Type.LONG_TYPE);
                        }
                    }
                }
            }
        };
        // Build nested eval method
        compileEval(evalMth, false, node, compiler, inputCompiler, outputCompiler);

        // Unpack results from nested eval call and store back into locals in outer eval method
        Connector[] outputs = node.getOutputs();
        switch (outputs.length)
        {
            case 0 -> {}
            case 1 ->
                    // return outputLocal0;
                    outerLocalWires.generateStore(outputMappings[0].external());
            case 2 ->
            {
                // return (outputLocal1 << 16) | outputLocal0;
                outerGenerator.dup();
                outerGenerator.push(0xFFFF);
                outerGenerator.math(GeneratorAdapter.AND, Type.INT_TYPE);
                outerGenerator.cast(Type.INT_TYPE, Type.SHORT_TYPE);
                outerLocalWires.generateStore(outputMappings[0].external());
                outerGenerator.push(16);
                outerGenerator.math(GeneratorAdapter.USHR, Type.INT_TYPE);
                outerGenerator.push(0xFFFF);
                outerGenerator.math(GeneratorAdapter.AND, Type.INT_TYPE);
                outerGenerator.cast(Type.INT_TYPE, Type.SHORT_TYPE);
                outerLocalWires.generateStore(outputMappings[1].external());
            }
            default ->
            {
                // return (outputLocal3 << 48) | (outputLocal2 << 32) | (outputLocal1 << 16) | outputLocal0;
                for (int i = 0; i < outputs.length; i++)
                {
                    if (i < outputs.length - 1)
                    {
                        outerGenerator.dup2();
                    }
                    if (i > 0)
                    {
                        outerGenerator.push(16 * i);
                        outerGenerator.math(GeneratorAdapter.USHR, Type.LONG_TYPE);
                    }
                    outerGenerator.push(0xFFFFL);
                    outerGenerator.math(GeneratorAdapter.AND, Type.LONG_TYPE);
                    outerGenerator.cast(Type.LONG_TYPE, Type.SHORT_TYPE);
                    outerLocalWires.generateStore(outputMappings[i].external());
                }
            }
        }
    }

    private void compileEval(
            Method evalMth,
            boolean rootMth,
            CompoundCircuitNode node,
            NodeCompiler compiler,
            InputOutputCompiler inputCompiler,
            InputOutputCompiler outputCompiler
    )
    {
        GeneratorAdapter generator = new GeneratorAdapter(rootMth ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE, evalMth, null, null, classWriter);
        LocalWireMapper localWires = new LocalWireMapper(generator, node.getWireCount());

        inputCompiler.compile(generator, localWires, node.getInputs());
        compiler.compile(this, generator, selfType, fieldAppender, localWires, node);
        outputCompiler.compile(generator, localWires, node.getOutputs());

        if (rootMth)
        {
            Label skipCaptureLabel = new Label();

            generator.loadArg(3);
            generator.ifNull(skipCaptureLabel);

            List<Wire> wires = node.getWires();
            for (int i = 0; i < wires.size(); i++)
            {
                if (wires.get(i).getWireType() == WireType.BUNDLED) continue;

                generator.loadArg(3);
                generator.push(i);
                localWires.generateLoad(i);
                generator.invokeVirtual(CircuitCompiler.WIRE_STATES_TYPE, CircuitCompiler.WIRE_STATES_SET_MTH);
            }

            generator.mark(skipCaptureLabel);
        }

        generator.returnValue();
        generator.endMethod();
    }

    private Method makeNestedEvalMethod(String nodeName, int inputCount, int outputCount)
    {
        String methodName = "evaluate$nested$" + nodeName.replace(" ", "_") + "$" + nestedNodeCounter;
        nestedNodeCounter++;
        Type[] paramTypes = Utils.fillArray(new Type[inputCount], $ -> Type.SHORT_TYPE);
        Type retType = switch (outputCount)
        {
            case 0 -> Type.VOID_TYPE;
            case 1 -> Type.SHORT_TYPE;
            case 2 -> Type.INT_TYPE;
            default -> Type.LONG_TYPE;
        };
        return new Method(methodName, retType, paramTypes);
    }

    List<CircuitCompiler.BufferFieldSpec> getBufferFields()
    {
        return List.copyOf(bufferFields.values());
    }

    List<CircuitCompiler.ClockFieldSpec> getClockFields()
    {
        return clockFields;
    }

    private FieldAppender makeFieldAppender()
    {
        return new FieldAppender()
        {
            @Override
            public String getOrAddBufferField(BufferCircuitNode buffer)
            {
                CircuitCompiler.BufferFieldSpec fieldSpec = bufferFields.get(buffer);
                if (fieldSpec == null)
                {
                    String fieldName = "bufferState" + bufferFields.size();
                    classWriter.visitField(Opcodes.ACC_PRIVATE, fieldName, Type.SHORT_TYPE.getDescriptor(), null, 0);
                    fieldSpec = new CircuitCompiler.BufferFieldSpec(fieldName);
                    bufferFields.put(buffer, fieldSpec);
                }
                return fieldSpec.name();
            }

            @Override
            public ClockCircuitNode.Fields addClockField(boolean needCounter, int counterInit)
            {
                int index = clockFields.size();
                String counterName = needCounter ? ("clockCounter" + index) : null;
                String stateName = "clockState" + index;
                if (counterName != null)
                {
                    classWriter.visitField(Opcodes.ACC_PRIVATE, counterName, Type.INT_TYPE.getDescriptor(), null, null);
                }
                classWriter.visitField(Opcodes.ACC_PRIVATE, stateName, Type.INT_TYPE.getDescriptor(), null, null);
                clockFields.add(new CircuitCompiler.ClockFieldSpec(counterName, stateName, counterInit));
                return new ClockCircuitNode.Fields(counterName, stateName);
            }
        };
    }

    @FunctionalInterface
    private interface InputOutputCompiler
    {
        void compile(GeneratorAdapter generator, LocalWireMapper localWires, Connector[] connectors);
    }
}
