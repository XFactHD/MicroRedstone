package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitAssembler;
import io.github.xfacthd.microredstone.common.circuit.assembler.report.CircuitErrorCollector;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

public final class TestUtils
{
    public static CompoundCircuitNode assemble(CompoundPrototypeNode protoNode, String name)
    {
        CircuitErrorCollector errors = new CircuitErrorCollector();
        CompoundCircuitNode node = CircuitAssembler.assemble(name, protoNode, errors);
        Assertions.assertNotNull(node, () -> "Assembler returned null: " + errors.printErrors());
        return node;
    }

    @Nullable
    public static CompiledCircuitNode compile(CompoundCircuitNode circuitNode)
    {
        RootCircuitNode node = CircuitCompiler.tryCompileNode(circuitNode).join();
        return node instanceof CompiledCircuitNode compiled ? compiled : null;
    }

    public static CircuitErrorCollector assertAssemblyFails(CompoundPrototypeNode protoNode, String name)
    {
        CircuitErrorCollector errors = new CircuitErrorCollector();
        CompoundCircuitNode node = CircuitAssembler.assemble(name, protoNode, errors);
        Assertions.assertNull(node, "Assembler returned non-null");
        return errors;
    }

    public static int expandToShort(int bits, int bitCount)
    {
        int value = 0;
        for (int i = 0; i < 16 / bitCount; i++)
        {
            value |= bits << (bitCount * i);
        }
        return value;
    }

    private TestUtils() { }
}
