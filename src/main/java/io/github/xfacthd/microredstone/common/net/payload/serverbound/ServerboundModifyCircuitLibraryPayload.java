package io.github.xfacthd.microredstone.common.net.payload.serverbound;

import com.mojang.datafixers.util.Either;
import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.data.library.ServerCircuitLibrary;
import io.github.xfacthd.microredstone.common.util.Utils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Objects;

public record ServerboundModifyCircuitLibraryPayload(Either<CircuitLibraryEntry, String> entry) implements CustomPacketPayload
{
    public static final Type<ServerboundModifyCircuitLibraryPayload> TYPE = Utils.payloadType("serverbound_modify_circuit_library");
    public static final StreamCodec<ByteBuf, ServerboundModifyCircuitLibraryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.either(CircuitLibraryEntry.STREAM_CODEC, ByteBufCodecs.STRING_UTF8),
            ServerboundModifyCircuitLibraryPayload::entry,
            ServerboundModifyCircuitLibraryPayload::new
    );

    public void handle(IPayloadContext ctx)
    {
        if (!(ctx.player() instanceof ServerPlayer player)) throw new IllegalStateException("");

        MinecraftServer server = Objects.requireNonNull(player.getServer());
        ServerCircuitLibrary library = ServerCircuitLibrary.get(server);
        entry.ifLeft(entry -> library.addOrModifyEntry(player, entry))
                .ifRight(entry -> library.removeEntry(player, entry));
    }

    @Override
    public Type<ServerboundModifyCircuitLibraryPayload> type()
    {
        return TYPE;
    }
}
