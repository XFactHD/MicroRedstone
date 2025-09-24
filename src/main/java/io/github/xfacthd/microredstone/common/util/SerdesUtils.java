package io.github.xfacthd.microredstone.common.util;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public final class SerdesUtils
{
    public static final Codec<int[]> INT_ARRAY_CODEC = Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of);

    public static <T> void readTypedArray(ValueInput input, String key, Codec<T> codec, T[] array)
    {
        int[] index = new int[] { 0 };
        input.listOrEmpty(key, codec).forEach(value ->
        {
            array[index[0]] = value;
            index[0]++;
        });
    }

    public static <T> void writeTypedArray(ValueOutput output, String key, Codec<T> codec, T[] array)
    {
        ValueOutput.TypedOutputList<T> list = output.list(key, codec);
        for (T value : array)
        {
            list.add(value);
        }
    }

    public static void readShortArray(ValueInput input, String key, short[] arrOut)
    {
        input.getIntArray(key).ifPresent(arrIn ->
        {
            for (int i = 0; i < arrIn.length; i++)
            {
                arrOut[i] = (short) arrIn[i];
            }
        });
    }

    public static void writeShortArray(ValueOutput output, String key, short[] arrIn)
    {
        int[] arrOut = new int[arrIn.length];
        for (int i = 0; i < arrIn.length; i++)
        {
            arrOut[i] = arrIn[i];
        }
        output.putIntArray(key, arrOut);
    }

    public static <T> Codec<T[]> arrayCodec(Codec<T> codec, IntFunction<T[]> arrayFactory)
    {
        return codec.listOf().xmap(list -> list.toArray(arrayFactory), List::of);
    }

    public static <T> Codec<Set<T>> setCodec(Codec<T> codec)
    {
        return setCodec(codec, Set::copyOf);
    }

    public static <T> Codec<Set<T>> setCodec(Codec<T> codec, Function<List<T>, Set<T>> setFactory)
    {
        return codec.listOf().xmap(setFactory, List::copyOf);
    }

    private SerdesUtils() { }
}
