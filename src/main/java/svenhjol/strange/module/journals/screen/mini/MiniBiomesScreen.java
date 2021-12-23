package svenhjol.strange.module.journals.screen.mini;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import svenhjol.strange.module.journals.JournalsClient;
import svenhjol.strange.module.journals.paginator.BiomePaginator;
import svenhjol.strange.module.journals.screen.JournalScreen;
import svenhjol.strange.module.journals.screen.MiniJournal;
import svenhjol.strange.module.knowledge.KnowledgeClient;

public class MiniBiomesScreen extends BaseMiniScreen {
    private BiomePaginator paginator;

    public MiniBiomesScreen(MiniJournal mini) {
        super(mini);
    }

    @Override
    public void init() {
        super.init();

        if (mini.selectedBiome != null) {

            mini.addBackButton(b -> {
                mini.selectedBiome = null;
                mini.changeSection(MiniJournal.Section.BIOMES);
            });

        } else {

            var journal = JournalsClient.getJournal().orElse(null);
            if (journal == null) return;

            var biomes = journal.getLearnedBiomes();

            paginator = new BiomePaginator(biomes);
            setPaginatorDefaults(paginator);
            paginator.setButtonWidth(94);

            paginator.init(screen, mini.offset, midX - 87, midY - 78, biome -> {
                mini.selectedBiome = biome;
                mini.redraw();
            }, newOffset -> {
                mini.offset = newOffset;
                mini.redraw();
            });

            mini.addBackButton(b -> mini.changeSection(MiniJournal.Section.HOME));

        }
    }

    @Override
    public void render(PoseStack poseStack, ItemRenderer itemRenderer, Font font) {
        mini.renderTitle(poseStack, JournalScreen.LEARNED_BIOMES, midY - 94);

        if (mini.selectedBiome != null) {
            var biomes = KnowledgeClient.getBiomes().orElse(null);
            if (biomes == null) return;

            // Get the runes for the selected biome.
            var runes = biomes.get(mini.selectedBiome);
            if (runes == null) return;

            runeStringRenderer.render(poseStack, font, runes);

        } else {

            paginator.render(poseStack, itemRenderer, font);

        }
    }
}
