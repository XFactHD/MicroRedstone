package io.github.xfacthd.microredstone.client.screen.dialog;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class QueryDialogScreen extends DialogScreen
{
    private static final int QUERY_WIDGET_PADDING = 2;

    private final List<QueryWidget> queryWidgets;
    private final List<BuiltQueryWidget> builtQueryWidgets = new ArrayList<>();
    private Runnable okStateUpdater = () -> {};

    QueryDialogScreen(Component title, List<Component> messageLines, List<QueryWidget> queryWidgets, Runnable okCallback, Runnable cancelCallback)
    {
        super(Type.QUERY, title, messageLines, mergeCallbacks(queryWidgets, okCallback), cancelCallback);
        this.queryWidgets = queryWidgets;
    }

    @Override
    protected boolean computeContent()
    {
        boolean addPadding = super.computeContent();

        builtQueryWidgets.clear();

        boolean addWidgetPadding = false;
        for (QueryWidget queryWidget : queryWidgets)
        {
            if (addPadding)
            {
                imageHeight += PADDING;
                addPadding = false;
            }
            else if (addWidgetPadding)
            {
                imageHeight += QUERY_WIDGET_PADDING;
            }

            int labelWidth = font.width(queryWidget.label()) + PADDING;
            MutableInt maxWidgetX = new MutableInt();
            MutableInt maxWidgetY = new MutableInt();
            List<AbstractWidget> widgets = new ArrayList<>();
            queryWidget.setupWidget(font, labelWidth, imageHeight, MAX_TEXT_WIDTH - labelWidth, widget ->
            {
                maxWidgetX.setValue(Math.max(maxWidgetX.intValue(), widget.getRight()));
                maxWidgetY.setValue(Math.max(maxWidgetY.intValue(), widget.getBottom()));
                widgets.add(widget);
            });
            int height = maxWidgetY.intValue() - imageHeight;
            int labelY = imageHeight + (height / 2) - (font.lineHeight / 2);
            imageWidth = Math.max(imageWidth, maxWidgetX.intValue() + PADDING * 2);
            imageHeight += height;
            builtQueryWidgets.add(new BuiltQueryWidget(queryWidget.label(), labelY, widgets));

            addWidgetPadding = true;
        }
        if (!queryWidgets.isEmpty())
        {
            imageHeight += PADDING;
        }

        return addPadding;
    }

    @Override
    protected void finalizeContent()
    {
        super.finalizeContent();

        builtQueryWidgets.replaceAll(queryWidget ->
        {
            for (AbstractWidget widget : queryWidget.widgets())
            {
                widget.setPosition(leftPos + PADDING + widget.getX(), topPos + widget.getY());
                addRenderableWidget(widget);
            }
            return queryWidget.offsetLabelY(topPos);
        });
        if (!queryWidgets.isEmpty())
        {
            BooleanSupplier state = () ->
            {
                for (QueryWidget queryWidget : queryWidgets)
                {
                    if (!queryWidget.isInputValid()) return false;
                }
                return true;
            };
            okStateUpdater = () -> buttonPair.okButton().active = state.getAsBoolean();
        }
    }

    @Override
    protected int extractContent(GuiGraphicsExtractor graphics, int contentX, int mouseX, int mouseY, float partialTick)
    {
        int contentY = super.extractContent(graphics, contentX, mouseX, mouseY, partialTick);
        for (BuiltQueryWidget widget : builtQueryWidgets)
        {
            graphics.text(font, widget.label(), contentX, widget.labelY(), 0xFF404040, false);
        }
        okStateUpdater.run();
        return contentY;
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        if (event.isConfirmation())
        {
            if (!buttonPair.isFocused() && buttonPair.okButton().active)
            {
                buttonPair.okButton().onPress(event);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private static Runnable mergeCallbacks(List<QueryWidget> queryWidgets, Runnable okCallback)
    {
        return queryWidgets.isEmpty() ? okCallback : () ->
        {
            queryWidgets.forEach(QueryWidget::saveQueryResult);
            okCallback.run();
        };
    }
}
