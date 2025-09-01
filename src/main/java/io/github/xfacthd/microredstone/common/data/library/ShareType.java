package io.github.xfacthd.microredstone.common.data.library;

import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public enum ShareType
{
    PRIVATE,
    SHARED,
    PUBLIC,
    BUILTIN;

    private final String name = toString().toLowerCase(Locale.ROOT);
    private final Component title = Utils.translate("label", "circuit_workbench.library_browser.filter." + name);
    private final ResourceLocation icon = Utils.rl("filter/type_" + name);

    public Component getTitle()
    {
        return title;
    }

    public ResourceLocation getIcon()
    {
        return icon;
    }
}
