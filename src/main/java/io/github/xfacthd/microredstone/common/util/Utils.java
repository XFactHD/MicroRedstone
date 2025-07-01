package io.github.xfacthd.microredstone.common.util;

import io.github.xfacthd.microredstone.MicroRedstone;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Objects;
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
    private static final Rotation[][] DIR_PAIR_TO_ROT = makeDirPairToRotMap();

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

    public static Rotation getRotationFromFacingOrientation(Direction facing, Direction orientation)
    {
        return Objects.requireNonNull(DIR_PAIR_TO_ROT[facing.ordinal()][orientation.ordinal()]);
    }

    private static Rotation[][] makeDirPairToRotMap()
    {
        Rotation[][] arr = new Rotation[6][6];

        arr[Direction.DOWN.ordinal()][Direction.NORTH.ordinal()] = Rotation.NONE;
        arr[Direction.DOWN.ordinal()][Direction.SOUTH.ordinal()] = Rotation.CLOCKWISE_180;
        arr[Direction.DOWN.ordinal()][Direction.WEST.ordinal()] = Rotation.CLOCKWISE_90;
        arr[Direction.DOWN.ordinal()][Direction.EAST.ordinal()] = Rotation.COUNTERCLOCKWISE_90;

        arr[Direction.UP.ordinal()][Direction.NORTH.ordinal()] = Rotation.CLOCKWISE_90;
        arr[Direction.UP.ordinal()][Direction.SOUTH.ordinal()] = Rotation.COUNTERCLOCKWISE_90;
        arr[Direction.UP.ordinal()][Direction.WEST.ordinal()] = Rotation.NONE;
        arr[Direction.UP.ordinal()][Direction.EAST.ordinal()] = Rotation.CLOCKWISE_180;

        arr[Direction.NORTH.ordinal()][Direction.DOWN.ordinal()] = Rotation.NONE;
        arr[Direction.NORTH.ordinal()][Direction.UP.ordinal()] = Rotation.CLOCKWISE_180;
        arr[Direction.NORTH.ordinal()][Direction.WEST.ordinal()] = Rotation.COUNTERCLOCKWISE_90;
        arr[Direction.NORTH.ordinal()][Direction.EAST.ordinal()] = Rotation.CLOCKWISE_90;

        arr[Direction.SOUTH.ordinal()][Direction.DOWN.ordinal()] = Rotation.NONE;
        arr[Direction.SOUTH.ordinal()][Direction.UP.ordinal()] = Rotation.CLOCKWISE_180;
        arr[Direction.SOUTH.ordinal()][Direction.WEST.ordinal()] = Rotation.CLOCKWISE_90;
        arr[Direction.SOUTH.ordinal()][Direction.EAST.ordinal()] = Rotation.COUNTERCLOCKWISE_90;

        arr[Direction.WEST.ordinal()][Direction.DOWN.ordinal()] = Rotation.NONE;
        arr[Direction.WEST.ordinal()][Direction.UP.ordinal()] = Rotation.CLOCKWISE_180;
        arr[Direction.WEST.ordinal()][Direction.NORTH.ordinal()] = Rotation.CLOCKWISE_90;
        arr[Direction.WEST.ordinal()][Direction.SOUTH.ordinal()] = Rotation.COUNTERCLOCKWISE_90;

        arr[Direction.EAST.ordinal()][Direction.DOWN.ordinal()] = Rotation.NONE;
        arr[Direction.EAST.ordinal()][Direction.UP.ordinal()] = Rotation.CLOCKWISE_180;
        arr[Direction.EAST.ordinal()][Direction.NORTH.ordinal()] = Rotation.COUNTERCLOCKWISE_90;
        arr[Direction.EAST.ordinal()][Direction.SOUTH.ordinal()] = Rotation.CLOCKWISE_90;

        return arr;
    }

    private Utils() { }
}
