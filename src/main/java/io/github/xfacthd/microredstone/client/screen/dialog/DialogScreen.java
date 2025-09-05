package io.github.xfacthd.microredstone.client.screen.dialog;

import io.github.xfacthd.microredstone.common.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BinaryOperator;

public final class DialogScreen extends Screen
{
    private static final ResourceLocation BACKGROUND = Utils.rl("dialog/background");
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 10;
    private static final int TITLE_X = PADDING + ICON_SIZE + PADDING;
    private static final int TITLE_Y = PADDING + 1;
    private static final int CONTENT_Y = PADDING + ICON_SIZE + PADDING * 2;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MIN_WIDTH = BUTTON_WIDTH * 2 + PADDING * 3;
    private static final int MAX_WIDTH = 220;
    private static final int MAX_TEXT_WIDTH = MAX_WIDTH - PADDING * 2;
    private static final int BASE_HEIGHT = CONTENT_Y + PADDING * 2 + BUTTON_HEIGHT + PADDING;

    private final Type type;
    private final List<Component> messageLines;
    private final List<Property> properties;
    private final Runnable okCallback;
    private final Runnable cancelCallback;
    private final List<List<FormattedCharSequence>> textBlocks = new ArrayList<>();
    private final List<FormattedProperty> propertyLines = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private int propValueX;

    public static DialogScreenBuilder builder(Type type)
    {
        return new DialogScreenBuilder(type);
    }

    DialogScreen(
            Type type,
            Component title,
            List<Component> messageLines,
            List<Property> properties,
            Runnable okCallback,
            Runnable cancelCallback
    )
    {
        super(title);
        this.type = type;
        this.messageLines = messageLines;
        this.properties = properties;
        this.okCallback = okCallback;
        this.cancelCallback = cancelCallback;
    }

    @Override
    protected void init()
    {
        textBlocks.clear();
        propertyLines.clear();

        imageWidth = MIN_WIDTH;
        imageHeight = BASE_HEIGHT;
        boolean addPadding = false;
        for (Component line : messageLines)
        {
            if (addPadding)
            {
                imageHeight += PADDING;
            }

            List<FormattedCharSequence> segments = font.split(line, MAX_TEXT_WIDTH);
            for (FormattedCharSequence segment : segments)
            {
                int segmentWidth = font.width(segment) + PADDING * 2;
                imageWidth = Math.max(imageWidth, segmentWidth);
            }
            imageHeight += segments.size() * font.lineHeight;
            textBlocks.add(segments);

            addPadding = true;
        }
        int maxLabelWidth = 0;
        for (Property property : properties)
        {
            if (addPadding)
            {
                imageHeight += PADDING;
            }

            maxLabelWidth = Math.max(maxLabelWidth, font.width(property.label()));
            imageHeight += font.lineHeight;

            addPadding = false;
        }
        int maxValueWidth = MAX_TEXT_WIDTH - PADDING - maxLabelWidth;
        for (Property property : properties)
        {
            FormattedProperty formatted = property.format(font, maxValueWidth);
            propertyLines.add(formatted);
            formatted.value().text(); // Try resolving potential DelayedValues
            int lineWidth = maxLabelWidth + PADDING * 3 + formatted.value().valueWidth();
            imageWidth = Math.max(imageWidth, lineWidth);
        }

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;
        propValueX = leftPos + PADDING * 2 + maxLabelWidth;

        type.initialize(this, leftPos + imageWidth / 2, okCallback, cancelCallback);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderMenuBackground(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, imageWidth, imageHeight);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.icon, leftPos + PADDING, topPos + PADDING, ICON_SIZE, ICON_SIZE);
        graphics.drawString(font, title, leftPos + TITLE_X, topPos + TITLE_Y, 0xFF404040, false);

        int contentX = leftPos + PADDING;
        int contentY = topPos + CONTENT_Y;
        for (List<FormattedCharSequence> block : textBlocks)
        {
            for (FormattedCharSequence line : block)
            {
                graphics.drawString(font, line, contentX, contentY, 0xFF404040, false);
                contentY += font.lineHeight;
            }
            contentY += PADDING;
        }
        for (FormattedProperty line : propertyLines)
        {
            graphics.drawString(font, line.label(), contentX, contentY, 0xFF404040, false);
            FormattedProperty.Value value = line.value();
            graphics.drawString(font, value.text(), propValueX, contentY, 0xFF404040, false);
            Component tooltip = value.tooltip();
            if (tooltip != null && mouseY >= contentY && mouseY < contentY + font.lineHeight && mouseX >= propValueX && mouseX < propValueX + value.valueWidth())
            {
                graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            }
            contentY += font.lineHeight;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            type.escCallbackSelector.apply(okCallback, cancelCallback).run();
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    public enum Type
    {
        INFO(Utils.rl("dialog/icon_info"), false, CommonComponents.GUI_OK, (ok, cancel) -> ok),
        WARNING(Utils.rl("dialog/icon_warning"), false, CommonComponents.GUI_OK, (ok, cancel) -> ok),
        ERROR(Utils.rl("dialog/icon_error"), false, CommonComponents.GUI_OK, (ok, cancel) -> ok),
        CONFIRM(Utils.rl("dialog/icon_confirm"), true, CommonComponents.GUI_YES, (ok, cancel) -> cancel),
        PROPERTIES(Utils.rl("dialog/icon_properties"), false, CommonComponents.GUI_OK, (ok, cancel) -> ok),
        ;

        private final ResourceLocation icon;
        final Component defaultTitle = Utils.translate("title", "dialog.title." + toString().toLowerCase(Locale.ROOT));
        final boolean hasCancel;
        private final Component okText;
        private final BinaryOperator<Runnable> escCallbackSelector;

        Type(ResourceLocation icon, boolean hasCancel, Component okText, BinaryOperator<Runnable> escCallbackSelector)
        {
            this.icon = icon;
            this.hasCancel = hasCancel;
            this.okText = okText;
            this.escCallbackSelector = escCallbackSelector;
        }

        void initialize(DialogScreen screen, int xCenter, Runnable okCallback, Runnable cancelCallback)
        {
            if (hasCancel)
            {
                int halfWidth = (xCenter - screen.leftPos) / 2;
                int xLeft = screen.leftPos + halfWidth - BUTTON_WIDTH / 2;
                addButton(screen, xLeft, okText, okCallback);
                int xRight = xCenter + halfWidth - BUTTON_WIDTH / 2;
                addButton(screen, xRight, CommonComponents.GUI_CANCEL, cancelCallback);
            }
            else
            {
                int x = xCenter - BUTTON_WIDTH / 2;
                addButton(screen, x, okText, okCallback);
            }
        }

        private static void addButton(DialogScreen screen, int x, Component text, Runnable callback)
        {
            int y = screen.topPos + screen.imageHeight - PADDING - BUTTON_HEIGHT;
            screen.addRenderableWidget(
                    Button.builder(text, btn ->
                            {
                                callback.run();
                                screen.onClose();
                            })
                            .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                            .build()
            );
        }

        public Component getDefaultTitle()
        {
            return defaultTitle;
        }
    }
}
