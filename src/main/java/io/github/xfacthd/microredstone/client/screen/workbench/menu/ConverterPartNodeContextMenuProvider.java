package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.dialog.QueryWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.NumberEditBox;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Set;
import java.util.function.Consumer;

public final class ConverterPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ConverterPrototypeNode> {
    public static final Component ENTRY_SET_BIT = Utils.translate("label", "circuit_workbench.canvas.menu.node.converter.set_bit");
    public static final Component TITLE_SET_BIT = Utils.translate("title", "circuit_workbench.canvas.node.converter.set_bit");
    public static final Component LABEL_SET_BIT = Utils.translate("label", "circuit_workbench.canvas.node.converter.set_bit.query");
    public static final Component TOOLTIP_INVALID_BIT = Utils.translate("tooltip", "circuit_workbench.canvas.node.converter.set_bit.invalid_value");

    public ConverterPartNodeContextMenuProvider(CircuitCanvas canvas, ConverterPrototypeNode node) {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder) {
        menuBuilder.addActionEntry(ENTRY_SET_BIT, this::openBitConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openBitConfigDialog() {
        DialogScreen.builder(DialogScreen.Type.QUERY)
                .withTitle(TITLE_SET_BIT)
                .withQueryWidget(new ConverterQueryWidget(node))
                .show();
    }

    private static final class ConverterQueryWidget implements QueryWidget {
        private static final int EDIT_BOX_WIDTH = 60;
        private static final int EDIT_BOX_HEIGHT = 20;
        private static final NumberEditBox.ParserConfig PARSER_CONFIG = new NumberEditBox.ParserConfig(
                value -> value >= 0 && value < 16,
                Set.of(NumberEditBox.NumberFormat.DEC),
                false,
                TOOLTIP_INVALID_BIT
        );

        private final ConverterPrototypeNode node;
        @UnknownNullability
        private NumberEditBox editBox = null;

        private ConverterQueryWidget(ConverterPrototypeNode node) {
            this.node = node;
        }

        @Override
        public Component label() {
            return LABEL_SET_BIT;
        }

        @Override
        public void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder) {
            int width = Math.min(EDIT_BOX_WIDTH, maxWidth);
            editBox = new NumberEditBox(font, x, y, width, EDIT_BOX_HEIGHT, PARSER_CONFIG, editBox, node.getBitIndex());
            widgetAdder.accept(editBox);
        }

        @Override
        public boolean isInputValid() {
            return editBox.isInputValid();
        }

        @Override
        public void saveQueryResult() {
            node.setBitIndex(editBox.getIntValue());
        }
    }
}
