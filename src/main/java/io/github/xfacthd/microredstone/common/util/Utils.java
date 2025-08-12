package io.github.xfacthd.microredstone.common.util;

import io.github.xfacthd.microredstone.MicroRedstone;
import io.github.xfacthd.microredstone.common.util.registration.DeferredBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.function.IOFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public final class Utils
{
    private static final ResourceLocation RL_TEMPLATE = ResourceLocation.fromNamespaceAndPath(MicroRedstone.MOD_ID, "");
    private static final Long2ObjectMap<Direction> DIRECTION_BY_NORMAL = Arrays.stream(Direction.values())
            .collect(Collectors.toMap(
                    side -> new BlockPos(side.getUnitVec3i()).asLong(),
                    Function.identity(),
                    (sideA, sideB) -> { throw new IllegalArgumentException("Duplicate keys"); },
                    Long2ObjectOpenHashMap::new
            ));

    public static Direction getDirection(BlockPos srcPos, BlockPos destPos)
    {
        return dirByNormal(destPos.getX() - srcPos.getX(), destPos.getY() - srcPos.getY(), destPos.getZ() - srcPos.getZ());
    }

    public static Direction dirByNormal(int x, int y, int z)
    {
        return DIRECTION_BY_NORMAL.get(BlockPos.asLong(x, y, z));
    }

    public static ResourceLocation rl(String path)
    {
        return RL_TEMPLATE.withPath(path);
    }

    public static ResourceLocation rl(String namespace, String path)
    {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path)
    {
        return new CustomPacketPayload.Type<>(Utils.rl(path));
    }

    public static <T> ResourceKey<T> getKeyOrThrow(Holder<T> holder)
    {
        return holder.unwrapKey().orElseThrow(
                () -> new IllegalArgumentException("Direct holders and unbound reference holders are not supported")
        );
    }

    public static MutableComponent translate(@Nullable String prefix, @Nullable String postfix, Object... arguments)
    {
        return Component.translatable(translationKey(prefix, postfix), arguments);
    }

    public static MutableComponent translate(@Nullable String prefix, @Nullable String postfix)
    {
        return Component.translatable(translationKey(prefix, postfix));
    }

    public static String translationKey(@Nullable String prefix, @Nullable String postfix)
    {
        String key = "";
        if (prefix != null)
        {
            key = prefix + ".";
        }
        key += MicroRedstone.MOD_ID;
        if (postfix != null)
        {
            key += "." + postfix;
        }
        return key;
    }

    public static Direction getDirFromCross(Vec3 hitVec, Direction hitFace)
    {
        hitVec = fraction(hitVec).subtract(.5, .5, .5);

        return switch (hitFace.getAxis())
        {
            case X -> Direction.getApproximateNearest(0, hitVec.y, hitVec.z);
            case Y -> Direction.getApproximateNearest(hitVec.x, 0, hitVec.z);
            case Z -> Direction.getApproximateNearest(hitVec.x, hitVec.y, 0);
        };
    }

    public static Vec3 fraction(Vec3 vec)
    {
        return new Vec3(
                vec.x() - Math.floor(vec.x()),
                vec.y() - Math.floor(vec.y()),
                vec.z() - Math.floor(vec.z())
        );
    }

    public static <T> T[] fillArray(T[] arr, IntFunction<T> initializer)
    {
        for (int i = 0; i < arr.length; i++)
        {
            arr[i] = initializer.apply(i);
        }
        return arr;
    }

    public static <T> T[] concatArrays(T[] arrOne, T[] arrTwo)
    {
        T[] arrNew = Arrays.copyOf(arrOne, arrOne.length + arrTwo.length);
        System.arraycopy(arrTwo, 0, arrNew, arrOne.length, arrTwo.length);
        return arrNew;
    }

    public static Rotation getRotationFromFacingOrientation(Direction facing, Direction orientation)
    {
        int idx = MappingArrays.dirPairToRotIndex(facing, orientation);
        return Objects.requireNonNull(MappingArrays.DIR_PAIR_TO_ROT[idx]);
    }

    public static Direction getSideFromFacingRotation(Direction facing, Rotation rotation)
    {
        int idx = MappingArrays.dirRotToSideIndex(facing, rotation);
        return MappingArrays.DIR_ROT_TO_SIDE[idx];
    }

    public static Rotation invertRotation(Rotation rotation)
    {
        return switch (rotation)
        {
            case NONE -> Rotation.NONE;
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
        };
    }

    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity>BlockEntityTicker<A> createBlockEntityTicker(
            BlockEntityType<A> actualType, DeferredBlockEntity<E> expectedType, Consumer<? super E> instTicker
    )
    {
        BlockEntityTicker<? super E> ticker = (level, pos, state, be) -> instTicker.accept(be);
        return BaseEntityBlock.createTickerHelper(actualType, expectedType.value(), ticker);
    }

    public static <T, R> Function<T, R> uncheckIO(IOFunction<T, R> function)
    {
        return function.asFunction();
    }

    private Utils() { }
}
