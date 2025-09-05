package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import net.minecraft.network.chat.Component;

public interface MultiModeTab<M extends MultiModeTab.Mode>
{
    M getMode();

    void setMode(M mode);

    interface Mode
    {
        Component getTitle();
    }
}
