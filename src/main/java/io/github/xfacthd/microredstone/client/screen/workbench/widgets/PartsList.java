package io.github.xfacthd.microredstone.client.screen.workbench.widgets;

import com.mojang.datafixers.util.Either;
import io.github.xfacthd.microredstone.client.screen.workbench.CircuitWorkbenchScreen;
import io.github.xfacthd.microredstone.common.circuit.connection.Connection;
import io.github.xfacthd.microredstone.common.circuit.connection.WireType;
import io.github.xfacthd.microredstone.common.circuit.node.NodePos;
import io.github.xfacthd.microredstone.common.circuit.prototype.BufferPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ClockPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.ConverterPrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.IconConfig;
import io.github.xfacthd.microredstone.common.circuit.prototype.PlaceableNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrimitivePrototypeNode;
import io.github.xfacthd.microredstone.common.circuit.prototype.PrototypeNode;
import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public final class PartsList
{
    private static final ResourceLocation BUTTON = Utils.rl("minecraft", "widget/button");
    private static final ResourceLocation BUTTON_HOVER = Utils.rl("minecraft", "widget/button_highlighted");
    private static final ResourceLocation SCROLLER_BACKGROUND = Utils.rl("scroller_background");
    private static final ResourceLocation SCROLLER_HANDLE = Utils.rl("minecraft", "container/villager/scroller");

    private static final int PADDING = 5;
    private static final int BORDER = 1;
    private static final int WIDTH = 120;
    static final int INNER_WIDTH = WIDTH - (BORDER * 2);
    private static final int MAX_HEIGHT = CircuitCanvas.MAX_HEIGHT;
    private static final int ENTRY_WIDTH = WIDTH - (BORDER * 2);
    static final int ENTRY_HEIGHT = 30;
    private static final int ENTRY_ICON_SIZE = 16;
    private static final int ICON_OFF_Y = (ENTRY_HEIGHT / 2) - (ENTRY_ICON_SIZE / 2);
    private static final int ENTRY_NAME_OFF_X = ENTRY_ICON_SIZE + (PADDING * 2);
    private static final int ENTRY_NAME_OFF_Y = 11;
    private static final int ENTRY_NAME_BORDER_RIGHT = 3;
    private static final int SCROLLER_HANDLE_WIDTH = 6;
    private static final int SCROLLER_HANDLE_HEIGHT = 27;
    static final int SCROLLER_BG_WIDTH = SCROLLER_HANDLE_WIDTH + 2;
    public static final int FULL_WIDTH = WIDTH + SCROLLER_BG_WIDTH;
    private static final int FULL_INNER_WIDTH = FULL_WIDTH - (BORDER * 2);
    private static final int SCROLL_SPEED = 10;

    private static final Entry[] ENTRIES = new Entry[] {
            entry("connection_single").spec(Connection.ICON_SINGLE_IN, WireType.SINGLE).build(),
            entry("connection_bundled").spec(Connection.ICON_BUNDLED_IN, WireType.BUNDLED).build(),
            entry("clock").spec(ClockPrototypeNode.ICON, ClockPrototypeNode::new).withoutSubtitle().build(),
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
    private static final int ENTRIES_HEIGHT = ENTRIES.length * ENTRY_HEIGHT;

    private final CircuitWorkbenchScreen owner;
    private int listX;
    private int scrollbarX;
    private int y;
    private int height;
    private int innerListX;
    private int innerScrollbarX;
    private int innerY;
    private int innerHeight;
    private int scrollOffset = 0;
    private boolean dragging = false;

    public PartsList(CircuitWorkbenchScreen owner)
    {
        this.owner = owner;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY)
    {
        int minX = innerListX;
        int maxX = minX + ENTRY_WIDTH;
        int minY = innerY;
        int maxY = minY + innerHeight;
        boolean mouseOverX = mouseX >= minX && mouseX < maxX;
        boolean mouseOverY = mouseY >= minY && mouseY < maxY;

        graphics.fill(minX, minY, maxX, maxY, 0xFF333333);

        graphics.enableScissor(minX, minY, maxX, maxY);

        for (int i = 0; i < ENTRIES.length; i++)
        {
            int y = minY + i * ENTRY_HEIGHT - scrollOffset;
            if (y + ENTRY_HEIGHT < minY || y > maxY)
            {
                continue;
            }

            Entry entry = ENTRIES[i];

            boolean hovered = mouseOverX && mouseOverY && mouseY >= y && mouseY < y + ENTRY_HEIGHT;
            ResourceLocation sprite = hovered ? BUTTON_HOVER : BUTTON;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, minX, y, ENTRY_WIDTH, ENTRY_HEIGHT);

            CircuitCanvas.drawPartNode(graphics, entry.icon, minX + CircuitWorkbenchScreen.PADDING, y + ICON_OFF_Y, 0, ENTRY_ICON_SIZE);

            int nameX = minX + ENTRY_NAME_OFF_X;
            int nameY = y + ENTRY_NAME_OFF_Y;
            // FIXME: the scrolling helper passes the wrong max Y to the actual scrolling string helper (missing +1 pixel)
            EntryTexts texts = entry.texts;
            if (texts.subTitle != null)
            {
                graphics.drawScrollingString(owner.getFont(), texts.title, nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY - 6, 0xFFFFFFFF);
                graphics.drawScrollingString(owner.getFont(), texts.subTitle, nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY + 5, 0xFFFFFFFF);
            }
            else
            {
                graphics.drawScrollingString(owner.getFont(), texts.title, nameX, maxX - ENTRY_NAME_BORDER_RIGHT, nameY, 0xFFFFFFFF);
            }
        }

        graphics.disableScissor();

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CircuitWorkbenchScreen.WINDOW_FRAME, listX, y, WIDTH, height);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND, scrollbarX, y, SCROLLER_BG_WIDTH, height);
        float scrollFactor = (float) scrollOffset / (ENTRIES_HEIGHT - innerHeight);
        int scrollerY = minY + (int)(scrollFactor * (innerHeight - SCROLLER_HANDLE_HEIGHT));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_HANDLE, innerScrollbarX, scrollerY, SCROLLER_HANDLE_WIDTH, SCROLLER_HANDLE_HEIGHT);
    }

    public boolean isMouseOver(double mouseX, double mouseY)
    {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) return false;
        return mouseX >= innerListX && mouseX < (innerListX + FULL_INNER_WIDTH);
    }

    public boolean isMouseOverList(double mouseX, double mouseY)
    {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) return false;
        return mouseX >= innerListX && mouseX < (innerListX + INNER_WIDTH);
    }

    public boolean isMouseOverScrollBar(double mouseX, double mouseY)
    {
        if (mouseY < innerY || mouseY >= (innerY + innerHeight)) return false;
        return mouseX >= innerScrollbarX && mouseX < (innerScrollbarX + SCROLLER_HANDLE_WIDTH);
    }

    @Nullable
    public Either<NodePos, Integer> getClickedPartIdx(double mouseY)
    {
        int relY = (int) (mouseY - innerY + scrollOffset);
        return relY >= 0 ? Either.right(relY / ENTRY_HEIGHT) : null;
    }

    public void scroll(double yDiff)
    {
        int offset = (int) (yDiff * SCROLL_SPEED);
        scrollOffset = Mth.clamp(scrollOffset + offset, 0, ENTRIES_HEIGHT - innerHeight);
    }

    public void dragScrollBar(double mouseY)
    {
        int maxOffset = ENTRIES_HEIGHT - innerHeight;
        double offset = (mouseY - innerY - (SCROLLER_HANDLE_HEIGHT / 2F)) / (innerHeight - SCROLLER_HANDLE_HEIGHT);
        scrollOffset = (int) Mth.clamp(offset * maxOffset, 0, maxOffset);
    }

    public void setDragging(boolean dragging)
    {
        this.dragging = dragging;
    }

    public boolean isDragging()
    {
        return dragging;
    }

    public void computeDimensions(int leftPos, int topPos, int imageWidth, int height)
    {
        int windowPadding = CircuitWorkbenchScreen.PADDING * 2;
        this.height = Math.min(MAX_HEIGHT, height - windowPadding - CircuitWorkbenchScreen.NON_CIRCUIT_HEIGHT);
        innerHeight = this.height - (BORDER * 2);
        listX = leftPos + imageWidth - CircuitWorkbenchScreen.BORDER_RIGHT - WIDTH - SCROLLER_BG_WIDTH;
        scrollbarX = listX + WIDTH;
        y = topPos + CircuitWorkbenchScreen.OFFSET_TOP;
        innerListX = listX + BORDER;
        innerScrollbarX = scrollbarX + BORDER;
        innerY = y + BORDER;
    }

    public static Entry getEntryAt(int index)
    {
        return ENTRIES[index];
    }

    public static EntryTexts getEntryName(String componentName)
    {
        for (Entry entry : ENTRIES)
        {
            if (entry.name.equals(componentName))
            {
                return entry.texts;
            }
        }
        throw new IllegalArgumentException("Unknown component: " + componentName);
    }

    private static EntryBuilder entry(String name)
    {
        return new EntryBuilder(name);
    }

    public record Entry(String name, EntryTexts texts, IconConfig icon, Supplier<? extends PlaceableNode> factory)
    {
        public PlaceableNode create()
        {
            return factory.get();
        }
    }

    public record EntryTexts(Component title, @Nullable Component subTitle, Component description) { }

    private static final class EntryBuilder
    {
        private final String name;
        private boolean hasSubtitle = true;
        @Nullable
        private IconConfig icon = null;
        @Nullable
        private Supplier<? extends PlaceableNode> factory = null;

        private EntryBuilder(String name)
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
            return new Entry(name, new EntryTexts(title, subTitle, description), icon, factory);
        }
    }

    private interface Provider<T>
    {
        T get(int inputCount, WireType wireType);
    }
}
