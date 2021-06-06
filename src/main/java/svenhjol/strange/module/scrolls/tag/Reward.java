package svenhjol.strange.module.scrolls.tag;

import svenhjol.charm.helper.PlayerHelper;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class Reward implements ISerializable {
    public static final String ITEM_DATA = "item_data";
    public static final String ITEM_COUNT = "item_count";
    public static final String PLAYER_XP_TAG = "player_xp";
    public static final String VILLAGER_XP_TAG = "villager_xp";

    private int playerXp;
    private int villagerXp;
    private final Quest quest;
    private Map<ItemStack, Integer> items = new HashMap<>();

    public Reward(Quest quest) {
        this.quest = quest;
    }

    public void complete(Player player, @Nullable AbstractVillager merchant) {
        for (ItemStack stack : items.keySet()) {
            int count = items.get(stack);
            ItemStack stackToDrop = stack.copy();
            stackToDrop.setCount(count);

            if (merchant != null) {
                BehaviorUtils.throwItem(merchant, stackToDrop, player.position());
            } else {
                PlayerHelper.addOrDropStack(player, stackToDrop);
            }
        }

        if (playerXp > 0)
            player.giveExperienceLevels(playerXp);

        // TODO: handle merchant XP
    }

    public CompoundTag toTag() {
        CompoundTag outTag = new CompoundTag();
        CompoundTag dataTag = new CompoundTag();
        CompoundTag countTag = new CompoundTag();

        if (!items.isEmpty()) {
            int index = 0;
            for (ItemStack stack : items.keySet()) {
                String stackIndex = Integer.toString(index);

                CompoundTag itemTag = new CompoundTag();
                stack.save(itemTag);
                dataTag.put(stackIndex, itemTag);
                countTag.putInt(stackIndex, items.get(stack));
                index++;
            }
        }

        outTag.put(ITEM_DATA, dataTag);
        outTag.put(ITEM_COUNT, countTag);
        outTag.putInt(PLAYER_XP_TAG, playerXp);
        outTag.putInt(VILLAGER_XP_TAG, villagerXp);
        return outTag;
    }

    public void fromTag(CompoundTag tag) {
        this.playerXp = tag.getInt(PLAYER_XP_TAG);
        this.villagerXp = tag.getInt(VILLAGER_XP_TAG);
        CompoundTag dataTag = (CompoundTag)tag.get(ITEM_DATA);
        CompoundTag countTag = (CompoundTag)tag.get(ITEM_COUNT);

        this.items = new HashMap<>();

        if (dataTag != null && dataTag.size() > 0 && countTag != null) {
            for (int i = 0; i < dataTag.size(); i++) {
                String stackIndex = String.valueOf(i);
                Tag tagAtIndex = dataTag.get(stackIndex);

                if (tagAtIndex == null)
                    continue;

                ItemStack stack = ItemStack.of((CompoundTag)tagAtIndex);
                int count = Math.max(countTag.getInt(stackIndex), 1);
                items.put(stack, count);
            }
        }
    }

    public void addItem(ItemStack stack) {
        this.items.put(stack, stack.getCount());
    }

    public Map<ItemStack, Integer> getItems() {
        return items;
    }

    public int getPlayerXp() {
        return playerXp;
    }

    public int getVillagerXp() {
        return villagerXp;
    }

    public void setPlayerXp(int count) {
        this.playerXp = count;
    }

    public void setVillagerXp(int count) {
        this.villagerXp = count;
    }
}
