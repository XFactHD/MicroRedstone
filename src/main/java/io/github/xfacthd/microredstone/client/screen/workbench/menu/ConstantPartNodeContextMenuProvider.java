package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.dialog.QueryWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.NumberEditBox;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public final class ConstantPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ConstantPrototypeNode>
{
    public static final Component ENTRY_SET_VALUE = Utils.translate("label", "circuit_workbench.canvas.menu.node.constant.set_value");
    public static final Component TITLE_SET_VALUE = Utils.translate("title", "circuit_workbench.canvas.node.constant.set_value");
    public static final Component LABEL_SET_VALUE = Utils.translate("label", "circuit_workbench.canvas.node.constant.set_value.query");
    public static final Component TOOLTIP_INVALID_VALUE_SINGLE = Utils.translate("tooltip", "circuit_workbench.canvas.node.constant.set_value.invalid_value.single");
    public static final Component TOOLTIP_INVALID_VALUE_BUNDLED = Utils.translate("tooltip", "circuit_workbench.canvas.node.constant.set_value.invalid_value.bundled");

    public ConstantPartNodeContextMenuProvider(CircuitCanvas canvas, ConstantPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_SET_VALUE, this::openValueConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openValueConfigDialog()
    {
        DialogScreen.builder(DialogScreen.Type.QUERY)
                .withTitle(TITLE_SET_VALUE)
                .withQueryWidget(new ConstantQueryWidget(node))
                .show();
    }

    private static final class ConstantQueryWidget implements QueryWidget
    {
        private static final int EDIT_BOX_WIDTH = 120;
        private static final int EDIT_BOX_HEIGHT = 20;
        private static final NumberEditBox.ParserConfig PARSER_CONFIG_SINGLE = new NumberEditBox.ParserConfig(
                value -> value == 0 || value == 1,
                Reference2IntMaps.singleton(NumberEditBox.NumberFormat.BIN, 1),
                false,
                TOOLTIP_INVALID_VALUE_SINGLE
        );
        private static final NumberEditBox.ParserConfig PARSER_CONFIG_BUNDLED = new NumberEditBox.ParserConfig(
                value -> value >= 0 && value <= 65_535,
                Util.make(() ->
                {
                    Reference2IntMap<NumberEditBox.NumberFormat> formats = new Reference2IntOpenHashMap<>();
                    formats.put(NumberEditBox.NumberFormat.HEX, 4);
                    formats.put(NumberEditBox.NumberFormat.BIN, 16);
                    return formats;
                }),
                false,
                TOOLTIP_INVALID_VALUE_BUNDLED
        );

        private final ConstantPrototypeNode node;
        @UnknownNullability
        private NumberEditBox editBox = null;

        public ConstantQueryWidget(ConstantPrototypeNode node)
        {
            this.node = node;
        }

        @Override
        public Component label()
        {
            return LABEL_SET_VALUE;
        }

        @Override
        public void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder)
        {
            int width = Math.min(EDIT_BOX_WIDTH, maxWidth);
            NumberEditBox.ParserConfig parserConfig = node.getWireType().select(PARSER_CONFIG_SINGLE, PARSER_CONFIG_BUNDLED);
            editBox = new NumberEditBox(font, x, y, width, EDIT_BOX_HEIGHT, parserConfig, editBox, node.getValue());
            widgetAdder.accept(editBox);
        }

        @Override
        public boolean isInputValid()
        {
            return editBox.isInputValid();
        }

        @Override
        public void saveQueryResult()
        {
            node.setValue((short) editBox.getIntValue());
        }
    }
}
