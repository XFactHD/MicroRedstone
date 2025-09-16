package io.github.xfacthd.microredstone.client.screen.workbench.menu;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.dialog.QueryWidget;
import io.github.xfacthd.microredstone.client.screen.widgets.ValidatingEditBox;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.ContextMenuBuilder;
import io.github.xfacthd.microredstone.client.screen.widgets.menu.SubMenuKey;
import io.github.xfacthd.microredstone.client.screen.workbench.widgets.CircuitCanvas;
import io.github.xfacthd.microredstone.common.circuit.assembler.CircuitValidator;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.PortDir;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public final class ConnectionNodeContextMenuProvider extends BaseNodeContextMenuProvider<Connection>
{
    public static final Component ENTRY_PORT_DIR = Utils.translate("label", "circuit_workbench.canvas.menu.node.connection.port_dir");
    public static final Component ENTRY_NAME = Utils.translate("label", "circuit_workbench.canvas.menu.node.connection.name");
    public static final Component TITLE_SET_NAME = Utils.translate("title", "circuit_workbench.canvas.node.connection.set_name");
    public static final Component LABEL_SET_NAME = Utils.translate("label", "circuit_workbench.canvas.node.connection.set_name.query");
    private static final SubMenuKey DIR_SUB_MENU = new SubMenuKey("port_direction");
    private static final PortDir[] PORT_DIRS = PortDir.values();

    public ConnectionNodeContextMenuProvider(CircuitCanvas canvas, Connection node)
    {
        super(canvas, node);
    }

    @Override
    public void fillRootMenu(ContextMenuBuilder menuBuilder)
    {
        menuBuilder.addSubMenuEntry(ENTRY_PORT_DIR, DIR_SUB_MENU);
        menuBuilder.addActionEntry(ENTRY_NAME, this::openSetNameDialog);
        super.fillRootMenu(menuBuilder);
    }

    @Override
    public void fillSubMenu(ContextMenuBuilder menuBuilder, SubMenuKey subMenuKey)
    {
        if (subMenuKey == DIR_SUB_MENU)
        {
            for (PortDir dir : PORT_DIRS)
            {
                menuBuilder.addActionEntry(dir.getTitle(), entry -> entry
                        .withAction(() -> node.setPortDir(dir))
                        .withStateSupplier(() -> node.getPortDir() == dir)
                );
            }
        }
    }

    private void openSetNameDialog()
    {
        DialogScreen.builder(DialogScreen.Type.QUERY)
                .withTitle(TITLE_SET_NAME)
                .withQueryWidget(new NameQueryWidget(node))
                .show();
    }

    private static final class NameQueryWidget implements QueryWidget
    {
        private static final int EDIT_BOX_WIDTH = 100;
        private static final int EDIT_BOX_HEIGHT = 20;
        private static final ValidatingEditBox.Validator VALIDATOR = value ->
                value.length() <= CircuitValidator.MAX_CON_NAME_LEN ? TriState.TRUE : TriState.DEFAULT;

        private final Connection node;
        @UnknownNullability
        private ValidatingEditBox editBox;

        public NameQueryWidget(Connection node)
        {
            this.node = node;
        }

        @Override
        public Component label()
        {
            return LABEL_SET_NAME;
        }

        @Override
        public void setupWidget(Font font, int x, int y, int maxWidth, Consumer<AbstractWidget> widgetAdder)
        {
            int width = Math.min(EDIT_BOX_WIDTH, maxWidth);
            editBox = new ValidatingEditBox(font, x, y, width, EDIT_BOX_HEIGHT, VALIDATOR, editBox, node.getName());
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
            node.setName(editBox.getTrimmedValue());
        }
    }
}
