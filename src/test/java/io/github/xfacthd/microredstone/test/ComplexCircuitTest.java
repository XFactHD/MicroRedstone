package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.util.ComplexTestCircuits;
import io.github.xfacthd.microredstone.util.DebugExportConfigExtension;
import io.github.xfacthd.microredstone.util.TestInterfaceAdapter;
import io.github.xfacthd.microredstone.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugExportConfigExtension.class)
public final class ComplexCircuitTest
{
    @Test
    void testComplexCircuit()
    {
        CompoundPrototypeNode protoNode = ComplexTestCircuits.bcdTo7SegDecoder();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "BCD_2_7SEG");
        Assertions.assertNotNull(compiled, "Compilation failed");

        int[] expectedOutputs = new int[] {
                0b0111111,
                0b0000110,
                0b1011011,
                0b1001111,
                0b1100110,
                0b1101101,
                0b1111101,
                0b0000111,
                0b1111111,
                0b1101111
        };
        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 10; i++)
        {
            adapter.setValue(Port.LEFT, i);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(expectedOutputs[i], adapter.getValue(Port.RIGHT), "Interpreted BCD_TO_7SEG input " + i);
        }
        for (int i = 0; i < 10; i++)
        {
            adapter.setValue(Port.LEFT, i);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(expectedOutputs[i], adapter.getValue(Port.RIGHT), "Compiled BCD_TO_7SEG input " + i);
        }
    }
}
