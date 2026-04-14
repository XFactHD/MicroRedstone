package io.github.xfacthd.microredstone.common.data;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class PropertyHolder {
    public static final EnumProperty<Rotation> ROTATION = EnumProperty.create("rotation", Rotation.class);

    public static final BooleanProperty HAS_CIRCUIT = BooleanProperty.create("has_circuit");

    private PropertyHolder() { }
}
