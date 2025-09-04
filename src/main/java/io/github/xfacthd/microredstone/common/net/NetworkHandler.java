package io.github.xfacthd.microredstone.common.net;

import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryUpdatePayload;
import io.github.xfacthd.microredstone.common.net.payload.serverbound.ServerboundModifyCircuitLibraryPayload;
import io.github.xfacthd.microredstone.common.net.task.SyncCircuitLibraryTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler
{
    private static final String VERSION = "1";

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
    {
        event.registrar(VERSION)
                .configurationToClient(
                        ClientboundCircuitLibraryPayload.TYPE,
                        ClientboundCircuitLibraryPayload.STREAM_CODEC
                )
                .playToClient(
                        ClientboundCircuitLibraryUpdatePayload.TYPE,
                        ClientboundCircuitLibraryUpdatePayload.STREAM_CODEC
                )
                .playToServer(
                        ServerboundModifyCircuitLibraryPayload.TYPE,
                        ServerboundModifyCircuitLibraryPayload.STREAM_CODEC,
                        ServerboundModifyCircuitLibraryPayload::handle
                );
    }

    public static void onRegisterConfigTasks(RegisterConfigurationTasksEvent event)
    {
        event.register(new SyncCircuitLibraryTask(event.getListener()));
    }

    private NetworkHandler() {}
}
