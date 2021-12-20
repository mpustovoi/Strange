package svenhjol.strange.module.journals2.screen.mini;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import svenhjol.charm.helper.DimensionHelper;
import svenhjol.strange.module.bookmarks.Bookmark;
import svenhjol.strange.module.bookmarks.BookmarksClient;
import svenhjol.strange.module.journals2.paginator.BookmarkPaginator;
import svenhjol.strange.module.journals2.screen.JournalScreen;
import svenhjol.strange.module.journals2.screen.MiniJournal;

public class MiniBookmarksScreen extends BaseMiniScreen {
    public static final Component INCORRECT_DIMENSION;

    private BookmarkPaginator paginator;

    public MiniBookmarksScreen(MiniJournal mini) {
        super(mini);
    }

    @Override
    public void init() {
        super.init();

        if (mini.selectedBookmark != null) {
            // If the selected bookmark is not in the player's dimension then return early.
            if (!validDimension(mini.selectedBookmark)) {
                mini.selectedBookmark = null;
                return;
            }

            // TODO: render runes here

            mini.addBackButton(b -> {
                mini.selectedBookmark = null;
                mini.changeSection(MiniJournal.Section.BOOKMARKS);
            });

        } else {

            var branch = BookmarksClient.branch;
            if (branch == null) return;

            mini.addBackButton(b -> mini.changeSection(MiniJournal.Section.HOME));

            paginator = new BookmarkPaginator(branch.values(minecraft.player.getUUID()));
            paginator.setButtonWidth(88);
            paginator.setButtonHeight(20);
            paginator.setYControls(midY + 50);
            paginator.setDistBetweenPageButtons(23);
            paginator.setDistBetweenIconAndButton(5);

            // If the listed bookmark is not in the player's dimension then disable it and show a tooltip to this effect.
            paginator.setOnItemHovered(bookmark -> validDimension(bookmark) ? new TextComponent(bookmark.getName()) : INCORRECT_DIMENSION);
            paginator.setOnItemButtonRendered((bookmark, button) -> button.active = validDimension(bookmark));

            paginator.init(screen, mini.offset, midX - 87, midY - 78, bookmark -> {
                mini.selectedBookmark = bookmark;
                mini.redraw();
            }, newOffset -> {
                mini.offset = newOffset;
                mini.redraw();
            });

        }
    }

    @Override
    public void render(PoseStack poseStack, ItemRenderer itemRenderer, Font font) {
        mini.renderTitle(poseStack, JournalScreen.BOOKMARKS, midY - 94);

        if (mini.selectedBookmark == null) {
            paginator.render(poseStack, itemRenderer, font);
        }
    }

    private boolean validDimension(Bookmark bookmark) {
        return DimensionHelper.isDimension(minecraft.level, bookmark.getDimension());
    }

    static {
        INCORRECT_DIMENSION = new TranslatableComponent("gui.strange.journal.incorrect_dimension");
    }
}
