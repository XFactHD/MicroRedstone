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
    private final Runnable okCallback;
    private final Runnable cancelCallback;
    private final List<List<FormattedCharSequence>> textBlocks = new ArrayList<>();
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;

    public static DialogScreenBuilder builder(Type type)
    {
        return new DialogScreenBuilder(type);
    }

    DialogScreen(Type type, Component title, List<Component> messageLines, Runnable okCallback, Runnable cancelCallback)
    {
        super(title);
        this.type = type;
        this.messageLines = messageLines;
        this.okCallback = okCallback;
        this.cancelCallback = cancelCallback;
    }

    @Override
    protected void init()
    {
        textBlocks.clear();

        imageWidth = MIN_WIDTH;
        imageHeight = BASE_HEIGHT;
        boolean first = true;
        for (Component line : messageLines)
        {
            if (!first)
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

            first = false;
        }

        leftPos = (width - imageWidth) / 2;
        topPos = (height - imageHeight) / 2;

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

    public enum Type
    {
        INFO(Utils.rl("dialog/icon_info"), false, (ok, cancel) -> ok),
        WARNING(Utils.rl("dialog/icon_warning"), false, (ok, cancel) -> ok),
        ERROR(Utils.rl("dialog/icon_error"), false, (ok, cancel) -> ok),
        CONFIRM(Utils.rl("dialog/icon_confirm"), true, (ok, cancel) -> cancel)
        {
            @Override
            void initialize(DialogScreen screen, int xCenter, Runnable okCallback, Runnable cancelCallback)
            {
                int halfWidth = (xCenter - screen.leftPos) / 2;
                int xLeft = screen.leftPos + halfWidth - BUTTON_WIDTH / 2;
                Type.addButton(screen, xLeft, CommonComponents.GUI_YES, okCallback);
                int xRight = xCenter + halfWidth - BUTTON_WIDTH / 2;
                Type.addButton(screen, xRight, CommonComponents.GUI_CANCEL, cancelCallback);
            }
        },
        ;

        private final ResourceLocation icon;
        final Component defaultTitle = Utils.translate("title", "dialog.title." + toString().toLowerCase(Locale.ROOT));
        final boolean hasCancel;
        private final BinaryOperator<Runnable> escCallbackSelector;

        Type(ResourceLocation icon, boolean hasCancel, BinaryOperator<Runnable> escCallbackSelector)
        {
            this.icon = icon;
            this.hasCancel = hasCancel;
            this.escCallbackSelector = escCallbackSelector;
        }

        void initialize(DialogScreen screen, int xCenter, Runnable okCallback, Runnable cancelCallback)
        {
            int x = xCenter - BUTTON_WIDTH / 2;
            addButton(screen, x, CommonComponents.GUI_OK, okCallback);
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
