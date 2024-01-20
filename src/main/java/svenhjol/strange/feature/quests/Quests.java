package svenhjol.strange.feature.quests;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import svenhjol.charmony.api.event.EntityJoinEvent;
import svenhjol.charmony.api.event.PlayerTickEvent;
import svenhjol.charmony.api.event.ServerStartEvent;
import svenhjol.charmony.base.Mods;
import svenhjol.charmony.common.CommonFeature;
import svenhjol.strange.Strange;
import svenhjol.strange.event.QuestEvents;
import svenhjol.strange.feature.quests.QuestsNetwork.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Quests extends CommonFeature {
    static final List<QuestDefinition> DEFINITIONS = new ArrayList<>();
    public static final Map<UUID, List<Quest<?>>> PLAYER_QUESTS = new HashMap<>();
    public static final Map<UUID, List<Quest<?>>> VILLAGER_QUESTS = new HashMap<>();
    public static final Map<UUID, Long> VILLAGER_QUESTS_REFRESH = new HashMap<>();
    public static final Map<UUID, Integer> VILLAGER_LOYALTY = new HashMap<>();
    static Supplier<SoundEvent> abandonSound;
    static Supplier<SoundEvent> acceptSound;
    static Supplier<SoundEvent> completeSound;

    public static int maxPlayerQuests = 3;
    public static int maxVillagerQuests = 3;

    @Override
    public void register() {
        var registry = mod().registry();

        QuestDefinitions.init();
        QuestsNetwork.register(registry);

        abandonSound = registry.soundEvent("quest_abandon");
        acceptSound = registry.soundEvent("quest_accept");
        completeSound = registry.soundEvent("quest_complete");
    }

    @Override
    public void runWhenEnabled() {
        ServerStartEvent.INSTANCE.handle(this::handleServerStart);
        EntityJoinEvent.INSTANCE.handle(this::handleEntityJoin);
        PlayerTickEvent.INSTANCE.handle(this::handlePlayerTick);
    }

    private void handlePlayerTick(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var uuid = serverPlayer.getUUID();
            if (PLAYER_QUESTS.containsKey(uuid)) {
                for (Quest<?> quest : PLAYER_QUESTS.get(uuid)) {
                    quest.tick(serverPlayer);
                }
            }
        }
    }

    public static void registerDefinition(QuestDefinition definition) {
        Mods.common(Strange.ID).log().debug(Quests.class, "Registering definition " + definition);
        DEFINITIONS.add(definition);
    }

    public static void syncQuests(ServerPlayer player) {
        SyncPlayerQuests.send(player, getQuests(player));
    }

    public static void addQuest(ServerPlayer player, Quest<?> quest) {
        PLAYER_QUESTS.computeIfAbsent(player.getUUID(), a -> new ArrayList<>())
            .add(quest);

        quest.player = player;
    }

    public static void increaseLoyalty(UUID villagerUuid, int amount) {
        var current = getLoyalty(villagerUuid);
        VILLAGER_LOYALTY.put(villagerUuid, current + amount);
    }

    public static void resetLoyalty(UUID villagerUuid) {
        VILLAGER_LOYALTY.put(villagerUuid, 0);
    }

    public static int getLoyalty(UUID villagerUuid) {
        return VILLAGER_LOYALTY.getOrDefault(villagerUuid, 0);
    }

    public static void removeQuest(ServerPlayer player, Quest<?> quest) {
        PLAYER_QUESTS.computeIfAbsent(player.getUUID(), a -> new ArrayList<>())
            .remove(quest);
    }

    public static List<Quest<?>> getQuests(Player player) {
        return PLAYER_QUESTS.getOrDefault(player.getUUID(), List.of());
    }

    public static Optional<Quest<?>> getQuest(Player player, String questId) {
        return getQuests(player)
            .stream().filter(q -> q.id().equals(questId))
            .findFirst();
    }

    private void handleServerStart(MinecraftServer server) {
        PLAYER_QUESTS.clear();
    }

    private void handleEntityJoin(Entity entity, Level level) {
        if (entity instanceof ServerPlayer player) {
            syncQuests(player);
        }
    }

    public static void handleRequestVillagerQuests(RequestVillagerQuests message, Player player) {
        var ticksToRefresh = 80; // TODO: test value, refresh quests every 4 seconds
        var level = player.level();
        var random = level.getRandom();
        var gameTime = level.getGameTime();
        var villagerUuid = message.getVillagerUuid();
        var serverPlayer = (ServerPlayer)player;

        // Is villager nearby?
        var nearby = QuestHelper.getNearbyMatchingVillager(level, player.blockPosition(), villagerUuid);
        if (nearby.isEmpty()) {
            NotifyVillagerQuestsResult.send(serverPlayer, VillagerQuestsResult.NO_VILLAGER);
            return;
        }

        var villager = nearby.get();
        var villagerData = villager.getVillagerData();
        var villagerProfession = villagerData.getProfession();
        var villagerLevel = villagerData.getLevel();
        var lastRefresh = VILLAGER_QUESTS_REFRESH.get(villagerUuid);
        var quests = VILLAGER_QUESTS.getOrDefault(villagerUuid, new ArrayList<>());

        if (lastRefresh != null && gameTime - lastRefresh < ticksToRefresh) {
            NotifyVillagerQuestsResult.send(serverPlayer, VillagerQuestsResult.SUCCESS);
            SyncVillagerQuests.send(serverPlayer, quests, villagerUuid, villagerProfession);
            return;
        }

        // Generate new quests for this villager
        quests.clear();

        var definitions = QuestHelper.makeDefinitions(villagerUuid, villagerProfession, 1, villagerLevel, maxVillagerQuests, random);
        if (definitions.isEmpty()) {
            NotifyVillagerQuestsResult.send(serverPlayer, VillagerQuestsResult.NO_QUESTS_GENERATED);
            return;
        }

        var newQuests = QuestHelper.makeQuestsFromDefinitions(definitions, villagerUuid);

        VILLAGER_QUESTS.put(villagerUuid, newQuests);
        VILLAGER_QUESTS_REFRESH.put(villagerUuid, gameTime);

        NotifyVillagerQuestsResult.send(serverPlayer, VillagerQuestsResult.SUCCESS);
        SyncVillagerQuests.send(serverPlayer, newQuests, villagerUuid, villagerProfession);
    }

    public static void handleRequestPlayerQuests(RequestPlayerQuests message, Player player) {
        syncQuests((ServerPlayer)player);
    }

    public static void handleAcceptQuest(AcceptQuest message, Player player) {
        var level = player.level();
        var questId = message.getQuestId();
        var villagerUuid = message.getVillagerUuid();
        var serverPlayer = (ServerPlayer)player;

        // Player at max quests?
        if (QuestHelper.hasMaxQuests(player)) {
            NotifyAcceptQuestResult.send(serverPlayer, null, AcceptQuestResult.MAX_QUESTS);
            return;
        }

        // Player already on this quest?
        if (getQuest(serverPlayer, questId).isPresent()) {
            NotifyAcceptQuestResult.send(serverPlayer, null, AcceptQuestResult.ALREADY_ON_QUEST);
            return;
        }

        // Is villager nearby?
        var nearby = QuestHelper.getNearbyMatchingVillager(level, player.blockPosition(), villagerUuid);
        if (nearby.isEmpty()) {
            NotifyAcceptQuestResult.send(serverPlayer, null, AcceptQuestResult.NO_VILLAGER);
            return;
        }

        // Check that the villager has quests.
        var villagerQuests = VILLAGER_QUESTS.get(villagerUuid);
        if (villagerQuests == null || villagerQuests.isEmpty()) {
            NotifyAcceptQuestResult.send(serverPlayer, null, AcceptQuestResult.VILLAGER_HAS_NO_QUESTS);
            return;
        }

        // Check that the villager quest ID is valid.
        var opt = villagerQuests.stream().filter(q -> q.id().equals(questId)).findFirst();
        if (opt.isEmpty()) {
            NotifyAcceptQuestResult.send(serverPlayer, null, AcceptQuestResult.NO_QUEST);
            return;
        }

        // Remove this quest from the villager quests.
        VILLAGER_QUESTS.put(villagerUuid, villagerQuests.stream().filter(q -> !q.id().equals(questId))
            .collect(Collectors.toCollection(ArrayList::new)));

        level.playSound(null, serverPlayer.blockPosition(), acceptSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        // Add this quest to the player's quests.
        var quest = opt.get();
        quest.start();

        addQuest(serverPlayer, quest);
        syncQuests(serverPlayer);

        // Fire the AcceptQuestEvent on the server side.
        QuestEvents.ACCEPT_QUEST.invoke(player, quest);

        // Update the client with the result.
        NotifyAcceptQuestResult.send(serverPlayer, quest, AcceptQuestResult.SUCCESS);
    }

    public static void handleAbandonQuest(AbandonQuest message, Player player) {
        var serverPlayer = (ServerPlayer)player;
        var quests = getQuests(player);
        var questId = message.getQuestId();

        // Player even has quests?
        if (quests.isEmpty()) {
            NotifyAbandonQuestResult.send(serverPlayer, null, AbandonQuestResult.NO_QUESTS);
            return;
        }

        // Player has this quest?
        var opt = quests.stream().filter(q -> q.id.equals(questId)).findFirst();
        if (opt.isEmpty()) {
            NotifyAbandonQuestResult.send(serverPlayer, null, AbandonQuestResult.NO_QUEST);
            return;
        }

        var quest = opt.get();
        quest.cancel();

        serverPlayer.level().playSound(null, serverPlayer.blockPosition(), abandonSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        // Fire the AbandonQuestEvent on the server side.
        QuestEvents.ABANDON_QUEST.invoke(player, quest);

        // Remove this quest from the player's quests.
        removeQuest(serverPlayer, quest);

        // Update the client with the result.
        NotifyAbandonQuestResult.send(serverPlayer, quest, AbandonQuestResult.SUCCESS);
    }

    /**
     * Called by mixin when a player interacts with a villager.
     * If any player quests can be completed by the villager then run that action here.
     */
    public static boolean tryComplete(ServerPlayer player, Villager villager) {
        var quests = getQuests(player);
        var satisfied = quests.stream().filter(Quest::satisfied).toList();
        if (satisfied.isEmpty()) return false;

        var matchesQuestGiver = satisfied.stream().filter(q -> q.villagerUuid().equals(villager.getUUID())).toList();
        if (!matchesQuestGiver.isEmpty()) {
            for (Quest<?> quest : matchesQuestGiver) {
                completeWithVillager(player, villager, quest);
            }
            return true;
        }

        var matchesProfession = satisfied.stream().filter(q -> q.villagerProfession().equals(villager.getVillagerData().getProfession())).toList();
        if (!matchesProfession.isEmpty()) {
            for (Quest<?> quest : matchesProfession) {
                quest.villagerUuid = villager.getUUID();
                completeWithVillager(player, villager, quest);
            }
            return true;
        }

        return false;
    }

    private static void completeWithVillager(ServerPlayer player, Villager villager, Quest<?> quest) {
        var level = (ServerLevel)player.level();
        var pos = player.blockPosition();
        var villagerUuid = villager.getUUID();

        level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, pos, completeSound.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (quest.isEpic()) {
            resetLoyalty(villagerUuid);
        }

        quest.complete();
        removeQuest(player, quest);
        syncQuests(player);
        increaseLoyalty(villagerUuid, 1);
    }

    public enum VillagerQuestsResult {
        NO_VILLAGER,
        NO_QUESTS_GENERATED,
        EMPTY,
        SUCCESS
    }

    public enum AcceptQuestResult {
        NO_VILLAGER,
        NO_QUEST,
        MAX_QUESTS,
        ALREADY_ON_QUEST,
        VILLAGER_HAS_NO_QUESTS,
        SUCCESS
    }

    public enum AbandonQuestResult {
        NO_QUESTS,
        NO_QUEST,
        SUCCESS
    }
}
