package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.net.MRStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.time.Instant;
import java.util.UUID;

public record CircuitLibraryEntry(
        UUID id,
        String name,
        CompoundCircuitNode circuitNode,
        UUID author,
        Instant timeCreated,
        Instant timeModified,
        ShareInfo shareInfo
)
{
    static final Codec<CircuitLibraryEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CircuitLibraryEntry::id),
            Codec.STRING.fieldOf("name").forGetter(CircuitLibraryEntry::name),
            CompoundCircuitNode.CODEC.codec().fieldOf("circuit").forGetter(CircuitLibraryEntry::circuitNode),
            UUIDUtil.CODEC.fieldOf("author").forGetter(CircuitLibraryEntry::author),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_created").forGetter(CircuitLibraryEntry::timeCreated),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_modified").forGetter(CircuitLibraryEntry::timeModified),
            ShareInfo.CODEC.fieldOf("share_info").forGetter(CircuitLibraryEntry::shareInfo)
    ).apply(inst, CircuitLibraryEntry::new));
    public static final StreamCodec<ByteBuf, CircuitLibraryEntry> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CircuitLibraryEntry::id,
            ByteBufCodecs.STRING_UTF8,
            CircuitLibraryEntry::name,
            CompoundCircuitNode.STREAM_CODEC,
            CircuitLibraryEntry::circuitNode,
            UUIDUtil.STREAM_CODEC,
            CircuitLibraryEntry::author,
            MRStreamCodecs.INSTANT,
            CircuitLibraryEntry::timeCreated,
            MRStreamCodecs.INSTANT,
            CircuitLibraryEntry::timeModified,
            ShareInfo.STREAM_CODEC,
            CircuitLibraryEntry::shareInfo,
            CircuitLibraryEntry::new
    );

    public static CircuitLibraryEntry createCircuit(String name, CompoundCircuitNode circuitNode, UUID author)
    {
        Instant time = Instant.now();
        return new CircuitLibraryEntry(Util.NIL_UUID, name, circuitNode, author, time, time, ShareInfo.Private.INSTANCE);
    }

    public CircuitLibraryEntry modifyCircuit(CompoundCircuitNode circuitNode)
    {
        return new CircuitLibraryEntry(id, name, circuitNode, author, timeCreated, Instant.now(), shareInfo);
    }

    public CircuitLibraryEntry modifyShareInfo(ShareInfo shareInfo)
    {
        return new CircuitLibraryEntry(id, name, circuitNode, author, timeCreated, Instant.now(), shareInfo);
    }

    CircuitLibraryEntry withId(UUID id)
    {
        return new CircuitLibraryEntry(id, name, circuitNode, author, timeCreated, timeModified, shareInfo);
    }

    CircuitLibraryEntry withoutShareTargets()
    {
        if (shareInfo.type() == ShareType.SHARED)
        {
            return new CircuitLibraryEntry(id, name, circuitNode, author, timeCreated, timeModified, shareInfo.withoutShareTargets());
        }
        return this;
    }
}
