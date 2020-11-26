package svenhjol.strange.scrolls.tag;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import svenhjol.charm.base.helper.PosHelper;
import svenhjol.strange.scrolls.populator.BossPopulator;

import java.util.*;

public class Boss implements ISerializable {
    public static final String TARGET_ENTITIES = "target_entities";
    public static final String TARGET_COUNT = "target_count";
    public static final String TARGET_KILLED = "target_killed";
    public static final String SPAWNED = "spawned";
    public static final String STRUCTURE = "structure";
    public static final String DIMENSION = "dimension";

    private Quest quest;
    private BlockPos structure;
    private Identifier dimension;
    private boolean spawned;

    private Map<Identifier, Integer> entities = new HashMap<>();
    private Map<Identifier, Integer> killed = new HashMap<>();

    // these are dynamically generated, not stored in nbt
    private Map<Identifier, Boolean> satisfied = new HashMap<>();
    private Map<Identifier, String> names = new HashMap<>();

    public Boss(Quest quest) {
        this.quest = quest;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag outTag = new CompoundTag();
        CompoundTag entitiesTag = new CompoundTag();
        CompoundTag countTag = new CompoundTag();
        CompoundTag killedTag = new CompoundTag();

        // write boss entity data
        if (!entities.isEmpty()) {
            int index = 0;

            for (Identifier id : entities.keySet()) {
                String tagIndex = Integer.toString(index);
                int entityCount = entities.get(id);
                int entityKilled = killed.getOrDefault(id, 0);

                entitiesTag.putString(tagIndex, id.toString());
                countTag.putInt(tagIndex, entityCount);
                killedTag.putInt(tagIndex, entityKilled);
            }
            outTag.put(TARGET_ENTITIES, entitiesTag);
            outTag.put(TARGET_COUNT, countTag);
            outTag.put(TARGET_KILLED, killedTag);
        }

        outTag.putBoolean(SPAWNED, spawned);

        if (dimension != null)
            outTag.putString(DIMENSION, dimension.toString());

        if (structure != null)
            outTag.putLong(STRUCTURE, structure.asLong());

        return outTag;
    }

    @Override
    public void fromTag(CompoundTag fromTag) {
        CompoundTag entitiesTag = (CompoundTag)fromTag.get(TARGET_ENTITIES);
        CompoundTag countTag = (CompoundTag)fromTag.get(TARGET_COUNT);
        CompoundTag killedTag = (CompoundTag)fromTag.get(TARGET_KILLED);

        entities = new HashMap<>();

        structure = fromTag.contains(STRUCTURE) ? BlockPos.fromLong(fromTag.getLong(STRUCTURE)) : null;
        dimension = Identifier.tryParse(fromTag.getString(DIMENSION));
        spawned = fromTag.contains(SPAWNED) && fromTag.getBoolean(SPAWNED);

        if (dimension == null)
            dimension = new Identifier("minecraft", "overworld");

        if (entitiesTag != null && entitiesTag.getSize() > 0 && countTag != null) {
            for (int i = 0; i < entitiesTag.getSize(); i++) {
                // read data from the tags at specified index
                String tagIndex = String.valueOf(i);
                Identifier id = Identifier.tryParse(entitiesTag.getString(tagIndex));
                if (id == null)
                    continue;

                int entityCount = countTag.getInt(tagIndex);
                int entityKilled = killedTag != null ? killedTag.getInt(tagIndex) : 0;
                entities.put(id, entityCount);
                killed.put(id, entityKilled);
            }
        }
    }

    public void addTarget(Identifier entity, int count) {
        entities.put(entity, count);
    }

    public void setDimension(Identifier dimension) {
        this.dimension = dimension;
    }

    public void setStructure(BlockPos structure) {
        this.structure = structure;
    }

    public Quest getQuest() {
        return quest;
    }

    public Map<Identifier, Integer> getEntities() {
        return entities;
    }

    public Map<Identifier, Integer> getKilled() {
        return killed;
    }

    public Map<Identifier, String> getNames() {
        return names;
    }

    public Map<Identifier, Boolean> getSatisfied() {
        return satisfied;
    }

    public BlockPos getStructure() {
        return structure;
    }

    public boolean isSatisfied() {
        if (entities.isEmpty())
            return true;

        return entities.size() == satisfied.size() && getSatisfied().values().stream().allMatch(r -> r);
    }

    public void forceKill(MobEntity entity) {
        Identifier id = Registry.ENTITY_TYPE.getId(entity.getType());
        Integer count = killed.getOrDefault(id, 0);
        killed.put(id, count + 1);
        quest.setDirty(true);
    }

    public void playerTick(PlayerEntity player) {
        if (player.world.isClient || spawned || structure == null)
            return;

        double dist = PosHelper.getDistanceSquared(player.getBlockPos(), structure);
        if (dist < 260) {
            boolean result = BossPopulator.startEncounter(player, this);

            if (!result) {
                this.abandon(player);
                return;
            }

            quest.setDirty(true);
            spawned = true;
        }
    }

    public void complete(PlayerEntity player, MerchantEntity merchant) {
        BossPopulator.checkEncounter(player, this);
    }

    public void abandon(PlayerEntity player) {
        entities.clear();
        BossPopulator.checkEncounter(player, this);
    }

    public void update(PlayerEntity player) {
        satisfied.clear();

        entities.forEach((id, count) -> {
            int countKilled = killed.getOrDefault(id, 0);
            satisfied.put(id, countKilled >= count);

            Optional<EntityType<?>> optionalEntityType = Registry.ENTITY_TYPE.getOrEmpty(id);
            optionalEntityType.ifPresent(entityType -> names.put(id, entityType.getName().getString()));
        });
    }

    public void playerKilledEntity(PlayerEntity player, LivingEntity entity) {
        List<String> tags = new ArrayList<>(entity.getScoreboardTags());
        Identifier id = Registry.ENTITY_TYPE.getId(entity.getType());

        if (tags.contains(quest.getId())) {
            Integer count = killed.getOrDefault(id, 0);
            killed.put(id, count + 1);

            quest.setDirty(true);
            update(player);
        }

        BossPopulator.checkEncounter(player, this);
    }
}
