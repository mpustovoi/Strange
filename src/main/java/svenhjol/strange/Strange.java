package svenhjol.strange;

import net.minecraft.resources.ResourceLocation;
import svenhjol.charmony.base.Mods;
import svenhjol.charmony.common.CommonFeature;
import svenhjol.charmony.common.CommonMod;
import svenhjol.strange.feature.ambient_music_discs.AmbientMusicDiscs;
import svenhjol.strange.feature.bookmarks.Bookmarks;
import svenhjol.strange.feature.casks.Casks;
import svenhjol.strange.feature.cooking_pots.CookingPots;
import svenhjol.strange.feature.core.Core;
import svenhjol.strange.feature.ebony_wood.EbonyWood;
import svenhjol.strange.feature.learned_runes.LearnedRunes;
import svenhjol.strange.feature.piglin_pointing.PiglinPointing;
import svenhjol.strange.feature.quests.Quests;
import svenhjol.strange.feature.raid_horns.RaidHorns;
import svenhjol.strange.feature.respawn_anchors_work_everywhere.RespawnAnchorsWorkEverywhere;
import svenhjol.strange.feature.runestones.Runestones;
import svenhjol.strange.feature.stone_circles.StoneCircles;
import svenhjol.strange.feature.travel_journal.TravelJournal;
import svenhjol.strange.feature.waypoints.Waypoints;

import java.util.List;

public class Strange extends CommonMod {
    public static final String ID = "strange";
    public static final String CHARM_ID = "charm";
    public static final String CHARMONY_ID = "charmony";

    @Override
    public String modId() {
        return ID;
    }

    @Override
    public List<Class<? extends CommonFeature>> features() {
        return List.of(
            AmbientMusicDiscs.class,
            Bookmarks.class,
            Casks.class,
            CookingPots.class,
            Core.class,
            EbonyWood.class,
            LearnedRunes.class,
            PiglinPointing.class,
            Quests.class,
            RaidHorns.class,
            RespawnAnchorsWorkEverywhere.class,
            Runestones.class,
            StoneCircles.class,
            TravelJournal.class,
            Waypoints.class
        );
    }

    public static boolean isFeatureEnabled(ResourceLocation feature) {
        return Mods.common(ID).loader().isEnabled(feature);
    }
}
