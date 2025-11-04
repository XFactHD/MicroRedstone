package io.github.xfacthd.microredstone.common.circuit.assembler.report;

import com.google.common.collect.Iterables;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CircuitErrorCollector
{
    private final List<RootError> rootErrors = new ArrayList<>();
    private final List<NodeError> nodeErrors = new ArrayList<>();
    private final List<WireError> wireErrors = new ArrayList<>();

    public void submit(RootError rootError)
    {
        this.rootErrors.add(rootError);
    }

    public void submit(NodeError nodeError)
    {
        this.nodeErrors.add(nodeError);
    }

    public void submit(WireError wireError)
    {
        this.wireErrors.add(wireError);
    }

    public boolean hasErrors()
    {
        return !rootErrors.isEmpty() || !nodeErrors.isEmpty() || !wireErrors.isEmpty();
    }

    public List<RootError> getRootErrors()
    {
        return rootErrors;
    }

    public List<NodeError> getNodeErrors()
    {
        return nodeErrors;
    }

    public List<WireError> getWireErrors()
    {
        return wireErrors;
    }

    // TODO: make errors descriptions and this listing translatable
    public List<Component> getErrorMessages()
    {
        List<Component> lines = new ArrayList<>();
        int errorCount = rootErrors.size() + nodeErrors.size() + wireErrors.size();
        lines.add(Component.literal("Encountered " + errorCount + " errors:"));
        for (CircuitError error : Iterables.concat(rootErrors, nodeErrors, wireErrors))
        {
            lines.add(Component.literal("- ").append(Objects.requireNonNull(error).description()));
        }
        return lines;
    }

    public String printErrors()
    {
        int errorCount = rootErrors.size() + nodeErrors.size() + wireErrors.size();
        StringBuilder builder = new StringBuilder("Encountered ").append(errorCount).append(" errors");
        if (errorCount == 0)
        {
            return builder.append(".").toString();
        }

        builder.append(":");
        for (CircuitError error : Iterables.concat(rootErrors, nodeErrors, wireErrors))
        {
            builder.append("\n- ").append(Objects.requireNonNull(error).description());
        }
        return builder.toString();
    }
}
