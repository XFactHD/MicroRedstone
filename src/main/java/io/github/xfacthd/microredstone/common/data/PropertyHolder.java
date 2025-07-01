package io.github.xfacthd.microredstone.common.data;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class PropertyHolder
{
    public static final EnumProperty<Rotation> ROTATION = EnumProperty.create("rotation", Rotation.class);

    private PropertyHolder() { }
}
