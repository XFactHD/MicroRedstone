package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.client.screen.workbench.tab.LogicGateList;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

public sealed interface DragStart {
    static DragStart canvas(NodePos pos) {
        return new Canvas(pos);
    }

    static DragStart partsList(int partIdx) {
        return new PartsListEntry(partIdx);
    }

    static DragStart library(int entryIdx) {
        return new LibraryBrowserEntry(entryIdx);
    }

    Optional<NodePos> canvas();

    OptionalInt partsList();

    OptionalInt library();

    @Nullable PlaceableNode resolve(CircuitWorkbenchScreen workbenchScreen);

    record Canvas(NodePos pos) implements DragStart {
        @Override
        public Optional<NodePos> canvas() {
            return Optional.of(pos);
        }

        @Override
        public OptionalInt partsList() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt library() {
            return OptionalInt.empty();
        }

        @Override
        public @Nullable PlaceableNode resolve(CircuitWorkbenchScreen workbenchScreen) {
            return workbenchScreen.getCanvas().getPartGrid().getPartNode(pos);
        }
    }

    record PartsListEntry(int partIdx) implements DragStart {
        @Override
        public Optional<NodePos> canvas() {
            return Optional.empty();
        }

        @Override
        public OptionalInt partsList() {
            return OptionalInt.of(partIdx);
        }

        @Override
        public OptionalInt library() {
            return OptionalInt.empty();
        }

        @Override
        public PlaceableNode resolve(CircuitWorkbenchScreen workbenchScreen) {
            return LogicGateList.instantiate(partIdx);
        }
    }

    record LibraryBrowserEntry(int entryIdx) implements DragStart {
        @Override
        public Optional<NodePos> canvas() {
            return Optional.empty();
        }

        @Override
        public OptionalInt partsList() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt library() {
            return OptionalInt.of(entryIdx);
        }

        @Override
        public @Nullable PlaceableNode resolve(CircuitWorkbenchScreen workbenchScreen) {
            return workbenchScreen.getToolPane().getPartsList().instantiateLibraryNode(entryIdx);
        }
    }
}
