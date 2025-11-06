package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.base.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "WIRE");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i < 32; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b1111, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Interpreted WIRE input " + i);
        }
        for (int i = 0; i < 32; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b1111, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(left, adapter.getValue(Port.RIGHT), "Compiled WIRE input " + i);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted WIRE serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled WIRE serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted WIRE apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted WIRE apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled WIRE apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled WIRE apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "BUFFER_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        int lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b11, 2));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Interpreted BUFFER last cycle input " + lastInput);
            lastInput = left;
        }
        lastInput = 0;
        for (int i = 0; i < 16; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i & 0b11, 2));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(lastInput, adapter.getValue(Port.RIGHT), "Compiled BUFFER last cycle input " + lastInput);
            lastInput = left;
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted BUFFER serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled BUFFER serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted BUFFER apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted BUFFER apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled BUFFER apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled BUFFER apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "NOT_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~left & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOT input " + i);
        }
        for (int i = 0; i <= 0b1111; i++)
        {
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort(i, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~left & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOT input " + i);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted NOT serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled NOT serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted NOT apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted NOT apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled NOT apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled NOT apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "AND_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted AND serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled AND serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted AND apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted AND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled AND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled AND apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "AND_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Interpreted AND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up & left & down, adapter.getValue(Port.RIGHT), "Compiled AND input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted AND serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled AND serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted AND apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted AND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled AND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled AND apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "OR_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted OR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled OR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted OR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted OR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled OR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled OR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "OR_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Interpreted OR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up | left | down, adapter.getValue(Port.RIGHT), "Compiled OR input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted OR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled OR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted OR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted OR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled OR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled OR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "XOR_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted XOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled XOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted XOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted XOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled XOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled XOR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "XOR_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Interpreted XOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(up ^ left ^ down, adapter.getValue(Port.RIGHT), "Compiled XOR input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted XOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled XOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted XOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted XOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled XOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled XOR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "NAND_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted NAND serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled NAND serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted NAND apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted NAND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled NAND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled NAND apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "NAND_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up & left & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NAND input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up & left & down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NAND input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted NAND serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled NAND serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted NAND apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted NAND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled NAND apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled NAND apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "NOR_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted NOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled NOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted NOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted NOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled NOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled NOR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "NOR_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up | left | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted NOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up | left | down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled NOR input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted NOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled NOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted NOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted NOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled NOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled NOR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "XNOR_two_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted XNOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled XNOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted XNOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted XNOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled XNOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled XNOR apply interpreted state");
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
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "XNOR_three_bundled");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(~(up ^ left ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Interpreted XNOR input " + up + "," + left + "," + down);
        }
        for (int i = 0; i <= 0b1111_1111_1111; i++)
        {
            int up = adapter.setValue(Port.UP, TestUtils.expandToShort(i & 0xF, 4));
            int left = adapter.setValue(Port.LEFT, TestUtils.expandToShort((i >> 4) & 0xF, 4));
            int down = adapter.setValue(Port.DOWN, TestUtils.expandToShort((i >> 8) & 0xF, 4));
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(~(up ^ left ^ down) & 0xFFFF, adapter.getValue(Port.RIGHT), "Compiled XNOR input " + up + "," + left + "," + down);
        }

        CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted XNOR serialize state");
        CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled XNOR serialize state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted XNOR apply interpreted state");
        Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted XNOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled XNOR apply compiled state");
        Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled XNOR apply interpreted state");
    }
}
