package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.dialog.QueryWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public final class ConverterPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ConverterPrototypeNode>
{
    public static final Component ENTRY_SET_BIT = Utils.translate("label", "circuit_workbench.canvas.menu.node.converter.set_bit");
    public static final Component TITLE_SET_BIT = Utils.translate("title", "circuit_workbench.canvas.node.converter.set_bit");
    public static final Component LABEL_SET_BIT = Utils.translate("label", "circuit_workbench.canvas.node.converter.set_bit.query");

    public ConverterPartNodeContextMenuProvider(CircuitCanvas canvas, ConverterPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_SET_BIT, this::openBitConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openBitConfigDialog()
    {
        DialogScreen.builder(DialogScreen.Type.QUERY)
                .withTitle(TITLE_SET_BIT)
                .withQueryWidget(new ConverterQueryWidget(node))
                .show();
    }

    private static final class ConverterQueryWidget implements QueryWidget
    {
        private static final int EDIT_BOX_WIDTH = 60;
        private static final int EDIT_BOX_HEIGHT = 20;

        private final ConverterPrototypeNode node;
        // TODO: replace with proper NumberEditBox based on improved EditBox needed for export name edit
        @UnknownNullability
        private EditBox editBox = null;

        private ConverterQueryWidget(ConverterPrototypeNode node)
        {
            this.node = node;
        }

        @Override
        public Component label()
        {
            return LABEL_SET_BIT;
        }

        @Override
        public void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder)
        {
            EditBox prevEditBox = editBox;
            editBox = new EditBox(font, x, y, Math.min(EDIT_BOX_WIDTH, maxWidth), EDIT_BOX_HEIGHT, Component.empty());
            editBox.setFilter(text -> isInputValid(text, false));
            editBox.setValue(prevEditBox != null ? prevEditBox.getValue() : Integer.toString(node.getBitIndex()));
            widgetAdder.accept(editBox);
        }

        @Override
        public boolean isInputValid()
        {
            return isInputValid(editBox.getValue(), true);
        }

        private static boolean isInputValid(String text, boolean strict)
        {
            if (text.isBlank()) return !strict;
            try
            {
                int value = Integer.parseInt(text);
                return value >= 0 && value < 16;
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }

        @Override
        public void saveQueryResult()
        {
            node.setBitIndex(Integer.parseInt(editBox.getValue()));
        }
    }
}
