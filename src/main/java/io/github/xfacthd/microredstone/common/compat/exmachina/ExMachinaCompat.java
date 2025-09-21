package io.github.xfacthd.microredstone.common.compat.exmachina;

import com.mojang.serialization.MapCodec;
import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.compat.CompatHandler;
import net.commoble.exmachina.api.ExMachinaGameEvents;
import net.commoble.exmachina.api.ExMachinaRegistries;
import net.commoble.exmachina.api.SignalComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExMachinaCompat
{
    private static boolean loaded = false;

    public static void init(IEventBus modBus)
    {
        if (ModList.get().isLoaded("exmachina"))
        {
            try
            {
                GuardedAccess.init(modBus);
                loaded = true;
            }
            catch (Throwable t)
            {
                CompatHandler.LOGGER.error("Failed to initialize ExMachina compat", t);
            }
        }
    }

    public static void updateNeighbor(Level level, BlockPos pos)
    {
        if (loaded)
        {
            GuardedAccess.updateNeighbor(level, pos);
        }
    }

    private static final class GuardedAccess
    {
        private static final DeferredRegister<MapCodec<? extends SignalComponent>> COMPONENTS = DeferredRegister.create(
                ExMachinaRegistries.SIGNAL_COMPONENT_TYPE,
                MicroRedstone.MOD_ID
        );
        private static final Holder<MapCodec<? extends SignalComponent>> MICROCHIP_COMPONENT = COMPONENTS.register(
                "microchip", () -> MicrochipSignalComponent.CODEC
        );

        static void init(IEventBus modBus)
        {
            COMPONENTS.register(modBus);
        }

        static void updateNeighbor(Level level, BlockPos pos)
        {
            ExMachinaGameEvents.scheduleSignalGraphUpdate(level, pos);
        }

        private GuardedAccess() {}
    }

    private ExMachinaCompat() { }
}
