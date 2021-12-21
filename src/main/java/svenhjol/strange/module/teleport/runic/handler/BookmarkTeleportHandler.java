package svenhjol.strange.module.teleport.runic.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import svenhjol.strange.module.bookmarks.Bookmark;
import svenhjol.strange.module.runes.RuneBranch;

public class BookmarkTeleportHandler extends BaseTeleportHandler<Bookmark> {
    public BookmarkTeleportHandler(RuneBranch<?, Bookmark> branch) {
        super(branch);
    }

    @Override
    public boolean process() {
        BlockPos target = value.getBlockPos();
        ResourceLocation dimension  = value.getDimension();
        return teleport(dimension, target, true, false);
    }
}
