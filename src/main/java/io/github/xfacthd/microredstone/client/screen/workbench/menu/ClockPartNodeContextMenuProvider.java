package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.dialog.QueryWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.NumberEditBox;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Set;
import java.util.function.Consumer;

public final class ClockPartNodeContextMenuProvider extends PartNodeContextMenuProvider<ClockPrototypeNode>
{
    public static final Component ENTRY_SET_PERIOD = Utils.translate("label", "circuit_workbench.canvas.menu.node.clock.set_period");
    public static final Component TITLE_SET_PERIOD = Utils.translate("title", "circuit_workbench.canvas.node.clock.set_period");
    public static final Component LABEL_SET_PERIOD = Utils.translate("label", "circuit_workbench.canvas.node.clock.set_period.query");
    public static final Component TOOLTIP_INVALID_PERIOD = Utils.translate("tooltip", "circuit_workbench.canvas.node.clock.set_perdiod.invalid_value");

    public ClockPartNodeContextMenuProvider(CircuitCanvas canvas, ClockPrototypeNode node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addActionEntry(ENTRY_SET_PERIOD, this::openPeriodConfigDialog);
        super.fillRootMenu(menuBuilder);
    }

    private void openPeriodConfigDialog()
    {
        DialogScreen.builder(DialogScreen.Type.QUERY)
                .withTitle(TITLE_SET_PERIOD)
                .withQueryWidget(new ClockQueryWidget(node))
                .show();
    }

    private static final class ClockQueryWidget implements QueryWidget
    {
        private static final int EDIT_BOX_WIDTH = 60;
        private static final int EDIT_BOX_HEIGHT = 20;
        private static final NumberEditBox.ParserConfig PARSER_CONFIG = new NumberEditBox.ParserConfig(
                value -> value >= 2 && value % 2 == 0,
                Set.of(NumberEditBox.NumberFormat.DEC),
                false,
                TOOLTIP_INVALID_PERIOD
        );

        private final ClockPrototypeNode node;
        @UnknownNullability
        private NumberEditBox editBox = null;

        private ClockQueryWidget(ClockPrototypeNode node)
        {
            this.node = node;
        }

        @Override
        public Component label()
        {
            return LABEL_SET_PERIOD;
        }

        @Override
        public void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder)
        {
            int width = Math.min(EDIT_BOX_WIDTH, maxWidth);
            int defVal = node.getHalfPeriodLength() * 2;
            editBox = new NumberEditBox(font, x, y, width, EDIT_BOX_HEIGHT, PARSER_CONFIG, editBox, defVal);
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
            node.setHalfPeriodLength(editBox.getIntValue() / 2);
        }
    }
}
