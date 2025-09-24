package io.github.xfacthd.microredstone.common.circuit.prototype;

import com.mojang.serialization.MapCodec;

public record ProtoNodeType<T extends PrototypeNode.Serializable>(MapCodec<T> codec) { }
