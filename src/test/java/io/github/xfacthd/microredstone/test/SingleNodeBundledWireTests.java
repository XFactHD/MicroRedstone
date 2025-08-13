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
import io.github.xfacthd.microredstone.common.circuit.prototype.BufferPrototypeNode;
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
public final class SingleNodeBundledWireTests
{
    @Test
    void testDirectWire()
    {
        TestBuilder builder = new TestBuilder();

        Wire wire = builder.addWire(WireType.BUNDLED);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conIn.connect(wire);

        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wire);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "WIRE");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 32; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b1111, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Interpreted WIRE input " + i);
        }
        for (int i = 0; i < 32; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b1111, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Compiled WIRE input " + i);
        }
    }

    @Test
    void testBufferBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireIn = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conIn.connect(wireIn);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        BufferPrototypeNode bufProtoNode = builder.addNode(new BufferPrototypeNode(WireType.BUNDLED));
        bufProtoNode.setConnection(Port.LEFT, wireIn, true);
        bufProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "BUFFER_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        int lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b11, 2));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Interpreted BUFFER last cycle input " + lastInput);
            lastInput = left;
        }
        lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b11, 2));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Compiled BUFFER last cycle input " + lastInput);
            lastInput = left;
        }
    }

    @Test
    void testNotBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireIn = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conIn = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conIn.connect(wireIn);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode notProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOT, 1, WireType.BUNDLED));
        notProtoNode.setConnection(Port.LEFT, wireIn, true);
        notProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOT_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~left & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOT input " + i);
        }
        for (int i = 0; i <= 0b1111; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~left & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOT input " + i);
        }
    }

    @Test
    void testAndTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.AND, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "AND_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + down);
        }
    }

    @Test
    void testAndThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.AND, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "AND_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testOrTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.OR, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "OR_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + down);
        }
    }

    @Test
    void testOrThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.OR, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "OR_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testXorTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XOR, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XOR_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + down);
        }
    }

    @Test
    void testXorThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XOR, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XOR_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testNandTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NAND, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NAND_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + down);
        }
    }

    @Test
    void testNandThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NAND, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NAND_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up & left & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up & left & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testNorTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOR, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOR_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + down);
        }
    }

    @Test
    void testNorThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.NOR, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "NOR_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up | left | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up | left | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + left + "," + down);
        }
    }

    @Test
    void testXnorTwoBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XNOR, 2, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.DOWN, wireInTwo, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XNOR_two_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + down);
        }
    }

    @Test
    void testXnorThreeBundled()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.BUNDLED);
        Wire wireInTwo = builder.addWire(WireType.BUNDLED);
        Wire wireInThree = builder.addWire(WireType.BUNDLED);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.BUNDLED, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conInThree = builder.addConnection(Port.DOWN, WireType.BUNDLED, PortDir.INPUT);
        conInThree.connect(wireInThree);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        PrimitivePrototypeNode andProtoNode = builder.addNode(new PrimitivePrototypeNode(PrimitivePrototypeNode.Type.XNOR, 3, WireType.BUNDLED));
        andProtoNode.setConnection(Port.UP, wireInOne, true);
        andProtoNode.setConnection(Port.LEFT, wireInTwo, true);
        andProtoNode.setConnection(Port.DOWN, wireInThree, true);
        andProtoNode.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode);

        RootCircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "XNOR_three_bundled");
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter);
            Assertions.assertEquals(~(up ^ left ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter);
            Assertions.assertEquals(~(up ^ left ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + left + "," + down);
        }
    }
}
