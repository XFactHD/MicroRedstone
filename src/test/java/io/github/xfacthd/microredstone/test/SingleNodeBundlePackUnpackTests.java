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
import io.github.xfacthd.microredstone.common.circuit.prototype.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
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
            CompoundCircuitNode node = TestUtils.assemble(protoNode);

            CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "PACK_bit" + bit);
            Assertions.assertNotNull(compiled, "Compilation failed");

            Circuit circuitInterp = new Circuit(node);
            Circuit circuitCompiled = new Circuit(compiled);
            TestInterfaceAdapter adapter = new TestInterfaceAdapter();

            for (int i = 0; i <= 0b11; i++)
            {
                int left = adapter.setValue(Port.LEFT, i & 1);
                circuitInterp.evaluate(adapter);
                Assertions.assertEquals(left << bit, adapter.getValue(Port.RIGHT), "Interpreted PACK bit " + bit + " input " + left);
            }
            for (int i = 0; i <= 0b11; i++)
            {
                int left = adapter.setValue(Port.LEFT, i & 1);
                circuitCompiled.evaluate(adapter);
                Assertions.assertEquals(left << bit, adapter.getValue(Port.RIGHT), "Compiled PACK bit " + bit + " input " + left);
            }
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
            CompoundCircuitNode node = TestUtils.assemble(protoNode);

            CircuitNode compiled = CircuitCompiler.getOrCompileNode(node, "UNPACK_bit" + bit);
            Assertions.assertNotNull(compiled, "Compilation failed");

            Circuit circuitInterp = new Circuit(node);
            Circuit circuitCompiled = new Circuit(compiled);
            TestInterfaceAdapter adapter = new TestInterfaceAdapter();

            for (int i = 0; i <= 16; i++)
            {
                int left = adapter.setValue(Port.LEFT, (1 << i) & 0xFFFF);
                circuitInterp.evaluate(adapter);
                Assertions.assertEquals(i == bit ? 1 : 0, adapter.getValue(Port.RIGHT), "Interpreted UNPACK bit " + bit + " input " + left);
            }
            for (int i = 0; i <= 16; i++)
            {
                int left = adapter.setValue(Port.LEFT, (1 << i) & 0xFFFF);
                circuitCompiled.evaluate(adapter);
                Assertions.assertEquals(i == bit ? 1 : 0, adapter.getValue(Port.RIGHT), "Compiled UNPACK bit " + bit + " input " + left);
            }
        }
    }
}
