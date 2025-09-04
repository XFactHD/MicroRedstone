package io.github.xfacthd.microredstone.client.net;

import io.github.xfacthd.microredstone.common.data.library.ClientCircuitLibrary;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryUpdatePayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandler
{
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event)
    {
        event.register(ClientboundCircuitLibraryPayload.TYPE, ClientNetworkHandler::handleCircuitLibrary);
        event.register(ClientboundCircuitLibraryUpdatePayload.TYPE, ClientNetworkHandler::handleCircuitLibraryUpdate);
    }

    private static void handleCircuitLibrary(ClientboundCircuitLibraryPayload payload, IPayloadContext ctx)
    {
        ClientCircuitLibrary.handleInitialSync(payload.entries());
    }

    private static void handleCircuitLibraryUpdate(ClientboundCircuitLibraryUpdatePayload payload, IPayloadContext ctx)
    {
        ClientCircuitLibrary.handleUpdateSync(payload.addedOrModified(), payload.removed());
    }

    private ClientNetworkHandler() {}
}
