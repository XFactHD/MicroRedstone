package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.datafixers.util.Either;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.net.payload.serverbound.ServerboundModifyCircuitLibraryPayload;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ClientCircuitLibrary {
    private static final Map<UUID, CircuitLibraryEntry> ENTRIES_BY_ID = new Object2ObjectOpenHashMap<>();
    private static final Map<String, CircuitLibraryEntry> OWNED_ENTRIES_BY_NAME = new Object2ObjectOpenHashMap<>();

    public static void getFilteredEntries(Predicate<ShareType> filter, Consumer<CircuitLibraryEntry> consumer) {
        for (CircuitLibraryEntry value : ENTRIES_BY_ID.values()) {
            if (filter.test(value.shareInfo().type())) {
                consumer.accept(value);
            }
        }
        // TODO: implement built-in entry handling
    }

    public static boolean hasEntryWithName(String name) {
        return OWNED_ENTRIES_BY_NAME.containsKey(name);
    }

    public static CircuitLibraryEntry getEntryById(UUID id) {
        return Objects.requireNonNull(ENTRIES_BY_ID.get(id));
    }

    public static void addOrModifyCircuit(CompoundCircuitNode circuitNode) {
        CircuitLibraryEntry entry;
        CircuitLibraryEntry existing = OWNED_ENTRIES_BY_NAME.get(circuitNode.getName());
        if (existing != null) {
            entry = existing.modifyCircuit(circuitNode);
        } else {
            UUID player = Minecraft.getInstance().getGameProfile().id();
            entry = CircuitLibraryEntry.createCircuit(circuitNode, player);
        }
        ClientPacketDistributor.sendToServer(new ServerboundModifyCircuitLibraryPayload(Either.left(entry)));
    }

    public static void modifyShareInfo(String name, ShareInfo shareInfo) {
        CircuitLibraryEntry entry = OWNED_ENTRIES_BY_NAME.get(name);
        if (entry != null) {
            entry = entry.modifyShareInfo(shareInfo);
            ClientPacketDistributor.sendToServer(new ServerboundModifyCircuitLibraryPayload(Either.left(entry)));
        }
    }

    public static void removeCircuit(String name) {
        if (OWNED_ENTRIES_BY_NAME.containsKey(name)) {
            ClientPacketDistributor.sendToServer(new ServerboundModifyCircuitLibraryPayload(Either.right(name)));
        }
    }

    public static void handleInitialSync(List<CircuitLibraryEntry> entries) {
        ENTRIES_BY_ID.clear();
        OWNED_ENTRIES_BY_NAME.clear();
        UUID player = Minecraft.getInstance().getGameProfile().id();
        for (CircuitLibraryEntry entry : entries) {
            ENTRIES_BY_ID.put(entry.id(), entry);
            if (entry.author().equals(player)) {
                OWNED_ENTRIES_BY_NAME.put(entry.name(), entry);
            }
        }
    }

    public static void handleUpdateSync(List<CircuitLibraryEntry> addedOrModified, List<UUID> removed) {
        UUID player = Minecraft.getInstance().getGameProfile().id();
        for (CircuitLibraryEntry entry : addedOrModified) {
            ENTRIES_BY_ID.put(entry.id(), entry);
            if (entry.author().equals(player)) {
                OWNED_ENTRIES_BY_NAME.put(entry.name(), entry);
            }
        }
        for (UUID id : removed) {
            CircuitLibraryEntry entry = ENTRIES_BY_ID.remove(id);
            if (entry != null && entry.author().equals(player)) {
                OWNED_ENTRIES_BY_NAME.remove(entry.name());
            }
        }

        Minecraft.getInstance().schedule(() -> {
            if (Minecraft.getInstance().gui.screen() instanceof CircuitWorkbenchScreen workbench) {
                workbench.getToolPane().getPartsList().updateImportList();
            }
        });
    }

    public static void onPlayerDisconnect(@SuppressWarnings("unused") ClientPlayerNetworkEvent.LoggingOut event) {
        ENTRIES_BY_ID.clear();
        OWNED_ENTRIES_BY_NAME.clear();
    }

    private ClientCircuitLibrary() { }
}
