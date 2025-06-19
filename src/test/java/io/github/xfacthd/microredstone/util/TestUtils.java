package io.github.xfacthd.microredstone.util;

import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitAssembler;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import net.minecraft.util.ProblemReporter;
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
