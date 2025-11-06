package io.github.xfacthd.microredstone.common.circuit.assembler;

import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.node.NodeEntry;
import io.github.xfacthd.microredstone.common.circuit.node.base.LeafCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.util.Utils;

import java.util.BitSet;
import java.util.regex.Pattern;

public final class CircuitValidator
{
    private static final Pattern NAME_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9 ]*)$");
    public static final int MAX_CON_NAME_LEN = 32;

    public static boolean validate(CompoundCircuitNode circuitNode)
    {
        return validateName(circuitNode.getName(), true) && validateCompound(circuitNode);
    }

    private static boolean validateCompound(CompoundCircuitNode circuitNode)
    {
        int wireCount = circuitNode.getWireCount();
        BitSet wires = new BitSet();

        for (Connector con : Utils.concatArrays(circuitNode.getInputs(), circuitNode.getOutputs()))
        {
            if (!checkWire(wires, wireCount, con.wire()))
            {
                return false;
            }
            if (con.name().length() > MAX_CON_NAME_LEN)
            {
                return false;
            }
        }
        for (NodeEntry<?> entry : circuitNode)
        {
            for (WirePair wirePair : Utils.concatArrays(entry.inputs(), entry.outputs()))
            {
                if (!checkWire(wires, wireCount, wirePair.external()))
                {
                    return false;
                }
            }

            boolean valid = switch (entry.node())
            {
                case CompoundCircuitNode nestedNode -> validateCompound(nestedNode);
                case LeafCircuitNode leafNode -> leafNode.validate(entry, wires, wireCount);
                default -> throw new IllegalArgumentException("Unknown node type: " + entry.node());
            };
            if (!valid)
            {
                return false;
            }
        }
        return wires.nextClearBit(0) == wireCount;
    }

    private static boolean checkWire(BitSet wires, int wireCount, int wire)
    {
        if (wire >= 0 && wire < wireCount)
        {
            wires.set(wire);
            return true;
        }
        return false;
    }

    public static boolean validateName(String name, boolean strict)
    {
        if (!NAME_PATTERN.matcher(name).matches()) return false;
        if (!strict) return true;
        return !Character.isSpaceChar(name.charAt(name.length() - 1));
    }

    private CircuitValidator() {}
}
