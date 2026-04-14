package io.github.xfacthd.microredstone.common.circuit.node;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.EnumSet;
import java.util.Set;

public record NodePos(int x, int y) implements Comparable<NodePos> {
    public static final Codec<NodePos> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("x").forGetter(NodePos::x),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("y").forGetter(NodePos::y)
    ).apply(inst, NodePos::new));
    public static final StreamCodec<ByteBuf, NodePos> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            NodePos::x,
            ByteBufCodecs.VAR_INT,
            NodePos::y,
            NodePos::new
    );

    public NodePos offset(Port port) {
        return switch (port) {
            case UP -> new NodePos(x, y - 1);
            case RIGHT -> new NodePos(x + 1, y);
            case DOWN -> new NodePos(x, y + 1);
            case LEFT -> new NodePos(x - 1, y);
        };
    }

    public boolean isValid(int maxX, int maxY) {
        return x >= 0 && y >= 0 && x < maxX && y < maxY;
    }

    public NodePos subtract(NodePos pos) {
        return new NodePos(x - pos.x, y - pos.y);
    }

    public Set<Port> getDirsTowards(NodePos pos) {
        Set<Port> dirs = EnumSet.noneOf(Port.class);
        if (pos.x < x) {
            dirs.add(Port.LEFT);
        } else if (pos.x > x) {
            dirs.add(Port.RIGHT);
        }
        if (pos.y < y) {
            dirs.add(Port.UP);
        } else if (pos.y > y) {
            dirs.add(Port.DOWN);
        }
        return dirs;
    }

    public Port getDirTowards(NodePos pos) {
        if (x == pos.x) {
            if (pos.y > y) {
                return Port.DOWN;
            } else if (pos.y < y) {
                return Port.UP;
            }
        } else if (y == pos.y) {
            if (pos.x > x) {
                return Port.RIGHT;
            } else /*if (pos.x < x)*/ {
                return Port.LEFT;
            }
        }
        throw new IllegalArgumentException("Same position or diagonal offset");
    }

    @Override
    public int compareTo(NodePos other) {
        int compX = Integer.compare(y, other.y);
        return compX != 0 ? compX : Integer.compare(x, other.x);
    }
}
