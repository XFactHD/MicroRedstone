package io.github.xfacthd.microredstone.common.circuit.assembler;

import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class WireMapper
{
    private final Reference2IntMap<Wire> assignedWires = new Reference2IntOpenHashMap<>();
    private int wireCounter = 0;

    public int resolveWire(Wire wire)
    {
        return assignedWires.computeIfAbsent(wire, _ ->
        {
            int value = wireCounter;
            wireCounter++;
            return value;
        });
    }

    public int size()
    {
        return assignedWires.size();
    }

    public List<Wire> getSortedWireCopies()
    {
        return assignedWires.reference2IntEntrySet()
                .stream()
                .sorted(Comparator.comparingInt(Reference2IntMap.Entry::getIntValue))
                .map(Map.Entry::getKey)
                .map(Wire::copy)
                .toList();
    }
}
