package io.github.xfacthd.microredstone.common.circuit;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;

public final class WireStates
{
    public static final StreamCodec<ByteBuf, WireStates> STREAM_CODEC = streamCodec();
    private static final int ADDRESS_BITS_PER_WORD = 6;

    private final long[] words;

    public WireStates(int wireCount)
    {
        this(new long[(wireCount + 63) >> ADDRESS_BITS_PER_WORD]);
    }

    private WireStates(long[] words)
    {
        this.words = words;
    }

    public void set(int bitIdx, int value)
    {
        int wordIdx = wordIndex(bitIdx);
        if (value > 0)
        {
            words[wordIdx] |= (1L << bitIdx);
        }
        else
        {
            words[wordIdx] &= ~(1L << bitIdx);
        }
    }

    public boolean get(int bitIdx)
    {
        return (words[wordIndex(bitIdx)] & (1L << bitIdx)) != 0;
    }

    private static int wordIndex(int bitIdx)
    {
        return bitIdx >> ADDRESS_BITS_PER_WORD;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof WireStates other)) return false;
        return Arrays.equals(words, other.words);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(words);
    }

    private static StreamCodec<ByteBuf, WireStates> streamCodec()
    {
        return new StreamCodec<>()
        {
            @Override
            public WireStates decode(ByteBuf buffer)
            {
                int wordCount = VarInt.read(buffer);
                long[] words = new long[wordCount];
                for (int i = 0; i < wordCount; i++)
                {
                    words[i] = buffer.readLong();
                }
                return new WireStates(words);
            }

            @Override
            public void encode(ByteBuf buffer, WireStates value)
            {
                VarInt.write(buffer, value.words.length);
                for (long word : value.words)
                {
                    buffer.writeLong(word);
                }
            }
        };
    }
}
