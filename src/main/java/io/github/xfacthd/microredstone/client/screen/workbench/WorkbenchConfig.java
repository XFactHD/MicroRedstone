package io.github.xfacthd.microredstone.client.screen.workbench;

import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.data.library.ShareType;
import net.minecraft.world.item.DyeColor;

import java.util.EnumSet;
import java.util.Set;

// TODO: consider persisting the workbench config
public final class WorkbenchConfig
{
    public static final WorkbenchConfig INSTANCE = new WorkbenchConfig();

    private final Set<ShareType> importFilters = EnumSet.allOf(ShareType.class);
    private DyeColor wireColor = WireType.SINGLE.getDefaultColor();
    private boolean routeLongWireSectionFirst = true;

    public boolean isImportFilterEnabled(ShareType filter)
    {
        return importFilters.contains(filter);
    }

    public DyeColor getWireColor()
    {
        return wireColor;
    }

    public boolean isRouteLongWireSectionFirst()
    {
        return routeLongWireSectionFirst;
    }

    public boolean setFilter(ShareType filter, boolean exclusive)
    {
        if (exclusive)
        {
            if (!importFilters.contains(filter) || importFilters.size() > 1)
            {
                importFilters.clear();
                importFilters.add(filter);
                return true;
            }
            return false;
        }
        if (importFilters.contains(filter))
        {
            importFilters.remove(filter);
        }
        else
        {
            importFilters.add(filter);
        }
        return true;
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
