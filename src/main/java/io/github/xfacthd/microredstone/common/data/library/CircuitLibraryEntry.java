package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.net.MRStreamCodecs;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CircuitLibraryEntry(
        UUID id,
        CompoundCircuitNode circuitNode,
        UUID author,
        Instant timeCreated,
        Instant timeModified,
        ShareInfo shareInfo
)
{
    static final Codec<CircuitLibraryEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(CircuitLibraryEntry::id),
            CompoundCircuitNode.CODEC.codec().fieldOf("circuit").forGetter(CircuitLibraryEntry::circuitNode),
            UUIDUtil.CODEC.fieldOf("author").forGetter(CircuitLibraryEntry::author),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_created").forGetter(CircuitLibraryEntry::timeCreated),
            ExtraCodecs.INSTANT_ISO8601.fieldOf("time_modified").forGetter(CircuitLibraryEntry::timeModified),
            ShareInfo.CODEC.fieldOf("share_info").forGetter(CircuitLibraryEntry::shareInfo)
    ).apply(inst, CircuitLibraryEntry::new));
    public static final StreamCodec<ByteBuf, CircuitLibraryEntry> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CircuitLibraryEntry::id,
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

    public static CircuitLibraryEntry createCircuit(CompoundCircuitNode circuitNode, UUID author)
    {
        Instant time = Instant.now();
        return new CircuitLibraryEntry(Util.NIL_UUID, circuitNode, author, time, time, ShareInfo.Private.INSTANCE);
    }

    public String name()
    {
        return circuitNode.getName();
    }

    public CircuitLibraryEntry modifyCircuit(CompoundCircuitNode circuitNode)
    {
        return new CircuitLibraryEntry(id, circuitNode, author, timeCreated, Instant.now(), shareInfo);
    }

    public CircuitLibraryEntry modifyShareInfo(ShareInfo shareInfo)
    {
        if (!this.shareInfo.equals(shareInfo))
        {
            return new CircuitLibraryEntry(id, circuitNode, author, timeCreated, Instant.now(), shareInfo);
        }
        return this;
    }

    CircuitLibraryEntry withId(UUID id)
    {
        if (!this.id.equals(id))
        {
            return new CircuitLibraryEntry(id, circuitNode, author, timeCreated, timeModified, shareInfo);
        }
        return this;
    }

    CircuitLibraryEntry withoutShareTargets()
    {
        if (shareInfo instanceof ShareInfo.Shared(Set<UUID> sharedTo) && !sharedTo.isEmpty())
        {
            return new CircuitLibraryEntry(id, circuitNode, author, timeCreated, timeModified, shareInfo.withoutShareTargets());
        }
        return this;
    }
}
