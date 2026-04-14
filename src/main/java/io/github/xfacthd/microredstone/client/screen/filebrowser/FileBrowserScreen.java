package io.github.xfacthd.microredstone.client.screen.filebrowser;

import io.github.xfacthd.microredstone.client.screen.dialog.DialogScreen;
import io.github.xfacthd.microredstone.client.screen.widgets.Scrollbar;
import io.github.xfacthd.microredstone.common.util.Utils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

// TODO: make title and dir/file icons
public final class FileBrowserScreen extends Screen {
    private static final Identifier BACKGROUND = Utils.rl("textures/gui/filebrowser.png");
    private static final Identifier ICON_DIRECTORY = Utils.rl("filebrowser/icon_directory");
    private static final Identifier ICON_FILE = Utils.rl("filebrowser/icon_file");
    public static final Component MSG_DIR_EMPTY = Utils.translate("msg", "filebrowser.dir_empty");
    public static final Component MSG_DIR_NO_MATCHING_FILES = Utils.translate("msg", "filebrowser.dir_no_matching_file");
    public static final Component LABEL_FILE_NAME = Utils.translate("label", "filebrowser.file_name");
    public static final Component LABEL_FILE_TYPE = Utils.translate("label", "filebrowser.file_type");
    public static final Component VALUE_FILE_TYPE_ALL = Utils.translate("value", "filebrowser.file_type.all").withStyle(ChatFormatting.ITALIC);
    public static final Component MSG_FILE_NOT_FOUND_LINE_ONE = Utils.translate("msg", "filebrowser.open.file_not_found.line_one");
    public static final Component MSG_FILE_NOT_FOUND_LINE_TWO = Utils.translate("msg", "filebrowser.open.file_not_found.line_two");
    public static final String TITLE_SAVE_CONFIRM_OVERWRITE = Utils.translationKey("title", "filebrowser.save.confirm_overwrite");
    public static final String MSG_SAVE_CONFIRM_OVERWRITE_LINE_ONE = Utils.translationKey("msg", "filebrowser.save.confirm_overwrite.line_one");
    public static final Component MSG_SAVE_CONFIRM_OVERWRITE_LINE_TWO = Utils.translate("msg", "filebrowser.save.confirm_overwrite.line_two");
    private static final WidgetSprites SPRITES_BACK = actionSprites("back");
    private static final WidgetSprites SPRITES_FORWARD = actionSprites("forward");
    private static final WidgetSprites SPRITES_UP = actionSprites("up");
    private static final WidgetSprites SPRITES_RELOAD = actionSprites("reload");
    private static final int WIDTH = 280;
    private static final int HEIGHT = 176;
    private static final int EDGE = 3;
    private static final int PADDING = 5;
    private static final int SMALL_PADDING = 1;
    private static final int ICON_SIZE = 10;
    private static final int TITLE_X = PADDING + ICON_SIZE + PADDING;
    private static final int TITLE_Y = PADDING + 2;
    private static final int ACTION_BUTTON_SIZE = 12;
    private static final int ACTION_BUTTON_BACK_X = EDGE + SMALL_PADDING;
    private static final int ACTION_BUTTON_FORWARD_X = ACTION_BUTTON_BACK_X + ACTION_BUTTON_SIZE + SMALL_PADDING;
    private static final int ACTION_BUTTON_UP_X = ACTION_BUTTON_FORWARD_X + ACTION_BUTTON_SIZE + SMALL_PADDING;
    private static final int ACTION_BUTTON_RELOAD_X = WIDTH - EDGE - SMALL_PADDING - ACTION_BUTTON_SIZE;
    private static final int ACTION_BUTTON_Y = 20;
    private static final int PATH_MIN_X = ACTION_BUTTON_UP_X + ACTION_BUTTON_SIZE + SMALL_PADDING + 2;
    private static final int PATH_MAX_X = ACTION_BUTTON_RELOAD_X - SMALL_PADDING - 2;
    private static final int PATH_MAX_WIDTH = PATH_MAX_X - PATH_MIN_X;
    private static final int PATH_MIN_Y = 22;
    private static final int PATH_MAX_Y = 32;
    private static final int LIST_X = EDGE;
    private static final int LIST_Y = 35;
    private static final int LIST_WIDTH = WIDTH - LIST_X - EDGE;
    private static final int LIST_HEIGHT = 103;
    private static final int ENTRY_MIN_X = LIST_X + SMALL_PADDING;
    private static final int ENTRY_MIN_Y = LIST_Y + SMALL_PADDING;
    private static final int ENTRIES_WIDTH = LIST_WIDTH - 2;
    private static final int ENTRIES_HEIGHT = LIST_HEIGHT - 2;
    private static final int ENTRY_HEIGHT = 12;
    private static final int FILE_NAME_EDIT_X = 56;
    private static final int FILE_NAME_EDIT_Y = 141;
    private static final int FILE_NAME_EDIT_WIDTH = 220;
    private static final int FILE_NAME_EDIT_HEIGHT = 14;
    private static final int FILE_TYPE_X = 56;
    private static final int FILE_TYPE_Y = 157;
    private static final int EXEC_CANCEL_BUTTON_WIDTH = 56;
    private static final int EXEC_CANCEL_BUTTON_HEIGHT = 16;
    private static final int CANCEL_BUTTON_X = WIDTH - EDGE - SMALL_PADDING - EXEC_CANCEL_BUTTON_WIDTH;
    private static final int EXEC_BUTTON_X = CANCEL_BUTTON_X - SMALL_PADDING - EXEC_CANCEL_BUTTON_WIDTH;
    private static final int EXEC_CANCEL_BUTTON_Y = HEIGHT - EDGE - SMALL_PADDING - EXEC_CANCEL_BUTTON_HEIGHT;

    private final Type type;
    private final Component typeDesc;
    private final Path rootPath;
    private final FileNameSuffix fileNameSuffix;
    private final Path initialPath;
    private final Consumer<@Nullable Path> pathConsumer;
    private final List<Path> pathHistory = new ArrayList<>();
    private final List<DirEntry> currEntries = new ArrayList<>();
    private final Set<GuiEventListener> dropFocusAfterClick = new ReferenceOpenHashSet<>();
    private int leftPos;
    private int topPos;
    @UnknownNullability
    private Button actionButtonBack;
    @UnknownNullability
    private Button actionButtonForward;
    @UnknownNullability
    private Button actionButtonUp;
    @UnknownNullability
    private Button actionButtonReload;
    @UnknownNullability
    private Scrollbar scrollbar;
    @UnknownNullability
    private EditBox fileNameEdit;
    @UnknownNullability
    private Button execButton;
    @UnknownNullability
    private Button cancelButton;
    @UnknownNullability
    private Path currPath;
    private boolean dirEmpty = true;
    private boolean noMatchingFiles = false;
    private int historyIdx = -1;
    private FormattedPath formattedPath = new FormattedPath("", 0);
    private int selectedEntry = -1;

    public static FileBrowserScreenBuilder builder(Type type) {
        return new FileBrowserScreenBuilder(type);
    }

    FileBrowserScreen(Type type, Component typeDesc, Path rootPath, @Nullable Path initialPath, FileNameSuffix fileNameSuffix, Consumer<@Nullable Path> pathConsumer) {
        super(Component.translatable(type.title, typeDesc));
        this.type = type;
        this.typeDesc = typeDesc;
        this.rootPath = rootPath;
        this.fileNameSuffix = fileNameSuffix;
        this.initialPath = Objects.requireNonNullElse(initialPath, rootPath);
        this.pathConsumer = pathConsumer;
    }

    @Override
    protected void init() {
        scrollbar = addRenderableOnly(new Scrollbar(0, 0, LIST_HEIGHT, ENTRIES_HEIGHT, true, true, () -> currEntries.size() * ENTRY_HEIGHT, this::isMouseOverList, scrollbar));

        actionButtonBack = addRenderableWidget(new ImageButton(ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, SPRITES_BACK, this::back, Component.empty()));
        actionButtonForward = addRenderableWidget(new ImageButton(ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, SPRITES_FORWARD, this::forward, Component.empty()));
        actionButtonUp = addRenderableWidget(new ImageButton(ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, SPRITES_UP, this::up, Component.empty()));
        actionButtonReload = addRenderableWidget(new ImageButton(ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE, SPRITES_RELOAD, this::reload, Component.empty()));

        fileNameEdit = addRenderableWidget(new EditBox(font, FILE_NAME_EDIT_WIDTH, FILE_NAME_EDIT_HEIGHT, Component.empty()));
        fileNameEdit.setMaxLength(48);
        fileNameEdit.setFilter(value -> !(value.contains("/") || value.contains("\\") || value.contains("..")));

        execButton = addRenderableWidget(Button.builder(type.buttonTitle, this::execute).size(EXEC_CANCEL_BUTTON_WIDTH, EXEC_CANCEL_BUTTON_HEIGHT).build());
        cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, this::cancel).size(EXEC_CANCEL_BUTTON_WIDTH, EXEC_CANCEL_BUTTON_HEIGHT).build());

        dropFocusAfterClick.addAll(Arrays.asList(actionButtonBack, actionButtonForward, actionButtonUp, actionButtonReload, execButton));

        repositionElements();

        setCurrentPath(initialPath, true);
    }

    @Override
    protected void repositionElements() {
        leftPos = (width / 2) - (WIDTH / 2);
        topPos = (height / 2) - (HEIGHT / 2);

        actionButtonBack.setPosition(leftPos + ACTION_BUTTON_BACK_X, topPos + ACTION_BUTTON_Y);
        actionButtonForward.setPosition(leftPos + ACTION_BUTTON_FORWARD_X, topPos + ACTION_BUTTON_Y);
        actionButtonUp.setPosition(leftPos + ACTION_BUTTON_UP_X, topPos + ACTION_BUTTON_Y);
        actionButtonReload.setPosition(leftPos + ACTION_BUTTON_RELOAD_X, topPos + ACTION_BUTTON_Y);
        scrollbar.setPosition(leftPos + LIST_X + LIST_WIDTH - scrollbar.getWidth(), topPos + LIST_Y);
        fileNameEdit.setPosition(leftPos + FILE_NAME_EDIT_X, topPos + FILE_NAME_EDIT_Y);
        execButton.setPosition(leftPos + EXEC_BUTTON_X, topPos + EXEC_CANCEL_BUTTON_Y);
        cancelButton.setPosition(leftPos + CANCEL_BUTTON_X, topPos + EXEC_CANCEL_BUTTON_Y);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, type.icon, leftPos + PADDING, topPos + PADDING, ICON_SIZE, ICON_SIZE);
        graphics.text(font, title, leftPos + TITLE_X, topPos + TITLE_Y, 0xFF404040, false);

        int nameLabelX = leftPos + FILE_NAME_EDIT_X - 2 - font.width(LABEL_FILE_NAME);
        int nameLabelY = topPos + FILE_NAME_EDIT_Y + 3;
        graphics.text(font, LABEL_FILE_NAME, nameLabelX, nameLabelY, 0xFF404040, false);

        int typeLabelX = leftPos + FILE_TYPE_X - 2 - font.width(LABEL_FILE_TYPE);
        int typeLabelY = topPos + FILE_TYPE_Y + 3;
        graphics.text(font, LABEL_FILE_TYPE, typeLabelX, typeLabelY, 0xFF404040, false);
        int typeValueX = leftPos + FILE_TYPE_X + 2;
        graphics.text(font, fileNameSuffix.label(), typeValueX, typeLabelY, 0xFF404040, false);

        extractPath(graphics);
        scrollbar.update();
        extractEntries(graphics, mouseX, mouseY);
    }

    private void extractPath(GuiGraphicsExtractor graphics) {
        int pathMinX = leftPos + PATH_MIN_X;
        int pathMaxX = leftPos + PATH_MAX_X;
        int pathMinY = topPos + PATH_MIN_Y;
        int pathMaxY = topPos + PATH_MAX_Y;
        graphics.enableScissor(pathMinX, pathMinY, pathMaxX, pathMaxY);
        int pathX = Math.min(pathMaxX - formattedPath.width, pathMinX);
        graphics.text(font, formattedPath.text, pathX, pathMinY, 0xFF404040, false);
        graphics.disableScissor();
    }

    private void extractEntries(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (dirEmpty || noMatchingFiles) {
            Component text = dirEmpty ? MSG_DIR_EMPTY : MSG_DIR_NO_MATCHING_FILES;
            int textWidth = font.width(text);
            int textX = leftPos + LIST_X + (LIST_WIDTH / 2) - (textWidth / 2);
            int textY = topPos + LIST_Y + PADDING;
            graphics.text(font, text, textX, textY, 0xFF404040, false);
            return;
        }

        int listMinY = topPos + LIST_Y;
        int listMaxY = listMinY + LIST_HEIGHT;
        int entryMinX = leftPos + ENTRY_MIN_X;
        int entryIconX = entryMinX + SMALL_PADDING;
        int entryNameX = entryIconX + ICON_SIZE + SMALL_PADDING;
        int entryMaxX = entryMinX + ENTRIES_WIDTH;
        if (scrollbar.isVisible()) {
            entryMaxX -= scrollbar.getWidth();
        }
        graphics.enableScissor(entryMinX, listMinY, entryMaxX, listMaxY);
        for (int i = 0; i < currEntries.size(); i++) {
            int minEntryY = listMinY + SMALL_PADDING + i * (ENTRY_HEIGHT) - Mth.floor(scrollbar.getOffset());
            int maxEntryY = minEntryY + ENTRY_HEIGHT;
            if (minEntryY >= listMaxY) {
                break;
            }
            if (maxEntryY <= listMinY) {
                continue;
            }

            DirEntry entry = currEntries.get(i);
            if (i == selectedEntry || (mouseX >= entryMinX && mouseX < entryMaxX && mouseY >= minEntryY && mouseY < maxEntryY)) {
                graphics.fill(entryMinX, minEntryY, entryMaxX, maxEntryY, 0xFFFFFFFF);
            }
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, entry.icon(), entryIconX, minEntryY + SMALL_PADDING, ICON_SIZE, ICON_SIZE);
            graphics.text(font, entry.name(), entryNameX, minEntryY + 2, 0xFF404040, false);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (getFocused() != null && !getFocused().isMouseOver(event.x(), event.y())) {
            setFocused(null);
        }

        int minX = leftPos + ENTRY_MIN_X;
        int maxX = minX + ENTRIES_WIDTH;
        int minY = topPos + ENTRY_MIN_Y;
        int maxY = minY + ENTRIES_HEIGHT;
        if (scrollbar.isVisible()) {
            maxX -= scrollbar.getWidth();
        }
        if (event.x() >= minX && event.x() < maxX && event.y() >= minY && event.y() < maxY) {
            int idx = (int) (event.y() - minY + scrollbar.getOffset()) / ENTRY_HEIGHT;
            if (idx >= 0 && idx < currEntries.size()) {
                if (doubleClick && selectedEntry == idx) {
                    switch (currEntries.get(idx)) {
                        case DirEntry.Directory dir -> setCurrentPath(dir.path, true);
                        case DirEntry.File ignored -> execute(execButton);
                    }
                } else {
                    selectedEntry = idx;
                    switch (currEntries.get(idx)) {
                        case DirEntry.Directory ignored -> {
                            if (type == Type.SAVE) {
                                execButton.setMessage(Type.OPEN.buttonTitle);
                            }
                        }
                        case DirEntry.File file -> {
                            fileNameEdit.setValue(file.name);
                            execButton.setMessage(type.buttonTitle);
                        }
                    }
                }
            }
            return true;
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled && getFocused() != null && dropFocusAfterClick.contains(getFocused())) {
            setFocused(null);
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (scrollbar.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (scrollbar.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollbar.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            cancel(cancelButton);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        int minX = leftPos + LIST_X;
        int maxX = minX + LIST_WIDTH;
        int minY = topPos + LIST_Y;
        int maxY = minY + LIST_HEIGHT;
        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    private void setCurrentPath(Path currPath, boolean addToHistory) {
        this.currPath = currPath;
        if (addToHistory) {
            pathHistory.add(currPath);
            historyIdx = pathHistory.size() - 1;
        }
        updateDirectoryEntries();
    }

    private void updateDirectoryEntries() {
        formattedPath = formatCurrentPath();
        listCurrentPath();
        execButton.setMessage(type.buttonTitle);
        selectedEntry = -1;

        actionButtonBack.active = canTraverseBack();
        actionButtonForward.active = canTraverseForward();
        actionButtonUp.active = canTraverseUp();
    }

    private boolean canTraverseBack() {
        return historyIdx > 0;
    }

    private boolean canTraverseForward() {
        return historyIdx < pathHistory.size() - 1;
    }

    private boolean canTraverseUp() {
        return currPath.startsWith(rootPath) && currPath.getNameCount() > rootPath.getNameCount();
    }

    private void back(Button btn) {
        if (canTraverseBack()) {
            historyIdx--;
            setCurrentPath(pathHistory.get(historyIdx), false);
        }
    }

    private void forward(Button btn) {
        if (canTraverseForward()) {
            historyIdx++;
            setCurrentPath(pathHistory.get(historyIdx), false);
        }
    }

    private void up(Button btn) {
        if (canTraverseUp()) {
            setCurrentPath(currPath.getParent(), true);
        }
    }

    private void reload(Button btn) {
        updateDirectoryEntries();
    }

    private void execute(Button btn) {
        if (selectedEntry > -1 && selectedEntry < currEntries.size() && currEntries.get(selectedEntry) instanceof DirEntry.Directory dir) {
            setCurrentPath(dir.path, false);
            return;
        }

        String fileName = fileNameEdit.getValue();
        if (fileName.isEmpty()) {
            return;
        }

        fileName = fileNameSuffix.applySuffix(fileName);
        Path filePath = currPath.resolve(fileName);
        if (type == Type.OPEN && !Files.exists(filePath)) {
            DialogScreen.builder(DialogScreen.Type.ERROR)
                    .withTitle(title)
                    .withMessage(Component.literal(fileName))
                    .withMessage(MSG_FILE_NOT_FOUND_LINE_ONE)
                    .withMessage(MSG_FILE_NOT_FOUND_LINE_TWO)
                    .show();
            return;
        } else if (type == Type.SAVE && Files.exists(filePath)) {
            DialogScreen.builder(DialogScreen.Type.CONFIRM)
                    .withTitle(Component.translatable(TITLE_SAVE_CONFIRM_OVERWRITE, typeDesc))
                    .withMessage(Component.translatable(MSG_SAVE_CONFIRM_OVERWRITE_LINE_ONE, fileName))
                    .withMessage(MSG_SAVE_CONFIRM_OVERWRITE_LINE_TWO)
                    .withOkCallback(() -> doExecute(filePath))
                    .show();
            return;
        }
        doExecute(filePath);
    }

    private void doExecute(Path filePath) {
        onClose();
        pathConsumer.accept(filePath);
    }

    private void cancel(Button btn) {
        onClose();
        pathConsumer.accept(null);
    }

    private void listCurrentPath() {
        currEntries.clear();
        dirEmpty = true;
        noMatchingFiles = false;

        try (Stream<Path> paths = Files.list(currPath)) {
            paths.forEach(path -> {
                dirEmpty = false;

                if (Files.isDirectory(path)) {
                    currEntries.add(new DirEntry.Directory(path, path.getFileName().toString()));
                } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    String fileName = path.getFileName().toString();
                    if (fileNameSuffix.filter(fileName)) {
                        currEntries.add(new DirEntry.File(path, fileName));
                    }
                }
            });
        } catch (IOException | UncheckedIOException ignored) { }

        noMatchingFiles = !dirEmpty && currEntries.isEmpty();

        currEntries.sort((e1, e2) -> {
            boolean dir1 = e1 instanceof DirEntry.Directory;
            boolean dir2 = e2 instanceof DirEntry.Directory;
            if (dir1 && !dir2) {
                return -1;
            }
            if (!dir1 && dir2) {
                return 1;
            }
            return e1.name().compareTo(e2.name());
        });
    }

    private FormattedPath formatCurrentPath() {
        Path path = rootPath.relativize(currPath);
        StringBuilder pathText = new StringBuilder();
        int runningWidth = 0;
        boolean first = true;
        for (int i = path.getNameCount() - 1; i >= 0; i--) {
            String partText = "> " + path.getName(i);
            if (!first) {
                partText += " ";
            }
            int partWidth = font.width(partText);
            if (runningWidth + partWidth > PATH_MAX_WIDTH) {
                break;
            }
            pathText.insert(0, partText);
            runningWidth += partWidth;
            first = false;
        }
        return new FormattedPath(pathText.toString().trim(), runningWidth);
    }

    private static WidgetSprites actionSprites(String type) {
        Identifier prefix = Utils.rl("filebrowser/button_" + type);
        return new WidgetSprites(prefix, prefix.withSuffix("_disabled"), prefix.withSuffix("_focused"));
    }

    private record FormattedPath(String text, int width) { }

    private sealed interface DirEntry {
        Path path();

        String name();

        Identifier icon();

        record Directory(Path path, String name) implements DirEntry {
            @Override
            public Identifier icon() {
                return ICON_DIRECTORY;
            }
        }

        record File(Path path, String name) implements DirEntry {
            @Override
            public Identifier icon() {
                return ICON_FILE;
            }
        }
    }

    public enum Type {
        OPEN(Utils.rl("filebrowser/icon_open")),
        SAVE(Utils.rl("filebrowser/icon_save")),
        ;

        private final String name = toString().toLowerCase(Locale.ROOT);
        private final String title = Utils.translationKey("title", "file_browser." + name);
        private final Component buttonTitle = Utils.translate("button", "file_browser." + name);
        private final Identifier icon;

        Type(Identifier icon) {
            this.icon = icon;
        }

        public String getTitle() {
            return title;
        }

        public Component getButtonTitle() {
            return buttonTitle;
        }
    }
}
