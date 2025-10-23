package io.github.xfacthd.microredstone.test;

import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.util.DebugExportConfigExtension;
import io.github.xfacthd.microredstone.util.TestBuilder;
import io.github.xfacthd.microredstone.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DebugExportConfigExtension.class)
public final class AssemblerValidationTests
{
    // TODO: implement (requires validation to output consistent messages)

    @Test
    void testBundlePackDriverCollision()
    {
        TestBuilder builder = new TestBuilder();

        Wire wireIn = builder.addWire(WireType.SINGLE);
        Wire wireThrough = builder.addWire(WireType.BUNDLED);

        Connection conInSingle = builder.addConnection(Port.UP, WireType.SINGLE, PortDir.INPUT);
        conInSingle.connect(wireIn);
        Connection conInBundled = builder.addConnection(Port.LEFT, WireType.BUNDLED, PortDir.INPUT);
        conInBundled.connect(wireThrough);
        Connection conOut = builder.addConnection(Port.RIGHT, WireType.BUNDLED, PortDir.OUTPUT);
        conOut.connect(wireThrough);

        ConverterPrototypeNode packProtoNode = builder.addNode(new ConverterPrototypeNode(ConverterPrototypeNode.Type.PACK));
        packProtoNode.setConnection(Port.LEFT, wireIn, true);
        packProtoNode.setConnection(Port.RIGHT, wireThrough, true);

        CompoundPrototypeNode protoNode = builder.build();
        // TODO: assert error "message" in returned reporter
        TestUtils.assertAssemblyFails(protoNode, "BundlePackDriverCollision");
    }

    @Test
    void testBundleMultiPackDriverCollision()
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
        packProtoNodeTwo.setBitIndex(0);
        packProtoNodeTwo.setConnection(Port.LEFT, wireInTwo, true);
        packProtoNodeTwo.setConnection(Port.RIGHT, wireOut, true);

        CompoundPrototypeNode protoNode = builder.build();
        // TODO: assert error "message" in returned reporter
        TestUtils.assertAssemblyFails(protoNode, "BundleMultiPackDriverCollision");
    }
}
