package io.github.xfacthd.microredstone.client.net;

import io.github.xfacthd.microredstone.client.screen.microchip.MicrochipCircuitScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.ExportTarget;
import io.github.xfacthd.microredstone.client.screen.workbench.ImportExportHandler;
import io.github.xfacthd.microredstone.common.data.library.ClientCircuitLibrary;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundCircuitLibraryUpdatePayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundMicrochipChangeCircuitPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundMicrochipUpdateWireStatesPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundModifyCircuitLibraryResultPayload;
import io.github.xfacthd.microredstone.common.net.payload.clientbound.ClientboundWorkbenchWriteCircuitResultPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientNetworkHandler
{
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event)
    {
        event.register(ClientboundCircuitLibraryPayload.TYPE, ClientNetworkHandler::handleCircuitLibrary);
        event.register(ClientboundCircuitLibraryUpdatePayload.TYPE, ClientNetworkHandler::handleCircuitLibraryUpdate);
        event.register(ClientboundWorkbenchWriteCircuitResultPayload.TYPE, ClientNetworkHandler::handleCircuitWriteResult);
        event.register(ClientboundModifyCircuitLibraryResultPayload.TYPE, ClientNetworkHandler::handleModifyCircuitLibraryResult);
        event.register(ClientboundMicrochipChangeCircuitPayload.TYPE, ClientNetworkHandler::handleMicrochipChangeCircuit);
        event.register(ClientboundMicrochipUpdateWireStatesPayload.TYPE, ClientNetworkHandler::handleMicrochipUpdateWireStates);
    }

    private static void handleCircuitLibrary(ClientboundCircuitLibraryPayload payload, IPayloadContext ctx)
    {
        ClientCircuitLibrary.handleInitialSync(payload.entries());
    }

    private static void handleCircuitLibraryUpdate(ClientboundCircuitLibraryUpdatePayload payload, IPayloadContext ctx)
    {
        ClientCircuitLibrary.handleUpdateSync(payload.addedOrModified(), payload.removed());
    }

    private static void handleCircuitWriteResult(ClientboundWorkbenchWriteCircuitResultPayload payload, IPayloadContext ctx)
    {
        if (Minecraft.getInstance().screen instanceof CircuitWorkbenchScreen screen && screen.getMenu().containerId == payload.containerId())
        {
            ImportExportHandler.displayExportResult(ImportExportHandler.ExportResult.ofServerResponse(payload.success(), ExportTarget.CIRCUIT_ITEM));
        }
    }

    private static void handleModifyCircuitLibraryResult(ClientboundModifyCircuitLibraryResultPayload payload, IPayloadContext ctx)
    {
        if (Minecraft.getInstance().screen instanceof CircuitWorkbenchScreen screen && screen.getImportExportHandler().isWaitingForExportResult())
        {
            ImportExportHandler.displayExportResult(ImportExportHandler.ExportResult.ofServerResponse(payload.success(), ExportTarget.LIBRARY));
        }
    }

    private static void handleMicrochipChangeCircuit(ClientboundMicrochipChangeCircuitPayload payload, IPayloadContext ctx)
    {
        if (Minecraft.getInstance().screen instanceof MicrochipCircuitScreen screen && screen.getMenu().containerId == payload.containerId())
        {
            screen.handleCircuitUpdate(payload.rootNode().orElse(null));
        }
    }

    private static void handleMicrochipUpdateWireStates(ClientboundMicrochipUpdateWireStatesPayload payload, IPayloadContext ctx)
    {
        if (Minecraft.getInstance().screen instanceof MicrochipCircuitScreen screen && screen.getMenu().containerId == payload.containerId())
        {
            screen.handleWireStateUpdate(payload.wireStates());
        }
    }

    private ClientNetworkHandler() {}
}
