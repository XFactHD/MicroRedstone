package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import net.minecraft.world.item.DyeColor;

// TODO: consider persisting the workbench config
public final class WorkbenchConfig
{
    public static final WorkbenchConfig INSTANCE = new WorkbenchConfig();

    private DyeColor wireColor = WireType.SINGLE.getDefaultColor();
    private boolean routeLongWireSectionFirst = true;

    public DyeColor getWireColor()
    {
        return wireColor;
    }

    public boolean isRouteLongWireSectionFirst()
    {
        return routeLongWireSectionFirst;
    }

    public void setWireColor(DyeColor wireColor)
    {
        this.wireColor = wireColor;
    }

    public void toggleRouteLongWireSectionFirst()
    {
        routeLongWireSectionFirst = !routeLongWireSectionFirst;
    }
}
