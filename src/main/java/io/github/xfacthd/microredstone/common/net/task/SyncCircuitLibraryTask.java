package io.github.xfacthd.microredstone.common.net.task;

import io.github.xfacthd.microredstone.common.data.library.ServerCircuitLibrary;
import io.github.xfacthd.microredstone.common.data.library.CircuitLibraryEntry;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryPayload;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.List;
import java.util.function.Consumer;

public final class SyncCircuitLibraryTask implements ConfigurationTask
{
    private static final Type TYPE = Utils.configTaskType("sync_circuit_library");

    private final ServerConfigurationPacketListenerImpl listener;
    private final MinecraftServer server;

    public SyncCircuitLibraryTask(ServerConfigurationPacketListener listener)
    {
        this.listener = (ServerConfigurationPacketListenerImpl) listener;
        this.server = (MinecraftServer) listener.getMainThreadEventLoop();
    }

    @Override
    public void start(Consumer<Packet<?>> sender)
    {
        ServerCircuitLibrary library = ServerCircuitLibrary.get(server);
        List<CircuitLibraryEntry> entries = library.getEntriesForPlayer(listener.getOwner().getId());
        if (!entries.isEmpty())
        {
            sender.accept(new ClientboundCircuitLibraryPayload(entries).toVanillaClientbound());
        }
        listener.finishCurrentTask(TYPE);
    }

    @Override
    public Type type()
    {
        return TYPE;
    }
}
