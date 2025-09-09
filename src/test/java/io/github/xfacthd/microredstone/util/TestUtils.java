package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitAssembler;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.node.compiled.CompiledCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import net.minecraft.util.ProblemReporter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

public final class TestUtils
{
    public static CompoundCircuitNode assemble(CompoundPrototypeNode protoNode)
    {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        CompoundCircuitNode node = CircuitAssembler.assemble(protoNode, reporter);
        Assertions.assertNotNull(node, () -> "Assembler returned null: " + reporter.getReport());
        return node;
    }

    @Nullable
    public static CompiledCircuitNode compile(CompoundCircuitNode circuitNode, String name)
    {
        RootCircuitNode node = CircuitCompiler.tryCompileNode(circuitNode, name).join();
        return node instanceof CompiledCircuitNode compiled ? compiled : null;
    }

    public static ProblemReporter.Collector assertAssemblyFails(CompoundPrototypeNode protoNode)
    {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        CompoundCircuitNode node = CircuitAssembler.assemble(protoNode, reporter);
        Assertions.assertNull(node, "Assembler returned non-null");
        return reporter;
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
