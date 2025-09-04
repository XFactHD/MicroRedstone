package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PlayerCircuitLibrary
{
    static final Codec<PlayerCircuitLibrary> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(PlayerCircuitLibrary::getOwner),
            Codec.unboundedMap(
                    UUIDUtil.STRING_CODEC,
                    CircuitLibraryEntry.CODEC
            ).fieldOf("entries").forGetter(library -> library.ownedById)
    ).apply(inst, PlayerCircuitLibrary::new));

    private final UUID owner;
    private final Map<UUID, CircuitLibraryEntry> ownedById;
    private final Map<String, CircuitLibraryEntry> ownedByName = new Object2ObjectOpenHashMap<>();
    private final Map<UUID, Map<UUID, CircuitLibraryEntry>> foreignEntries = new Object2ObjectOpenHashMap<>();
    private boolean prepared = false;

    private PlayerCircuitLibrary(UUID owner, Map<UUID, CircuitLibraryEntry> ownedById)
    {
        this.owner = owner;
        this.ownedById = new Object2ObjectOpenHashMap<>(ownedById);
        for (CircuitLibraryEntry entry : ownedById.values())
        {
            ownedByName.put(entry.name(), entry);
        }
    }

    PlayerCircuitLibrary(UUID owner)
    {
        this.owner = owner;
        this.ownedById = new Object2ObjectOpenHashMap<>();
    }

    UUID getOwner()
    {
        return owner;
    }

    @Nullable
    CircuitLibraryEntry addOrModifyEntry(CircuitLibraryEntry entry)
    {
        if (entry.author().equals(owner))
        {
            CircuitLibraryEntry existing = ownedByName.get(entry.name());
            if (existing != null)
            {
                entry = entry.withId(existing.id());
            }
            else
            {
                entry = entry.withId(UUID.randomUUID());
            }
            ownedById.put(entry.id(), entry);
            ownedByName.put(entry.name(), entry);
            return entry.withId(UUID.randomUUID());
        }
        return null;
    }

    @Nullable
    UUID removeEntry(String name)
    {
        CircuitLibraryEntry entry = ownedByName.remove(name);
        if (entry != null)
        {
            ownedById.remove(entry.id());
            return entry.id();
        }
        return null;
    }

    List<CircuitLibraryEntry> packEntries(Collection<PlayerCircuitLibrary> libraries)
    {
        if (!prepared)
        {
            collectReferences(libraries);
            prepared = true;
        }

        if (ownedById.isEmpty() && foreignEntries.isEmpty()) return List.of();

        List<CircuitLibraryEntry> entries = new ArrayList<>(ownedById.values());
        for (Map<UUID, CircuitLibraryEntry> map : foreignEntries.values())
        {
            for (CircuitLibraryEntry entry : map.values())
            {
                entries.add(entry.withoutShareTargets());
            }
        }
        return entries;
    }

    private void collectReferences(Collection<PlayerCircuitLibrary> libraries)
    {
        for (PlayerCircuitLibrary library : libraries)
        {
            if (library == this) continue;

            Map<UUID, CircuitLibraryEntry> foreign = foreignEntries.computeIfAbsent(library.owner, $ -> new Object2ObjectOpenHashMap<>());
            library.ownedById.forEach((id, entry) ->
            {
                if (entry.shareInfo().isVisibleTo(entry.author(), owner))
                {
                    foreign.put(id, entry);
                }
            });
        }
        foreignEntries.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    ServerCircuitLibrary.UpdateInfo updateReferences(Set<CircuitLibraryEntry> addedOrModified, Map<UUID, Set<UUID>> removed)
    {
        ServerCircuitLibrary.UpdateInfo info = new ServerCircuitLibrary.UpdateInfo();
        if (!prepared) return info;

        for (CircuitLibraryEntry entry : addedOrModified)
        {
            if (entry.author().equals(owner)) continue;

            Map<UUID, CircuitLibraryEntry> foreign = foreignEntries.computeIfAbsent(entry.author(), $ -> new Object2ObjectOpenHashMap<>());
            if (entry.shareInfo().isVisibleTo(entry.author(), owner))
            {
                foreign.put(entry.id(), entry);
                info.captureAddedOrModified(entry.withoutShareTargets());
            }
            else
            {
                if (foreign.remove(entry.id()) != null)
                {
                    info.captureRemoved(entry.id());
                }
            }
        }
        for (Map.Entry<UUID, Set<UUID>> entry : removed.entrySet())
        {
            UUID author = entry.getKey();
            if (author.equals(owner)) continue;

            Map<UUID, CircuitLibraryEntry> entryMap = foreignEntries.get(author);
            if (entryMap == null) continue;

            for (UUID id : entry.getValue())
            {
                if (entryMap.remove(id) != null)
                {
                    info.captureRemoved(id);
                }
            }
        }
        foreignEntries.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return info;
    }
}
