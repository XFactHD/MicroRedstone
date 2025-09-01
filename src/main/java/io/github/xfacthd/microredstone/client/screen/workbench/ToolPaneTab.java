package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum ToolPaneTab
{
    PARTS,
    TOOLS,
    LIBRARY,
    ;

    private static final ToolPaneTab[] TABS = values();
    public static final int TAB_COUNT = TABS.length;
    public static final int MAX_TAB_IDX = TABS.length - 1;

    private final Component title = Utils.translate("label", "circuit_workbench.tab." + toString().toLowerCase(Locale.ROOT));

    public Component getTitle()
    {
        return title;
    }
}
