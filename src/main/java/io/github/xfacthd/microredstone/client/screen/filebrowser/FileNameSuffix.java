package io.github.xfacthd.microredstone.client.screen.filebrowser;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

record FileNameSuffix(String suffix, boolean autoSuffix, boolean filterBySuffix, Component label)
{
    static final FileNameSuffix EMPTY = new FileNameSuffix("", false, false, FileBrowserScreen.VALUE_FILE_TYPE_ALL);

    static FileNameSuffix of(@Nullable String suffix, boolean autoSuffix, boolean filterBySuffix)
    {
        return suffix == null ? EMPTY : new FileNameSuffix(suffix, autoSuffix, filterBySuffix, Component.literal("*" + suffix));
    }

    String applySuffix(String fileName)
    {
        if (autoSuffix && !fileName.endsWith(suffix))
        {
            return fileName + suffix;
        }
        return fileName;
    }

    boolean filter(String fileName)
    {
        return !filterBySuffix || fileName.endsWith(suffix);
    }
}
