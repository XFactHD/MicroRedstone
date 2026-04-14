package io.github.xfacthd.microredstone.common.data.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public sealed interface ShareInfo {
    Codec<ShareInfo> CODEC = ShareType.CODEC.dispatch(ShareInfo::type, ShareType::getInfoCodec);
    StreamCodec<ByteBuf, ShareInfo> STREAM_CODEC = ShareType.STREAM_CODEC.dispatch(ShareInfo::type, ShareType::getInfoStreamCodec);

    boolean isVisibleTo(UUID author, UUID toCheck);

    ShareType type();

    ShareInfo withoutShareTargets();

    record Private() implements ShareInfo {
        public static final Private INSTANCE = new Private();
        static final MapCodec<Private> CODEC = MapCodec.unit(INSTANCE);
        static final StreamCodec<ByteBuf, Private> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public boolean isVisibleTo(UUID author, UUID toCheck) {
            return toCheck.equals(author);
        }

        @Override
        public ShareInfo withoutShareTargets() {
            return this;
        }

        @Override
        public ShareType type() {
            return ShareType.PRIVATE;
        }
    }

    record Shared(Set<UUID> sharedTo) implements ShareInfo {
        static final MapCodec<Shared> CODEC = UUIDUtil.CODEC.listOf()
                .xmap(Set::copyOf, List::copyOf)
                .xmap(Shared::new, Shared::sharedTo)
                .fieldOf("shared_to");
        static final StreamCodec<ByteBuf, Shared> STREAM_CODEC = UUIDUtil.STREAM_CODEC
                .apply(ByteBufCodecs.<ByteBuf, UUID, Set<UUID>>collection(HashSet::new))
                .map(Set::copyOf, Function.identity())
                .map(Shared::new, Shared::sharedTo);

        public Shared {
            sharedTo = Set.copyOf(sharedTo);
        }

        @Override
        public boolean isVisibleTo(UUID author, UUID toCheck) {
            return toCheck.equals(author) || sharedTo.contains(toCheck);
        }

        @Override
        public ShareInfo withoutShareTargets() {
            return new Shared(Set.of());
        }

        @Override
        public ShareType type() {
            return ShareType.SHARED;
        }
    }

    record Public() implements ShareInfo {
        public static final Public INSTANCE = new Public();
        static final MapCodec<Public> CODEC = MapCodec.unit(INSTANCE);
        static final StreamCodec<ByteBuf, Public> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public boolean isVisibleTo(UUID author, UUID toCheck) {
            return true;
        }

        @Override
        public ShareInfo withoutShareTargets() {
            return this;
        }

        @Override
        public ShareType type() {
            return ShareType.PUBLIC;
        }
    }
}
