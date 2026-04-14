package io.github.xfacthd.microredstone.client.util;

import io.github.xfacthd.microredstone.common.circuit.connection.Port;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class PortOverlays {
    private static final Port[] PORTS = Port.values();
    private static final List<Port> OVERLAY_PORT_ORDER = List.of(Port.UP, Port.DOWN, Port.LEFT, Port.RIGHT);
    private static final @Nullable Identifier[] OVERLAY_LOCATIONS = buildOverlayLocations();

    public static Identifier get(int portMask) {
        return Objects.requireNonNull(OVERLAY_LOCATIONS[portMask]);
    }

    public static void forEach(BiConsumer<Identifier, Map<Port, WireType>> consumer) {
        for (int i = 1; i < OVERLAY_LOCATIONS.length; i++) {
            if (OVERLAY_LOCATIONS[i] == null) {
                continue;
            }

            Map<Port, WireType> ports = new EnumMap<>(Port.class);
            for (Port port : PORTS) {
                if ((i & port.appendMask(0, WireType.SINGLE)) != 0) {
                    ports.put(port, WireType.SINGLE);
                } else if ((i & port.appendMask(0, WireType.BUNDLED)) != 0) {
                    ports.put(port, WireType.BUNDLED);
                }
            }
            consumer.accept(OVERLAY_LOCATIONS[i], Map.copyOf(ports));
        }
    }

    private static @Nullable Identifier[] buildOverlayLocations() {
        @Nullable WireType[] types = Arrays.copyOf(WireType.values(), 5);
        @Nullable Identifier[] map = new Identifier[256];
        for (WireType typeUp : types) {
            for (WireType typeRight : types) {
                for (WireType typeDown : types) {
                    for (WireType typeLeft : types) {
                        int portMask = 0;
                        if (typeUp != null) {
                            portMask = Port.UP.appendMask(portMask, typeUp);
                        }
                        if (typeRight != null) {
                            portMask = Port.RIGHT.appendMask(portMask, typeRight);
                        }
                        if (typeDown != null) {
                            portMask = Port.LEFT.appendMask(portMask, typeDown);
                        }
                        if (typeLeft != null) {
                            portMask = Port.DOWN.appendMask(portMask, typeLeft);
                        }
                        map[portMask] = buildOverlayLocation(portMask);
                    }
                }
            }
        }
        return map;
    }

    private static @Nullable Identifier buildOverlayLocation(int portMask) {
        return switch (portMask) {
            case 0b0000_0000 -> null;
            case 0b0000_1111 -> Utils.rl("port/full_single");
            case 0b1111_0000 -> Utils.rl("port/full_bundled");
            case 0b0000_0001 -> Utils.rl("port/up_single");
            case 0b0000_0010 -> Utils.rl("port/right_single");
            case 0b0000_0100 -> Utils.rl("port/down_single");
            case 0b0000_1000 -> Utils.rl("port/left_single");
            case 0b0000_1010 -> Utils.rl("port/hor_single");
            case 0b0000_0101 -> Utils.rl("port/vert_single");
            case 0b0001_0000 -> Utils.rl("port/up_bundled");
            case 0b0010_0000 -> Utils.rl("port/right_bundled");
            case 0b0100_0000 -> Utils.rl("port/down_bundled");
            case 0b1000_0000 -> Utils.rl("port/left_bundled");
            case 0b1010_0000 -> Utils.rl("port/hor_bundled");
            case 0b0101_0000 -> Utils.rl("port/vert_bundled");
            default -> {
                boolean suffixIndividually = (portMask & 0b1111_0000) != 0 && (portMask & 0b0000_1111) != 0;
                StringBuilder builder = new StringBuilder();
                for (Port port : OVERLAY_PORT_ORDER) {
                    boolean single = (portMask & port.appendMask(0, WireType.SINGLE)) != 0;
                    boolean bundled = (portMask & port.appendMask(0, WireType.BUNDLED)) != 0;
                    if (!single && !bundled) {
                        continue;
                    }

                    if (!builder.isEmpty()) {
                        builder.append("_");
                    }
                    builder.append(port.getSerializedName());
                    if (suffixIndividually) {
                        builder.append(single ? "_single" : "_bundled");
                    }
                }
                if (!suffixIndividually) {
                    boolean single = (portMask & 0b1111_0000) == 0;
                    builder.append(single ? "_single" : "_bundled");
                }
                yield Utils.rl("port/").withSuffix(builder.toString());
            }
        };
    }

    private PortOverlays() { }
}
