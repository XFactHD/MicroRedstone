package io.github.xfacthd.microredstone.client.screen.workbench;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartGrid;
import io.github.xfacthd.microredstone.client.screen.workbench.part.PartSetMode;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.client.screen.workbench.wire.WireGrid;
import io.github.xfacthd.microredstone.common.MRContent;
import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitAssembler;
import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitValidator;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.Connector;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.Wire;
import io.github.xfacthd.microredstone.common.circuit.connection.WirePair;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.node.special.CompoundCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.node.special.LampCircuitNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ReferencePrototypeNode;
import io.github.xfacthd.microredstone.common.data.component.StoredCircuit;
import io.github.xfacthd.microredstone.common.data.library.ClientCircuitLibrary;
import io.github.xfacthd.microredstone.common.menu.CircuitWorkbenchMenu;
import io.github.xfacthd.microredstone.common.net.payload.serverbound.ServerboundWorkbenchWriteCircuitPayload;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ImportExportHandler
{
    public static final Component TITLE_CONFIRM_IMPORT = Utils.translate("title", "circuit_workbench.library_browser.import_action.import_overwrite.confirm");
    public static final Component MESSAGE_CONFIRM_IMPORT_LINE_ONE = Utils.translate("msg", "circuit_workbench.library_browser.import_action.import_overwrite.confirm_line_one");
    public static final Component MESSAGE_CONFIRM_IMPORT_LINE_TWO = Utils.translate("msg", "circuit_workbench.library_browser.import_action.import_overwrite.confirm_line_two");
    public static final Component TITLE_IMPORT_BROKEN = Utils.translate("title", "circuit_workbench.library_browser.import_action.broken");
    public static final Component MESSAGE_IMPORT_BROKEN = Utils.translate("msg", "circuit_workbench.library_browser.import_action.broken");
    public static final Component TITLE_CONFIRM_EXPORT = Utils.translate("title", "circuit_workbench.library_browser.export_action.export_overwrite.confirm");
    public static final Component MESSAGE_CONFIRM_EXPORT = Utils.translate("msg", "circuit_workbench.library_browser.export_action.export_overwrite.confirm");
    public static final Component TITLE_IMPORT_ERROR = Utils.translate("title", "circuit_workbench.library_browser.import_action.error");
    public static final Component TITLE_EXPORT_SUCCESS = Utils.translate("title", "circuit_workbench.library_browser.export_action.success");
    public static final Component TITLE_EXPORT_ERROR = Utils.translate("title", "circuit_workbench.library_browser.export_action.error");
    private static final Codec<CompoundCircuitNode> NODE_CODEC = CompoundCircuitNode.CODEC.codec();

    private final CircuitWorkbenchScreen owner;
    private final CircuitWorkbenchMenu menu;
    private final CircuitCanvas canvas;
    // TODO: inhibit editing while waiting for server response
    private boolean waitingForExportResult = false;

    ImportExportHandler(CircuitWorkbenchScreen owner)
    {
        this.owner = owner;
        this.menu = owner.getMenu();
        this.canvas = owner.getCanvas();
    }

    public void importCircuitFromItem(ItemStack stack)
    {
        StoredCircuit circuit = stack.get(MRContent.DC_TYPE_CIRCUIT);
        if (circuit != null && circuit.rootNode() != null)
        {
            importCircuit(circuit.rootNode());
        }
    }

    public void importCircuitFromClipboard()
    {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard.isEmpty()) return;

        JsonObject json;
        try
        {
            json = GsonHelper.parse(clipboard);
        }
        catch (JsonParseException e)
        {
            displayImportError(ImportError.PARSE_FAILED);
            return;
        }
        CompoundCircuitNode circuitNode;
        try
        {
            DataResult<Pair<CompoundCircuitNode, JsonElement>> result = NODE_CODEC.decode(JsonOps.INSTANCE, json);
            if (result.isError() || result.result().isEmpty())
            {
                displayImportError(ImportError.DECODE_FAILED);
                return;
            }
            circuitNode = result.getOrThrow().getFirst();
        }
        catch (Throwable t)
        {
            displayImportError(ImportError.DECODE_FAILED);
            return;
        }

        if (!CircuitValidator.validate(circuitNode))
        {
            displayImportError(ImportError.VALIDATE_FAILED);
            return;
        }

        importCircuit(circuitNode);
    }

    public void importCircuit(CompoundCircuitNode circuitNode)
    {
        if (canvas.isEmpty())
        {
            doImportCircuit(circuitNode);
            return;
        }

        DialogScreen.builder(DialogScreen.Type.CONFIRM)
                .withTitle(TITLE_CONFIRM_IMPORT)
                .withMessage(MESSAGE_CONFIRM_IMPORT_LINE_ONE)
                .withMessage(MESSAGE_CONFIRM_IMPORT_LINE_TWO)
                .withOkCallback(() -> doImportCircuit(circuitNode))
                .show();
    }

    private void doImportCircuit(CompoundCircuitNode circuitNode)
    {
        canvas.clear();

        PartGrid partGrid = canvas.getPartGrid();
        WireGrid wireGrid = canvas.getWireGrid();
        MutableBoolean probablyBroken = new MutableBoolean();

        List<Wire> wires = circuitNode.getWires()
                .stream()
                .map(wireGrid::importWire)
                .toList();
        circuitNode.forAllNodes(entry ->
        {
            PrototypeNode protoNode = entry.node().disassemble();
            if (protoNode instanceof ReferencePrototypeNode)
            {
                // CompoundCircuitNodes are weird, their Connectors refer to the internal wire instead of the external one like everything else
                resolveReferenceNodeWires(protoNode, wires, entry.node().getInputs(), entry.inputs(), probablyBroken);
                resolveReferenceNodeWires(protoNode, wires, entry.node().getOutputs(), entry.outputs(), probablyBroken);
            }
            else
            {
                for (Connector con : Utils.concatArrays(entry.node().getInputs(), entry.node().getOutputs()))
                {
                    protoNode.setConnection(con.port(), wires.get(con.wire()), true);
                }
            }
            partGrid.setPartNode(entry.pos(), protoNode, entry.rotation(), PartSetMode.ADD);
            if (entry.node() instanceof LampCircuitNode lamp && protoNode instanceof LampPrototypeNode protoLamp)
            {
                importChainedLamps(partGrid, entry.pos(), lamp, protoLamp, probablyBroken);
            }
        });
        CompoundPrototypeNode rootNode = canvas.getRootNode();
        for (Connector connector : Utils.concatArrays(circuitNode.getInputs(), circuitNode.getOutputs()))
        {
            Connection connection = new Connection(connector.type());
            connection.setPos(connector.pos(), connector.port().toPartRotation());
            connection.setPortDir(connector.dir());
            connection.setName(connector.name());
            connection.connect(wires.get(connector.wire()));
            rootNode.setConnection(connector.port(), connection);
        }

        owner.getToolPane().getLibraryBrowser().setExportName(circuitNode.getName());

        if (probablyBroken.isTrue())
        {
            DialogScreen.builder(DialogScreen.Type.INFO)
                    .withTitle(TITLE_IMPORT_BROKEN)
                    .withMessage(MESSAGE_IMPORT_BROKEN)
                    .show();
        }
    }

    private static void resolveReferenceNodeWires(PrototypeNode protoNode, List<Wire> wires, Connector[] connectors, WirePair[] wirePairs, MutableBoolean probablyBroken)
    {
        outer: for (WirePair wirePair : wirePairs)
        {
            for (Connector connector : connectors)
            {
                if (connector.wire() == wirePair.internal())
                {
                    protoNode.setConnection(connector.port(), wires.get(wirePair.external()), true);
                    continue outer;
                }
            }
            probablyBroken.setTrue();
        }
    }

    private static void importChainedLamps(PartGrid partGrid, NodePos rootPos, LampCircuitNode lamp, LampPrototypeNode protoLamp, MutableBoolean probablyBroken)
    {
        List<LampCircuitNode.ChainEntry> chain = lamp.getChainedNodes();

        // Compute a set of candidate chain targets to avoid endlessly looping if a chain entry happens to be invalid
        Set<NodePos> lampPosSet = new HashSet<>();
        lampPosSet.add(rootPos);
        for (LampCircuitNode.ChainEntry chainEntry : chain)
        {
            lampPosSet.add(chainEntry.pos());
        }

        Deque<LampCircuitNode.ChainEntry> chainEntries = new ArrayDeque<>(chain);
        Map<NodePos, LampPrototypeNode> lampProtos = new HashMap<>(chainEntries.size());
        lampProtos.put(rootPos, protoLamp);
        while (!chainEntries.isEmpty())
        {
            LampCircuitNode.ChainEntry chainEntry = chainEntries.removeFirst();
            NodePos pos = chainEntry.pos();
            int rotation = chainEntry.rotation();

            NodePos targetPos = pos.offset(Port.ofPartRotation(rotation));
            LampPrototypeNode chainTarget = lampProtos.get(targetPos);
            if (chainTarget == null && lampPosSet.contains(targetPos))
            {
                chainEntries.addLast(chainEntry);
                continue;
            }

            LampPrototypeNode lampProto = new LampPrototypeNode();
            lampProto.setColor(lamp.getColor());
            if (chainTarget != null)
            {
                lampProto.chain(chainTarget);
            }
            else
            {
                probablyBroken.setTrue();
            }
            partGrid.setPartNode(pos, lampProto, rotation, PartSetMode.ADD);
            lampProtos.put(lampProto.getPos(), lampProto);
        }
    }

    public static void displayImportError(ImportError error)
    {
        DialogScreen.builder(DialogScreen.Type.ERROR)
                .withTitle(TITLE_IMPORT_ERROR)
                .withMessage(error.dialogMessage)
                .show();
    }

    public void assembleAndExport(String name, ExportTarget target)
    {
        ProblemReporter.Collector reporter = new ProblemReporter.Collector();
        CompoundCircuitNode assembled = CircuitAssembler.assemble(name, canvas.getRootNode(), reporter);
        if (assembled == null)
        {
            // TODO: unpack reporter and set up error annotations, replacing temporary error dialog
            DialogScreen.builder(DialogScreen.Type.ERROR)
                    .withTitle(Component.literal("Circuit Assembly Failed"))
                    .withMessage(Component.literal(reporter.getReport()))
                    .show();
            return;
        }

        boolean exists = switch (target)
        {
            case CIRCUIT_ITEM -> StoredCircuit.isPresent(menu.getCircuitSlot().getItem());
            case LIBRARY -> ClientCircuitLibrary.hasEntryWithName(name);
            case JSON_IN_CLIPBOARD -> false;
        };
        if (!exists)
        {
            exportCircuit(assembled, target);
            return;
        }

        DialogScreen.builder(DialogScreen.Type.CONFIRM)
                .withTitle(TITLE_CONFIRM_EXPORT)
                .withMessage(MESSAGE_CONFIRM_EXPORT)
                .withOkCallback(() -> exportCircuit(assembled, target))
                .show();
    }

    private void exportCircuit(CompoundCircuitNode circuitNode, ExportTarget target)
    {
        switch (target)
        {
            case LIBRARY ->
            {
                ClientCircuitLibrary.addOrModifyCircuit(circuitNode);
                waitingForExportResult = true;
            }
            case CIRCUIT_ITEM ->
            {
                var payload = new ServerboundWorkbenchWriteCircuitPayload(menu.containerId, circuitNode);
                ClientPacketDistributor.sendToServer(payload);
                waitingForExportResult = true;
            }
            case JSON_IN_CLIPBOARD ->
            {
                DataResult<JsonElement> result;
                try
                {
                    result = NODE_CODEC.encodeStart(JsonOps.INSTANCE, circuitNode);
                }
                catch (Throwable t)
                {
                    displayExportResult(ExportResult.JSON_ENCODE_FAILED);
                    return;
                }
                if (result.isError() || result.result().isEmpty())
                {
                    displayExportResult(ExportResult.JSON_ENCODE_FAILED);
                    return;
                }

                String string = GsonHelper.toStableString(result.getOrThrow());
                Minecraft.getInstance().keyboardHandler.setClipboard(string);
                displayExportResult(ExportResult.SUCCESS);
            }
        }
    }

    public static void displayExportResult(ExportResult result)
    {
        boolean success = result.isSuccess();
        DialogScreen.builder(success ? DialogScreen.Type.INFO : DialogScreen.Type.ERROR)
                .withTitle(success ? TITLE_EXPORT_SUCCESS : TITLE_EXPORT_ERROR)
                .withMessage(result.dialogMessage)
                .show();
        // TODO: mark as unmodified on success
    }

    public boolean isWaitingForExportResult()
    {
        boolean waiting = waitingForExportResult;
        waitingForExportResult = false;
        return waiting;
    }

    public enum ImportError
    {
        PARSE_FAILED,
        DECODE_FAILED,
        VALIDATE_FAILED,
        ;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component dialogMessage = Utils.translate("msg", "circuit_workbench.library_browser.import_action.failed." + name);

        public Component getDialogMessage()
        {
            return dialogMessage;
        }
    }

    public enum ExportResult
    {
        SUCCESS,
        ITEM_SERVER_ERROR,
        LIBRARY_SERVER_ERROR,
        JSON_ENCODE_FAILED,
        ;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final Component dialogMessage = Utils.translate("msg", "circuit_workbench.library_browser.export_action.result." + name);

        boolean isSuccess()
        {
            return this == SUCCESS;
        }

        public Component getDialogMessage()
        {
            return dialogMessage;
        }

        public static ExportResult ofServerResponse(boolean success, ExportTarget target)
        {
            if (success) return SUCCESS;
            return switch (target)
            {
                case CIRCUIT_ITEM -> ITEM_SERVER_ERROR;
                case LIBRARY -> LIBRARY_SERVER_ERROR;
                case JSON_IN_CLIPBOARD -> throw new UnsupportedOperationException();
            };
        }
    }
}
