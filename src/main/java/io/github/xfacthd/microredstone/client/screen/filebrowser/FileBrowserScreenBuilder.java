package io.github.xfacthd.microredstone.client.screen.filebrowser;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class FileBrowserScreenBuilder
{
    private final FileBrowserScreen.Type type;
    @Nullable
    private Component typeDesc;
    @Nullable
    private Path rootPath;
    @Nullable
    private Path initialPath;
    @Nullable
    private String fileNameSuffix;
    private boolean autoSuffix;
    private boolean filterBySuffix;
    @Nullable
    private Consumer<@Nullable Path> pathConsumer;

    FileBrowserScreenBuilder(FileBrowserScreen.Type type)
    {
        this.type = type;
    }

    public FileBrowserScreenBuilder typeDescription(Component typeDesc)
    {
        this.typeDesc = typeDesc;
        return this;
    }

    public FileBrowserScreenBuilder rootPath(Path rootPath)
    {
        if (!Files.isDirectory(rootPath, LinkOption.NOFOLLOW_LINKS))
        {
            throw new IllegalArgumentException("Root path must be a directory");
        }
        this.rootPath = rootPath;
        return this;
    }

    public FileBrowserScreenBuilder initialPath(Path initialPath)
    {
        if (!Files.isDirectory(initialPath, LinkOption.NOFOLLOW_LINKS))
        {
            throw new IllegalArgumentException("Initial path must be a directory");
        }
        this.initialPath = initialPath;
        return this;
    }

    public FileBrowserScreenBuilder fileNameSuffix(String fileNameSuffix, boolean autoSuffix, boolean filterBySuffix)
    {
        if (fileNameSuffix.isBlank())
        {
            throw new IllegalArgumentException("Suffix cannot be empty");
        }
        if (!autoSuffix && !filterBySuffix)
        {
            throw new IllegalArgumentException("File name suffix must be used for either auto-suffix or suffix filtering");
        }
        if (autoSuffix && type != FileBrowserScreen.Type.SAVE)
        {
            throw new IllegalArgumentException("Auto-suffix is only supported for saving");
        }
        this.fileNameSuffix = fileNameSuffix;
        this.autoSuffix = autoSuffix;
        this.filterBySuffix = filterBySuffix;
        return this;
    }

    public FileBrowserScreenBuilder pathConsumer(Consumer<@Nullable Path> pathConsumer)
    {
        this.pathConsumer = pathConsumer;
        return this;
    }

    public FileBrowserScreen build()
    {
        Objects.requireNonNull(typeDesc, "No type description specified");
        Objects.requireNonNull(rootPath, "No root path specified");
        Objects.requireNonNull(pathConsumer, "No path consumer specified");
        FileNameSuffix suffix = FileNameSuffix.of(fileNameSuffix, autoSuffix, filterBySuffix);
        return new FileBrowserScreen(type, typeDesc, rootPath, initialPath, suffix, pathConsumer);
    }

    public void show()
    {
        Minecraft.getInstance().pushGuiLayer(build());
    }
}
