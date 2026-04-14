package io.github.xfacthd.microredstone.common.compat.exmachina;

import com.mojang.serialization.MapCodec;
import io.github.xfacthd.microredstone.common.blockentity.MicrochipBlockEntity;
import io.github.xfacthd.microredstone.common.redstone.RedstoneType;
import net.commoble.exmachina.api.Channel;
import net.commoble.exmachina.api.NodeShape;
import net.commoble.exmachina.api.SignalComponent;
import net.commoble.exmachina.api.SignalGraphKey;
import net.commoble.exmachina.api.SignalStrength;
import net.commoble.exmachina.api.TransmissionNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

final class MicrochipSignalComponent implements SignalComponent {
    static final MicrochipSignalComponent INSTANCE = new MicrochipSignalComponent();
    static final MapCodec<MicrochipSignalComponent> CODEC = MapCodec.unit(INSTANCE);
    private static final Direction[] DIRECTIONS = Direction.values();

    @Override
    public Collection<TransmissionNode> getTransmissionNodes(ResourceKey<Level> levelKey, BlockGetter level, BlockPos pos, BlockState state, Channel channel) {
        if (level.getBlockEntity(pos) instanceof MicrochipBlockEntity be) {
            Direction dir = state.getValue(BlockStateProperties.FACING);
            List<TransmissionNode> nodes = new ArrayList<>(4);
            for (Direction side : DIRECTIONS) {
                if (side.getAxis() == dir.getAxis()) {
                    continue;
                }

                RedstoneType type = be.getRedstoneType(side);
                if (type == RedstoneType.NONE) {
                    continue;
                }

                if (channel != Channel.redstone() || type != RedstoneType.BUNDLED) {
                    nodes.add(new TransmissionNode(
                            NodeShape.ofSideSide(dir, side),
                            reader(be, side, channel),
                            Set.of(),
                            Set.of(new SignalGraphKey(
                                    levelKey,
                                    pos.relative(side),
                                    NodeShape.ofSideSide(dir, side.getOpposite()),
                                    channel
                            )),
                            listener(be, side, channel)
                    ));
                }
            }
            return nodes;
        }
        return List.of();
    }

    private static ToIntFunction<LevelReader> reader(MicrochipBlockEntity be, Direction side, Channel channel) {
        return switch (channel) {
            case Channel.Redstone ignored -> _ -> be.getRedstoneOutput(side) * 15;
            case Channel.Single single -> {
                int bitIdx = single.color().ordinal();
                yield _ -> {
                    int bitVal = be.getRedstoneOutput(side);
                    return ((bitVal >> bitIdx) & 0x1) * 15;
                };
            }
        };
    }

    private static BiFunction<LevelAccessor, Integer, Map<Direction, SignalStrength>> listener(MicrochipBlockEntity be, Direction side, Channel channel) {
        int bundleBit = switch (channel) {
            case Channel.Redstone ignored -> -1;
            case Channel.Single single -> single.color().ordinal();
        };
        return (_, value) -> {
            be.receiveExternalInput(side, value, bundleBit);
            return Map.of();
        };
    }

    @Override
    public MapCodec<MicrochipSignalComponent> codec() {
        return CODEC;
    }
}
