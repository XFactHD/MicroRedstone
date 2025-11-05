package io.github.xfacthd.microredstone.client.data;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.client.screen.filebrowser.FileBrowserScreen;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.CompoundPrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public final class LocalCircuitStorage
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component FILE_TYPE_DESCRIPTION = Utils.translate("desc", "circuit_workbench.local_storage.file_type");
    private static final LoadResult LOAD_CANCELED = new LoadResult.Canceled();
    private static final StoreResult STORE_SUCCESS = new StoreResult.Success();
    private static final StoreResult STORE_CANCELED = new StoreResult.Canceled();
    private static final Lazy<Path> ROOT_PATH = Lazy.of(() -> FMLPaths.GAMEDIR.get().resolve(MicroRedstone.MOD_ID).resolve("designs"));
    private static final String FILE_NAME_SUFFIX = ".mrc";
    private static final StandardOpenOption[] SAVE_OPTIONS = new StandardOpenOption[] {
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
    };

    public static void load(Consumer<LoadResult> callback)
    {
        if (!ensureDirectoryExists(e -> callback.accept(new LoadResult.Error(e))))
        {
            return;
        }

        FileBrowserScreen.builder(FileBrowserScreen.Type.OPEN)
                .typeDescription(FILE_TYPE_DESCRIPTION)
                .fileNameSuffix(FILE_NAME_SUFFIX, false, true)
                .rootPath(ROOT_PATH.get())
                .pathConsumer(path -> doLoad(callback, path))
                .show();
    }

    private static void doLoad(Consumer<LoadResult> callback, @Nullable Path path)
    {
        if (path == null)
        {
            callback.accept(LOAD_CANCELED);
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path))
        {
            JsonElement json = GsonHelper.parse(reader);
            DataResult<CompoundPrototypeNode> result = CompoundPrototypeNode.deserialize(JsonOps.INSTANCE, json);
            callback.accept(new LoadResult.Success(result.getOrThrow()));
        }
        catch (Throwable t)
        {
            callback.accept(new LoadResult.Error(t));
            LOGGER.error("Failed to load circuit design", t);
        }
    }

    public static void store(CompoundPrototypeNode circuit, Consumer<StoreResult> callback)
    {
        if (!ensureDirectoryExists(e -> callback.accept(new StoreResult.Error(e))))
        {
            return;
        }

        FileBrowserScreen.builder(FileBrowserScreen.Type.SAVE)
                .typeDescription(FILE_TYPE_DESCRIPTION)
                .fileNameSuffix(FILE_NAME_SUFFIX, true, true)
                .rootPath(ROOT_PATH.get())
                .pathConsumer(path -> doStore(circuit, callback, path))
                .show();
    }

    private static void doStore(CompoundPrototypeNode circuit, Consumer<StoreResult> callback, @Nullable Path path)
    {
        if (path == null)
        {
            callback.accept(STORE_CANCELED);
            return;
        }

        try
        {
            Files.createDirectories(path.getParent());

            JsonElement json = circuit.serialize(JsonOps.INSTANCE).getOrThrow();
            String jsonText = GsonHelper.toStableString(json);
            Files.writeString(path, jsonText, SAVE_OPTIONS);

            callback.accept(STORE_SUCCESS);
        }
        catch (Throwable t)
        {
            callback.accept(new StoreResult.Error(t));
            LOGGER.error("Failed to save circuit design", t);
        }
    }

    private static boolean ensureDirectoryExists(Consumer<IOException> errorHandler)
    {
        try
        {
            Files.createDirectories(ROOT_PATH.get());
            return true;
        }
        catch (IOException e)
        {
            errorHandler.accept(e);
            return false;
        }
    }

    public sealed interface LoadResult
    {
        record Success(CompoundPrototypeNode circuit) implements LoadResult { }

        record Canceled() implements LoadResult { }

        record Error(Throwable error) implements LoadResult { }
    }

    public sealed interface StoreResult
    {
        record Success() implements StoreResult { }

        record Canceled() implements StoreResult { }

        record Error(Throwable error) implements StoreResult { }
    }

    private LocalCircuitStorage() {}
}
