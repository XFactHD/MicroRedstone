package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryUpdatePayload;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ServerCircuitLibrary extends SavedData
{
    private static final Codec<Map<UUID, PlayerCircuitLibrary>> CODEC = PlayerCircuitLibrary.CODEC.listOf().xmap(
            ServerCircuitLibrary::deserialize,
            ServerCircuitLibrary::serialize
    );
    private static final SavedDataType<ServerCircuitLibrary> TYPE = new SavedDataType<>(
            "microredstone_circuit_library",
            ctx -> new ServerCircuitLibrary(ctx, new Object2ObjectOpenHashMap<>()),
            ctx -> CODEC.xmap(
                    libraries -> new ServerCircuitLibrary(ctx, libraries),
                    ServerCircuitLibrary::getLibraries
            ),
            null
    );

    private final MinecraftServer server;
    private final Map<UUID, PlayerCircuitLibrary> playerLibraries;

    public static ServerCircuitLibrary get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void addOrModifyEntry(ServerPlayer player, CircuitLibraryEntry entry)
    {
        entry = getOrCreateLibrary(player.getUUID()).addOrModifyEntry(entry);
        if (entry != null)
        {
            sendUpdatePacket(player, List.of(entry), List.of());
            updateReferences(Set.of(entry), Map.of());
            setDirty();
        }
    }

    public void removeEntry(ServerPlayer player, String name)
    {
        UUID playerId = player.getUUID();
        PlayerCircuitLibrary library = playerLibraries.get(playerId);
        if (library == null) return;

        UUID id = library.removeEntry(name);
        if (id != null)
        {
            sendUpdatePacket(player, List.of(), List.of(id));
            updateReferences(Set.of(), Map.of(playerId, Set.of(id)));
            setDirty();
        }
    }

    public List<CircuitLibraryEntry> getEntriesForPlayer(UUID player)
    {
        PlayerCircuitLibrary library = playerLibraries.get(player);
        return library != null ? library.packEntries(playerLibraries.values()) : List.of();
    }

    private ServerCircuitLibrary(Context ctx, Map<UUID, PlayerCircuitLibrary> playerLibraries)
    {
        this.server = ctx.levelOrThrow().getServer();
        this.playerLibraries = playerLibraries;
    }

    private PlayerCircuitLibrary getOrCreateLibrary(UUID player)
    {
        return playerLibraries.computeIfAbsent(player, PlayerCircuitLibrary::new);
    }

    private void updateReferences(Set<CircuitLibraryEntry> addedOrModified, Map<UUID, Set<UUID>> removed)
    {
        Map<UUID, UpdateInfo> updateInfos = new Object2ObjectOpenHashMap<>();
        playerLibraries.forEach((owner, library) ->
        {
            UpdateInfo info = library.updateReferences(addedOrModified, removed);
            updateInfos.put(owner, info);
        });
        updateInfos.forEach((owner, info) ->
        {
            if (info.isEmpty()) return;

            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player != null)
            {
                sendUpdatePacket(player, info.addedOrModified, info.removed);
            }
        });
    }

    private static void sendUpdatePacket(ServerPlayer player, List<CircuitLibraryEntry> addedOrModified, List<UUID> removed)
    {
        PacketDistributor.sendToPlayer(player, new ClientboundCircuitLibraryUpdatePayload(addedOrModified, removed));
    }

    private Map<UUID, PlayerCircuitLibrary> getLibraries()
    {
        return playerLibraries;
    }

    private static List<PlayerCircuitLibrary> serialize(Map<UUID, PlayerCircuitLibrary> libraries)
    {
        return List.copyOf(libraries.values());
    }

    private static Map<UUID, PlayerCircuitLibrary> deserialize(List<PlayerCircuitLibrary> libraries)
    {
        Map<UUID, PlayerCircuitLibrary> map = new Object2ObjectOpenHashMap<>();
        for (PlayerCircuitLibrary library : libraries)
        {
            map.putIfAbsent(library.getOwner(), library);
        }
        return map;
    }

    static final class UpdateInfo
    {
        private final List<CircuitLibraryEntry> addedOrModified = new ArrayList<>();
        private final List<UUID> removed = new ArrayList<>();

        void captureAddedOrModified(CircuitLibraryEntry entry)
        {
            addedOrModified.add(entry);
        }

        void captureRemoved(UUID id)
        {
            removed.add(id);
        }

        private boolean isEmpty()
        {
            return addedOrModified.isEmpty() && removed.isEmpty();
        }
    }
}
