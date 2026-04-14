package io.github.xfacthd.microredstone.common.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

final class MappingArrays {
    static final Rotation[] DIR_PAIR_TO_ROT = MappingArrays.makeDirPairToRotMap();
    static final Direction[] DIR_ROT_TO_SIDE = MappingArrays.makeDirRotToSideMap();

    private static Rotation[] makeDirPairToRotMap() {
        Rotation[] arr = new Rotation[6 * 8];

        arr[dirPairToRotIndex(Direction.DOWN, Direction.NORTH)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.DOWN, Direction.SOUTH)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.DOWN, Direction.WEST)] = Rotation.COUNTERCLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.DOWN, Direction.EAST)] = Rotation.CLOCKWISE_90;

        arr[dirPairToRotIndex(Direction.UP, Direction.NORTH)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.UP, Direction.SOUTH)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.UP, Direction.WEST)] = Rotation.COUNTERCLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.UP, Direction.EAST)] = Rotation.CLOCKWISE_90;

        arr[dirPairToRotIndex(Direction.NORTH, Direction.DOWN)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.NORTH, Direction.UP)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.NORTH, Direction.WEST)] = Rotation.COUNTERCLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.NORTH, Direction.EAST)] = Rotation.CLOCKWISE_90;

        arr[dirPairToRotIndex(Direction.SOUTH, Direction.DOWN)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.SOUTH, Direction.UP)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.SOUTH, Direction.WEST)] = Rotation.CLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.SOUTH, Direction.EAST)] = Rotation.COUNTERCLOCKWISE_90;

        arr[dirPairToRotIndex(Direction.WEST, Direction.DOWN)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.WEST, Direction.UP)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.WEST, Direction.NORTH)] = Rotation.CLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.WEST, Direction.SOUTH)] = Rotation.COUNTERCLOCKWISE_90;

        arr[dirPairToRotIndex(Direction.EAST, Direction.DOWN)] = Rotation.CLOCKWISE_180;
        arr[dirPairToRotIndex(Direction.EAST, Direction.UP)] = Rotation.NONE;
        arr[dirPairToRotIndex(Direction.EAST, Direction.NORTH)] = Rotation.COUNTERCLOCKWISE_90;
        arr[dirPairToRotIndex(Direction.EAST, Direction.SOUTH)] = Rotation.CLOCKWISE_90;

        return arr;
    }

    static int dirPairToRotIndex(Direction facing, Direction orientation) {
        return (facing.ordinal() << 3) | orientation.ordinal();
    }

    private static Direction[] makeDirRotToSideMap() {
        Direction[] arr = new Direction[6 * 4];

        for (int i = 0; i < DIR_PAIR_TO_ROT.length; i++) {
            Rotation rotation = DIR_PAIR_TO_ROT[i];
            if (rotation == null) {
                continue;
            }

            Direction facing = Direction.from3DDataValue((i >>> 3) & 0b111);
            Direction orientation = Direction.from3DDataValue(i & 0b111);
            arr[dirRotToSideIndex(facing, rotation)] = orientation;
        }

        return arr;
    }

    static int dirRotToSideIndex(Direction facing, Rotation rotation) {
        return (facing.ordinal() << 2) | rotation.ordinal();
    }

    private MappingArrays() { }
}
