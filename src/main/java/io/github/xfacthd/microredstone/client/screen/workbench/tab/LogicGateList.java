package io.github.xfacthd.microredstone.client.screen.workbench.tab;

import io.github.xfacthd.microredstone.client.screen.workbench.widgets.NodeListWidget;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConstantPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.special.LampPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.primitive.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.spec.IconConfig;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public final class LogicGateList
{
    static final Entry[] ENTRIES = new Entry[] {
            entry("connection_single").spec(Connection.ICON_SINGLE_IN, WireType.SINGLE).build(),
            entry("connection_bundled").spec(Connection.ICON_BUNDLED_IN, WireType.BUNDLED).build(),
            entry("constant_single").spec(ConstantPrototypeNode.ICON_SINGLE, () -> new ConstantPrototypeNode(WireType.SINGLE)).build(),
            entry("constant_bundled").spec(ConstantPrototypeNode.ICON_BUNDLED, () -> new ConstantPrototypeNode(WireType.BUNDLED)).build(),
            entry("clock").spec(ClockPrototypeNode.ICON, ClockPrototypeNode::new).withoutSubtitle().build(),
            entry("lamp").spec(LampPrototypeNode.ICON, LampPrototypeNode::new).withoutSubtitle().build(),
            entry("buffer_single").spec(BufferPrototypeNode.ICON_SINGLE, () -> new BufferPrototypeNode(WireType.SINGLE)).build(),
            entry("buffer_bundled").spec(BufferPrototypeNode.ICON_BUNDLED, () -> new BufferPrototypeNode(WireType.BUNDLED)).build(),
            entry("not_single").spec(1, WireType.SINGLE, PrimitivePrototypeNode.Type.NOT::icon, PrimitivePrototypeNode.Type.NOT::factory).build(),
            entry("and_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.AND::icon, PrimitivePrototypeNode.Type.AND::factory).build(),
            entry("and_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.AND::icon, PrimitivePrototypeNode.Type.AND::factory).build(),
            entry("or_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.OR::icon, PrimitivePrototypeNode.Type.OR::factory).build(),
            entry("or_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.OR::icon, PrimitivePrototypeNode.Type.OR::factory).build(),
            entry("xor_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.XOR::icon, PrimitivePrototypeNode.Type.XOR::factory).build(),
            entry("xor_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.XOR::icon, PrimitivePrototypeNode.Type.XOR::factory).build(),
            entry("nand_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.NAND::icon, PrimitivePrototypeNode.Type.NAND::factory).build(),
            entry("nand_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.NAND::icon, PrimitivePrototypeNode.Type.NAND::factory).build(),
            entry("nor_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.NOR::icon, PrimitivePrototypeNode.Type.NOR::factory).build(),
            entry("nor_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.NOR::icon, PrimitivePrototypeNode.Type.NOR::factory).build(),
            entry("xnor_two_single").spec(2, WireType.SINGLE, PrimitivePrototypeNode.Type.XNOR::icon, PrimitivePrototypeNode.Type.XNOR::factory).build(),
            entry("xnor_three_single").spec(3, WireType.SINGLE, PrimitivePrototypeNode.Type.XNOR::icon, PrimitivePrototypeNode.Type.XNOR::factory).build(),
            entry("not_bundled").spec(1, WireType.BUNDLED, PrimitivePrototypeNode.Type.NOT::icon, PrimitivePrototypeNode.Type.NOT::factory).build(),
            entry("and_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.AND::icon, PrimitivePrototypeNode.Type.AND::factory).build(),
            entry("and_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.AND::icon, PrimitivePrototypeNode.Type.AND::factory).build(),
            entry("or_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.OR::icon, PrimitivePrototypeNode.Type.OR::factory).build(),
            entry("or_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.OR::icon, PrimitivePrototypeNode.Type.OR::factory).build(),
            entry("xor_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.XOR::icon, PrimitivePrototypeNode.Type.XOR::factory).build(),
            entry("xor_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.XOR::icon, PrimitivePrototypeNode.Type.XOR::factory).build(),
            entry("nand_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.NAND::icon, PrimitivePrototypeNode.Type.NAND::factory).build(),
            entry("nand_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.NAND::icon, PrimitivePrototypeNode.Type.NAND::factory).build(),
            entry("nor_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.NOR::icon, PrimitivePrototypeNode.Type.NOR::factory).build(),
            entry("nor_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.NOR::icon, PrimitivePrototypeNode.Type.NOR::factory).build(),
            entry("xnor_two_bundled").spec(2, WireType.BUNDLED, PrimitivePrototypeNode.Type.XNOR::icon, PrimitivePrototypeNode.Type.XNOR::factory).build(),
            entry("xnor_three_bundled").spec(3, WireType.BUNDLED, PrimitivePrototypeNode.Type.XNOR::icon, PrimitivePrototypeNode.Type.XNOR::factory).build(),
            entry("packer").spec(ConverterPrototypeNode.Type.PACK.getIcon(), () -> new ConverterPrototypeNode(ConverterPrototypeNode.Type.PACK)).withoutSubtitle().build(),
            entry("unpacker").spec(ConverterPrototypeNode.Type.UNPACK.getIcon(), () -> new ConverterPrototypeNode(ConverterPrototypeNode.Type.UNPACK)).withoutSubtitle().build(),
    };
    static final int ENTRY_COUNT = ENTRIES.length;

    private static EntryBuilder entry(String name)
    {
        return new EntryBuilder(name);
    }

    public static PlaceableNode instantiate(int index)
    {
        return LogicGateList.ENTRIES[index].instantiate();
    }

    public static Entry getEntryByName(String componentName)
    {
        for (Entry entry : ENTRIES)
        {
            if (entry.name().equals(componentName))
            {
                return entry;
            }
        }
        throw new IllegalArgumentException("Unknown component: " + componentName);
    }

    public record Entry(
            String name,
            Component title,
            @Nullable Component subTitle,
            Component description,
            IconConfig icon,
            Supplier<? extends PlaceableNode> factory
    ) implements NodeListWidget.Entry
    {
        @Override
        public PlaceableNode instantiate()
        {
            return factory.get();
        }
    }

    private static final class EntryBuilder
    {
        private final String name;
        private boolean hasSubtitle = true;
        @Nullable
        private IconConfig icon = null;
        @Nullable
        private Supplier<? extends PlaceableNode> factory = null;

        EntryBuilder(String name)
        {
            this.name = name;
        }

        EntryBuilder spec(IconConfig icon, Supplier<PrototypeNode> factory)
        {
            this.icon = icon;
            this.factory = factory;
            return this;
        }

        EntryBuilder spec(int inputCount, WireType wireType, Provider<IconConfig> icon, Provider<Supplier<PrototypeNode>> factory)
        {
            return spec(icon.get(inputCount, wireType), factory.get(inputCount, wireType));
        }

        EntryBuilder spec(IconConfig icon, WireType wireType)
        {
            this.icon = icon;
            this.factory = () -> new Connection(wireType);
            return this;
        }

        EntryBuilder withoutSubtitle()
        {
            hasSubtitle = false;
            return this;
        }

        Entry build()
        {
            Objects.requireNonNull(icon);
            Objects.requireNonNull(factory);

            String translationSuffix = "circuit_workbench.part_entry." + name;
            Component title = Utils.translate("label", translationSuffix);
            Component subTitle = hasSubtitle ? Utils.translate("subtitle", translationSuffix) : null;
            Component description = Utils.translate("desc", translationSuffix);
            return new Entry(name, title, subTitle, description, icon, factory);
        }

        interface Provider<T>
        {
            T get(int inputCount, WireType wireType);
        }
    }

    private LogicGateList() {}
}
