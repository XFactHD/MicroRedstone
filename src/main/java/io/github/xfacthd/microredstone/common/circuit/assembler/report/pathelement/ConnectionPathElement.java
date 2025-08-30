package io.github.xfacthd.microredstone.common.circuit.assembler.report.pathelement;

import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import net.minecraft.util.ProblemReporter;

public record ConnectionPathElement(Connection connection) implements ProblemReporter.PathElement
{
    @Override
    public String get()
    {
        return connection.toString();
    }
}
