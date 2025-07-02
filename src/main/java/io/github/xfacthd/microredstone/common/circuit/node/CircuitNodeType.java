package io.github.xfacthd.microredstone.common.circuit.node;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CircuitNodeType<T extends CircuitNode>(MapCodec<T> codec, StreamCodec<ByteBuf, T> streamCodec) { }
