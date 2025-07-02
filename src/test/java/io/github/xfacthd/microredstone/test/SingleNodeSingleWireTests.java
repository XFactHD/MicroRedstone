package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.compiler.CircuitCompiler;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.CircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.util.DebugExportConfigExtension;
import io.github.xfacthd.microredstone.util.TestBuilder;
import io.github.xfacthd.microredstone.util.TestInterfaceAdapter;
import io.github.xfacthd.microredstone.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugExportConfigExtension.class)
public final class SingleNodeSingleWireTests
{
    @Test
    void testDirectWire()
    {
        TestBuilder builder = new TestBuilder();

        Wire wire = builder.addWire(WireType.SINGLE);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conIn.connect(wire);

        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wire);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "WIRE");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 4; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Interpreted WIRE input " + i);
        }
        for (int i = 0; i < 4; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Compiled WIRE input " + i);
        }
    }

    @Test
    void testClockOneTick()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        ClockPrototypeNode clockProtoNode = builder.addNode(new ClockPrototypeNode());
        clockProtoNode.setHalfPeriodLength(1);
        clockProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "CLOCK_one_tick");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 10; i++)
        {
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~i & 0x1, adapter.getValue(Port.RIGHT), "Interpreted CLOCK cycle " + i);
        }
        for (int i = 0; i < 10; i++)
        {
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~i & 0x1, adapter.getValue(Port.RIGHT), "Compiled CLOCK cycle " + i);
        }
    }

    @Test
    void testClockTwoTick()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        ClockPrototypeNode clockProtoNode = builder.addNode(new ClockPrototypeNode());
        clockProtoNode.setHalfPeriodLength(2);
        clockProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "CLOCK_two_tick");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 16; i++)
        {
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals((~i & 0b10) >> 1, adapter.getValue(Port.RIGHT), "Interpreted CLOCK cycle " + i);
        }
        for (int i = 0; i < 16; i++)
        {
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals((~i & 0b10) >> 1, adapter.getValue(Port.RIGHT), "Compiled CLOCK cycle " + i);
        }
    }

    @Test
    void testBufferSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireIn = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conIn.connect(wireIn);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        BufferPrototypeNode bufProtoNode = builder.addNode(new BufferPrototypeNode(WireType.SINGLE));
        bufProtoNode.setConnection(Port.RIGHT, wireIn, true);
        bufProtoNode.setConnection(Port.LEFT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "BUFFER_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        int lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Interpreted BUFFER last cycle input " + lastInput);
            lastInput = left;
        }
        lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, i & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Compiled BUFFER last cycle input " + lastInput);
            lastInput = left;
        }
    }

    @Test
    void testNotSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireIn = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conIn.connect(wireIn);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode notProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOT, 1, WireType.SINGLE));
        notProtoNode.setConnection(Port.LEFT, wireIn, true);
        notProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOT_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1; i++)
        {
            adapter.setValue(Port.LEFT, i);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~i & 0x1, adapter.getValue(Port.RIGHT), "Interpreted NOT input " + i);
        }
        for (int i = 0; i <= 0b1; i++)
        {
            adapter.setValue(Port.LEFT, i);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~i & 0x1, adapter.getValue(Port.RIGHT), "Compiled NOT input " + i);
        }
    }

    @Test
    void testAndTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.AND, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "AND_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + down);
        }
    }

    @Test
    void testAndThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.AND, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "AND_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testOrTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.OR, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "OR_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + down);
        }
    }

    @Test
    void testOrThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.OR, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "OR_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testXorTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XOR, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XOR_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + down);
        }
    }

    @Test
    void testXorThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XOR, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XOR_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testNandTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NAND, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NAND_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up & down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up & down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + down);
        }
    }

    @Test
    void testNandThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NAND, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NAND_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up & left & down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up & left & down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testNorTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOR, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOR_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up | down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up | down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + down);
        }
    }

    @Test
    void testNorThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOR, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOR_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up | left | down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up | left | down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testXnorTwoSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XNOR, 2, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XNOR_two_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up ^ down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up ^ down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + down);
        }
    }

    @Test
    void testXnorThreeSingle()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireInThree = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.SINGLE);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XNOR, 3, WireType.SINGLE));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XNOR_three_single");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up ^ left ^ down) & 0x1, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b111; i++)
        {
            int up = adapter.setValue(Port.UP, i & 0x1);
            int left = adapter.setValue(Port.LEFT, (i >> 1) & 0x1);
            int down = adapter.setValue(Port.DOWN, (i >> 2) & 0x1);
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up ^ left ^ down) & 0x1, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + left + "," + down);
        }
    }
}
