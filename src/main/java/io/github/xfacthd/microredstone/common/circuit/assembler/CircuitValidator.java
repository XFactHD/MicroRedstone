package io.github.xfacthd.microredstone.common.circuit.assembler;

import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class CircuitValidator
{
    private static final Pattern NAME_PATTERN = Pattern.compile("^([a-zA-Z][a-zA-Z0-9 ]*)$");
    public static final int MAX_CON_NAME_LEN = 32;

    public static boolean validate(@Nullable String name, CompoundCircuitNode circuitNode)
    {
        if (name != null && !validateName(name, true))
        {
            return false;
        }

        // TODO: implement validation of assembled circuit nodes (ensure nesting doesn't blow up processing times too much and potentially move checking off thread)
        return true;
    }

    public static boolean validateName(String name, boolean strict)
    {
        if (!NAME_PATTERN.matcher(name).matches()) return false;
        if (!strict) return true;
        return !Character.isSpaceChar(name.charAt(name.length() - 1));
    }

    private CircuitValidator() {}
}
