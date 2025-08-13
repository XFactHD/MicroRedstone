package io.github.xfacthd.microredstone.common.circuit.compiler;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.special.BufferCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.ClockCircuitNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EvalMethodCompiler
{
    private final List<CircuitCompiler.NodeFieldSpec> nodeFields = new ArrayList<>();
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

    public void compileRootEval(
            int wireCount,
            List<NodeEntry<CircuitNode>> childNodes,
            List<NodeEntry<BufferCircuitNode>> bufferNodes,
            Connector[] outputs,
            NodeCompiler compiler
    )
    {
        GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PUBLIC, CircuitCompiler.NODE_EVAL_MTH, null, null, classWriter);
        int contextLocal = generator.newLocal(CircuitCompiler.NESTED_EVAL_CONTEXT_TYPE);
        LocalWireMapper localWires = new LocalWireMapper(generator, contextLocal, wireCount, childNodes, bufferNodes, outputs);

        compiler.compile(generator, selfType, fieldAppender, localWires);

        generator.returnValue();
        generator.endMethod();
    }

    public void compileNestedEval(
            int wireCount,
            int wireParamCount,
            List<NodeEntry<CircuitNode>> childNodes,
            List<NodeEntry<BufferCircuitNode>> bufferNodes,
            Connector[] outputs,
            NodeCompiler compiler
    )
    {
        Type[] paramTypes = Utils.fillArray(new Type[wireParamCount], $ -> Type.SHORT_TYPE);
        Method evalMth = new Method("evaluate$nested$" + nestedNodeCounter, Type.LONG_TYPE, paramTypes);
        nestedNodeCounter++;
        GeneratorAdapter generator = new GeneratorAdapter(Opcodes.ACC_PRIVATE, evalMth, null, null, classWriter);
        LocalWireMapper localWires = new LocalWireMapper(generator, -1, wireCount, childNodes, bufferNodes, outputs);

        compiler.compile(generator, selfType, fieldAppender, localWires);

        generator.returnValue();
        generator.endMethod();
    }

    List<CircuitCompiler.NodeFieldSpec> getNodeFields()
    {
        return nodeFields;
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
            public String addNodeField()
            {
                String fieldName = "complexNode" + nodeFields.size();
                classWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, CircuitCompiler.NODE_ENTRY_TYPE.getDescriptor(), null, null);
                nodeFields.add(new CircuitCompiler.NodeFieldSpec(fieldName, nodeFields.size()));
                return fieldName;
            }

            @Override
            public String getOrAddBufferField(BufferCircuitNode buffer)
            {
                CircuitCompiler.BufferFieldSpec fieldSpec = bufferFields.get(buffer);
                if (fieldSpec == null)
                {
                    String fieldName = "bufferNode" + bufferFields.size();
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
}
