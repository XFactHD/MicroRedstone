package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.Circuit;
import io.github.xfacthd.microredstone.common.circuit.CircuitState;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.RootCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.util.DebugExportConfigExtension;
import io.github.xfacthd.microredstone.util.TestBuilder;
import io.github.xfacthd.microredstone.util.TestInterfaceAdapter;
import io.github.xfacthd.microredstone.util.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugExportConfigExtension.class)
public final class SingleNodeBundlePackUnpackTests
{
    @Test
    void testBundlePack()
    {
        for (int bit = 0; bit < 16; bit++)
        {
            TestBuilder builder = new TestBuilder();

            Wire wireIn = builder.addWire(WireType.SINGLE);
            Wire wireOut = builder.addWire(WireType.BUNDLED);

            Connection conIn = builder.addConnection(Port.LEFT, WireType.SINGLE, PortDir.INPUT);
            conIn.connect(wireIn);
            Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
            conOut.connect(wireOut);

            ConverterPrototypeNode packProtoNode = builder.addNode(new ConverterPrototypeNode(ConverterPrototypeNode.Type.PACK));
            packProtoNode.setBitIndex(bit);
            packProtoNode.setConnection(Port.LEFT, wireIn, true);
            packProtoNode.setConnection(Port.RIGHT, wireOut, true);

            CompoundPrototypeNode protoNode = builder.build();
            CompoundCircuitNode node = TestUtils.assemble(protoNode, "PACK_bit" + bit);

            RootCircuitNode compiled = TestUtils.compile(node);
            Assertions.assertNotNull(compiled, "Compilation failed");

            Circuit circuitInterp = new Circuit(node);
            Circuit circuitCompiled = new Circuit(compiled);
            TestInterfaceAdapter adapter = new TestInterfaceAdapter();

            for (int i = 0; i <= 0b11; i++)
            {
                int left = adapter.setValue(Port.LEFT, i & 1);
                circuitInterp.evaluate(adapter, null);
                Assertions.assertEquals(left << bit, adapter.getValue(Port.RIGHT), "Interpreted PACK bit " + bit + " input " + left);
            }
            for (int i = 0; i <= 0b11; i++)
            {
                int left = adapter.setValue(Port.LEFT, i & 1);
                circuitCompiled.evaluate(adapter, null);
                Assertions.assertEquals(left << bit, adapter.getValue(Port.RIGHT), "Compiled PACK bit " + bit + " input " + left);
            }

            CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted PACK serialize state");
            CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled PACK serialize state");
            Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted PACK apply interpreted state");
            Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted PACK apply compiled state");
            Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled PACK apply compiled state");
            Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled PACK apply interpreted state");
        }
    }

    @Test
    void testBundleMultiPack()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireInOne = builder.addWire(WireType.SINGLE);
        Wire wireInTwo = builder.addWire(WireType.SINGLE);
        Wire wireOut = builder.addWire(WireType.BUNDLED);

        Connection conInOne = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInOne.connect(wireInOne);
        Connection conInTwo = builder.addConnection(Port.DOWN, WireType.SINGLE, PortDir.INPUT);
        conInTwo.connect(wireInTwo);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireOut);

        ConverterPrototypeNode packProtoNodeOne = builder.addNode(new ConverterPrototypeNode(ConverterPrototypeNode.Type.PACK));
        packProtoNodeOne.setBitIndex(0);
        packProtoNodeOne.setConnection(Port.LEFT, wireInOne, true);
        packProtoNodeOne.setConnection(Port.RIGHT, wireOut, true);

        ConverterPrototypeNode packProtoNodeTwo = builder.addNode(new ConverterPrototypeNode(ConverterPrototypeNode.Type.PACK));
        packProtoNodeTwo.setBitIndex(1);
        packProtoNodeTwo.setConnection(Port.LEFT, wireInTwo, true);
        packProtoNodeTwo.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        CompoundCircuitNode node = TestUtils.assemble(protoNode, "PACK_MULTI");

        RootCircuitNode compiled = TestUtils.compile(node);
        Assertions.assertNotNull(compiled, "Compilation failed");

        Circuit circuitInterp = new Circuit(node);
        Circuit circuitCompiled = new Circuit(compiled);
        TestInterfaceAdapter adapter = new TestInterfaceAdapter();

        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 1);
            circuitInterp.evaluate(adapter, null);
            Assertions.assertEquals(i, adapter.getValue(Port.RIGHT), "Interpreted PACK_MULTI input " + up + ", " + down);
        }
        for (int i = 0; i <= 0b11; i++)
        {
            int up = adapter.setValue(Port.UP, i & 1);
            int down = adapter.setValue(Port.DOWN, (i >> 1) & 1);
            circuitCompiled.evaluate(adapter, null);
            Assertions.assertEquals(i, adapter.getValue(Port.RIGHT), "Compiled PACK_MULTI input " + up + ", " + down);
        }
    }

    @Test
    void testBundleUnpack()
    {
        for (int bit = 0; bit < 16; bit++)
        {
            TestBuilder builder = new TestBuilder();

            Wire wireIn = builder.addWire(WireType.BUNDLED);
            Wire wireOut = builder.addWire(WireType.SINGLE);

            Connection conIn = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
            conIn.connect(wireIn);
            Connection conOut = builder.addConnection(Port.RIGHT, WireType.SINGLE, PortDir.OUTPUT);
            conOut.connect(wireOut);

            ConverterPrototypeNode packProtoNode = builder.addNode(new ConverterPrototypeNode(ConverterPrototypeNode.Type.UNPACK));
            packProtoNode.setBitIndex(bit);
            packProtoNode.setConnection(Port.LEFT, wireIn, true);
            packProtoNode.setConnection(Port.RIGHT, wireOut, true);

            CompoundPrototypeNode protoNode = builder.build();
            CompoundCircuitNode node = TestUtils.assemble(protoNode, "UNPACK_bit" + bit);

            RootCircuitNode compiled = TestUtils.compile(node);
            Assertions.assertNotNull(compiled, "Compilation failed");

            Circuit circuitInterp = new Circuit(node);
            Circuit circuitCompiled = new Circuit(compiled);
            TestInterfaceAdapter adapter = new TestInterfaceAdapter();

            for (int i = 0; i <= 16; i++)
            {
                int left = adapter.setValue(Port.LEFT, (1 << i) & 0xFFFF);
                circuitInterp.evaluate(adapter, null);
                Assertions.assertEquals(i == bit ? 1 : 0, adapter.getValue(Port.RIGHT), "Interpreted UNPACK bit " + bit + " input " + left);
            }
            for (int i = 0; i <= 16; i++)
            {
                int left = adapter.setValue(Port.LEFT, (1 << i) & 0xFFFF);
                circuitCompiled.evaluate(adapter, null);
                Assertions.assertEquals(i == bit ? 1 : 0, adapter.getValue(Port.RIGHT), "Compiled UNPACK bit " + bit + " input " + left);
            }

            CircuitState stateInterp = Assertions.assertDoesNotThrow(node::serializeState, "Interpreted UNPACK serialize state");
            CircuitState stateCompiled = Assertions.assertDoesNotThrow(compiled::serializeState, "Compiled UNPACK serialize state");
            Assertions.assertDoesNotThrow(() -> node.applyState(stateInterp), "Interpreted UNPACK apply interpreted state");
            Assertions.assertDoesNotThrow(() -> node.applyState(stateCompiled), "Interpreted UNPACK apply compiled state");
            Assertions.assertDoesNotThrow(() -> compiled.applyState(stateCompiled), "Compiled UNPACK apply compiled state");
            Assertions.assertDoesNotThrow(() -> compiled.applyState(stateInterp), "Compiled UNPACK apply interpreted state");
        }
    }
}
