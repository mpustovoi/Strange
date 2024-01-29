package svenhjol.strange.feature.quests.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.npc.VillagerProfession;
import svenhjol.strange.feature.quests.Quest;
import svenhjol.strange.feature.quests.QuestsHelper;
import svenhjol.strange.feature.quests.Quests;
import svenhjol.strange.feature.quests.QuestsNetwork.AcceptQuest;
import svenhjol.strange.helper.GuiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestOffersScreen extends Screen {
    protected UUID villagerUuid;
    protected VillagerProfession villagerProfession;
    protected int villagerLevel;
    protected List<BaseQuestRenderer<?>> renderers = new ArrayList<>();
    protected int midX;

    public QuestOffersScreen(UUID villagerUuid, VillagerProfession villagerProfession, int villagerLevel) {
        super(QuestsHelper.makeVillagerOffersTitle(villagerProfession));
        this.villagerUuid = villagerUuid;
        this.villagerProfession = villagerProfession;
        this.villagerLevel = villagerLevel;

        var quests = Quests.getVillagerQuests(villagerUuid);
        for (Quest quest : quests.all()) {
            var renderer = quest.type().makeRenderer(quest);
            renderers.add(renderer);
        }
    }

    @Override
    protected void init() {
        super.init();
        midX = width / 2;

        for (var renderer : renderers) {
            var quest = renderer.quest();
            renderer.setAcceptAction(b -> AcceptQuest.send(quest.villagerUuid(), quest.id()));
            renderer.initPagedOffer(this);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTitle(guiGraphics, midX, 10);

        var yOffset = 30;
        for (int i = 0; i < renderers.size(); i++) {
            var renderer = renderers.get(i);
            renderer.renderPagedOffer(guiGraphics, yOffset, mouseX, mouseY);
            yOffset += renderer.getPagedOfferHeight();
        }
    }

    protected void renderTitle(GuiGraphics guiGraphics, int x, int y) {
        GuiHelper.drawCenteredString(guiGraphics, font, getTitle(), x, y, 0xa05f50, false);
    }
}
