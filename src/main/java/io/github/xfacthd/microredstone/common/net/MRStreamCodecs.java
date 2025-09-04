package io.github.xfacthd.microredstone.common.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.time.Instant;
import java.util.function.IntFunction;

public final class MRStreamCodecs
{
    public static final StreamCodec<ByteBuf, Instant> INSTANT = ByteBufCodecs.VAR_LONG.map(Instant::ofEpochSecond, Instant::getEpochSecond);

    public static <B extends ByteBuf, T> StreamCodec<B, T[]> array(StreamCodec<B, T> codec, IntFunction<T[]> arrayFactory)
    {
        return new StreamCodec<>()
        {
            @Override
            public T[] decode(B buffer)
            {
                int count = VarInt.read(buffer);
                T[] arr = arrayFactory.apply(count);
                for (int i = 0; i < count; i++)
                {
                    arr[i] = codec.decode(buffer);
                }
                return arr;
            }

            @Override
            public void encode(B buffer, T[] value)
            {
                VarInt.write(buffer, value.length);
                for (T entry : value)
                {
                    codec.encode(buffer, entry);
                }
            }
        };
    }

    private MRStreamCodecs() { }
}
