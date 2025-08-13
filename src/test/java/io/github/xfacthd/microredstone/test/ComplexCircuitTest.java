package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.util.ComplexTestCircuits;
import io.github.xfacthd.microredstone.util.DebugExportConfigExtension;
import io.github.xfacthd.microredstone.util.TestBuilder;
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

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "BCD_2_7SEG");
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

    @Test
    void testNestedCircuit()
    {
        TestBuilder innerBuilder = new TestBuilder();

        Wire innerWireIn = innerBuilder.addWire(WireType.SINGLE);
        Wire innerWireOut = innerBuilder.addWire(WireType.SINGLE);

        Connection innerConIn = innerBuilder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        innerConIn.connect(innerWireIn);
        Connection innerConOut = innerBuilder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        innerConOut.connect(innerWireOut);

        PrimitivePrototypeNode innerNotProtoNode = innerBuilder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOT, 1, WireType.SINGLE));
        innerNotProtoNode.setConnection(Port.LEFT, innerWireIn, true);
        innerNotProtoNode.setConnection(Port.RIGHT, innerWireOut, true);

        CompoundPrototypeNode innerProtoNode = innerBuilder.build();
        CompoundCircuitNode innerNode = TestUtils.assemble(innerProtoNode);

        TestBuilder outerBuilder = new TestBuilder();

        Wire outerWireIn = outerBuilder.addWire(WireType.SINGLE);
        Wire outerWireOut = outerBuilder.addWire(WireType.SINGLE);
        Wire outerWireCon = outerBuilder.addWire(WireType.SINGLE);

        Connection outerConIn = outerBuilder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        outerConIn.connect(outerWireIn);
        Connection outerConOut = outerBuilder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        outerConOut.connect(outerWireOut);

        ReferencePrototypeNode outerRefProtoNode = outerBuilder.addNode(ReferencePrototypeNode.create(innerNode));
        outerRefProtoNode.setConnection(Port.LEFT, outerWireIn, true);
        outerRefProtoNode.setConnection(Port.RIGHT, outerWireCon, true);
        PrimitivePrototypeNode outerNotProtoNode = outerBuilder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOT, 1, WireType.SINGLE));
        outerNotProtoNode.setConnection(Port.LEFT, outerWireCon, true);
        outerNotProtoNode.setConnection(Port.RIGHT, outerWireOut, true);

        CompoundPrototypeNode outerProtoNode = outerBuilder.build();
        CompoundCircuitNode outerNode = TestUtils.assemble(outerProtoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(outerNode, "NESTED");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(outerNode);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 4; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Interpreted NESTED input " + i);
        }
        for (int i = 0; i < 4; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Compiled NESTED input " + i);
        }
    }
}
