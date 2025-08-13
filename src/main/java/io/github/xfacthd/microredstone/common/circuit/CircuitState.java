package io.github.xfacthd.microredstone.common.circuit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.util.SerdesUtils;

public record CircuitState(int[] bufferStates, int[] clockCounters, int[] clockStates)
{
    public static final Codec<CircuitState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            SerdesUtils.INT_ARRAY_CODEC.fieldOf("buffer_states").forGetter(CircuitState::bufferStates),
            SerdesUtils.INT_ARRAY_CODEC.fieldOf("clock_counters").forGetter(CircuitState::clockCounters),
            SerdesUtils.INT_ARRAY_CODEC.fieldOf("clock_states").forGetter(CircuitState::clockStates)
    ).apply(inst, CircuitState::new));
    public static final CircuitState EMPTY = new CircuitState(new int[0], new int[0], new int[0]);
}
