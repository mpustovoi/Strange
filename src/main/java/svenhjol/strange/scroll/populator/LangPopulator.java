package svenhjol.strange.scroll.populator;

import net.minecraft.server.network.ServerPlayerEntity;
import svenhjol.strange.module.Scrolls;
import svenhjol.strange.scroll.JsonDefinition;
import svenhjol.strange.scroll.tag.QuestTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LangPopulator extends Populator {
    public static final String TITLE = "title";

    public LangPopulator(ServerPlayerEntity player, QuestTag quest, JsonDefinition definition) {
        super(player, quest, definition);
    }

    @Override
    public void populate() {
        Map<String, Map<String, String>> def = definition.getLang();
        if (def.isEmpty())
            return;

        if (def.containsKey(Scrolls.language)) {
            Map<String, String> strings = def.get(Scrolls.language);
            List<String> keys = new ArrayList<>(strings.keySet());

            if (keys.contains(TITLE))
                quest.setTitle(strings.get(TITLE));
        }
    }
}
