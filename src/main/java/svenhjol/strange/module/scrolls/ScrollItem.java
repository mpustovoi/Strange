package svenhjol.strange.module.scrolls;

import svenhjol.charm.helper.PlayerHelper;
import svenhjol.charm.item.CharmItem;
import svenhjol.charm.module.CharmModule;
import svenhjol.strange.helper.NetworkHelper;
import svenhjol.strange.module.scrolls.tag.Quest;

import javax.annotation.Nullable;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import java.util.Optional;
import java.util.UUID;

public class ScrollItem extends CharmItem {
    private static final String QUEST_TAG = "quest";
    private static final String RARITY_TAG = "rarity";
    private static final String MERCHANT_TAG = "merchant";
    private static final String OWNER_TAG = "owner";

    private final int tier;

    public ScrollItem(CharmModule module, int tier, String name) {
        super(module, name, new Item.Properties()
            .tab(CreativeModeTab.TAB_MISC)
            .rarity(Rarity.UNCOMMON)
            .stacksTo(1)
        );

        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (world.isClientSide)
            return new InteractionResultHolder<>(InteractionResult.PASS, held);

        Optional<QuestManager> optionalQuestManager = Scrolls.getQuestManager();
        if (!optionalQuestManager.isPresent())
            return new InteractionResultHolder<>(InteractionResult.FAIL, held);

        QuestManager questManager = optionalQuestManager.get();

        player.getCooldowns().addCooldown(this, 10);
        boolean hasBeenOpened = hasBeenOpened(held);

        // if the quest hasn't been populated yet, create it in the quest manager
        if (!hasBeenOpened) {
            ScrollDefinition definition = Scrolls.getRandomDefinition(tier, world, world.random);
            ServerPlayer serverPlayer = (ServerPlayer)player;

            if (definition == null)
                return destroyScroll(world, player, held);

            if (player.isShiftKeyDown())
                return new InteractionResultHolder<>(InteractionResult.FAIL, held);

            if (!questManager.checkPlayerCanStartQuest(serverPlayer))
                return new InteractionResultHolder<>(InteractionResult.FAIL, held);

            UUID seller = getScrollMerchant(held);
            int rarity = Math.min(1, getScrollRarity(held));

            // if quest fails to generate then destroy it here
            Quest quest = questManager.createQuest(serverPlayer, definition, rarity, seller);
            if (quest == null) {

                // if the scroll was purchased, refund the emeralds with stack count equal to the tier
                if (!seller.equals(ScrollsHelper.ANY_UUID))
                    PlayerHelper.addOrDropStack(player, new ItemStack(Items.EMERALD, tier));

                return destroyScroll(world, player, held);
            }

            questManager.sendToast((ServerPlayer) player, quest, QuestToastType.General, "event.strange.quests.accepted");
            setScrollQuest(held, quest);
            setScrollOwner(held, player);


            // tell the client to open the scroll
            Scrolls.sendPlayerOpenScrollPacket(serverPlayer, quest);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, held);

        } else {

            String questId = getScrollQuest(held);
            if (questId == null)
                return destroyScroll(world, player, held);

            // check if it belongs to current player
            UUID owner = getScrollOwner(held);
            if (owner != null && !player.getUUID().equals(owner) && !player.isCreative()) {
                player.displayClientMessage(new TranslatableComponent("gui.strange.scrolls.wrong_owner"), true);
                return InteractionResultHolder.fail(held);
            }

            // try and open the quest, or destroy it if it's no longer valid
            Optional<Quest> optionalQuest = questManager.getQuest(questId);

            if (optionalQuest.isPresent()) {

                // if there's no owner, then set it to current player
                if (owner == null)
                    claimOwnership(held, player);

                Scrolls.sendPlayerOpenScrollPacket((ServerPlayer)player, optionalQuest.get());
            } else {
                // scroll has expired, remove it
                return destroyScroll(world, player, held);
            }
        }

        return super.use(world, player, hand);
    }

    public static boolean hasBeenOpened(ItemStack scroll) {
        return scroll.getOrCreateTag().contains(QUEST_TAG);
    }

    public static void claimOwnership(ItemStack scroll, Player player) {
        player.displayClientMessage(new TranslatableComponent("gui.strange.scrolls.claim_ownership"), true);
        String questId = ScrollItem.getScrollQuest(scroll);

        Scrolls.getQuestManager().flatMap(manager
            -> manager.getQuest(questId)).ifPresent(quest
                -> quest.setOwner(player.getUUID()));

        ScrollItem.setScrollOwner(scroll, player);
    }

    public static void giveScrollToPlayer(Quest quest, Player player) {
        // allows quest objectives to update themselves
        quest.update(player);

        // create a new scroll for this quest and give it to the player
        ItemStack scroll = new ItemStack(Scrolls.SCROLL_TIERS.get(quest.getTier()));
        quest.setOwner(player.getUUID());
        ScrollItem.setScrollQuest(scroll, quest);
        PlayerHelper.addOrDropStack(player, scroll);
    }

    public static UUID getScrollMerchant(ItemStack scroll) {
        String string = scroll.getOrCreateTag().getString(MERCHANT_TAG);
        if (string.isEmpty())
            return ScrollsHelper.ANY_UUID;

        return UUID.fromString(string);
    }

    public static String getScrollQuest(ItemStack scroll) {
        return scroll.getOrCreateTag().getString(QUEST_TAG);
    }

    @Nullable
    public static UUID getScrollOwner(ItemStack scroll) {
        String string = scroll.getOrCreateTag().getString(OWNER_TAG);
        if (string.isEmpty())
            return null;

        return UUID.fromString(string);
    }

    public static int getScrollRarity(ItemStack scroll) {
        return scroll.getOrCreateTag().getInt(RARITY_TAG);
    }

    public static void setScrollMerchant(ItemStack scroll, AbstractVillager merchant) {
        scroll.getOrCreateTag().putString(MERCHANT_TAG, merchant.getStringUUID());
    }

    public static void setScrollQuest(ItemStack scroll, Quest quest) {
        scroll.getOrCreateTag().putString(QUEST_TAG, quest.getId());
        setScrollName(scroll, quest);
    }

    public static void setScrollName(ItemStack scroll, Quest quest) {
        scroll.setHoverName(new TranslatableComponent(quest.getTitle()));
    }

    public static void setScrollOwner(ItemStack scroll, Player player) {
        scroll.getOrCreateTag().putString(OWNER_TAG, player.getStringUUID());
    }

    public static void setScrollRarity(ItemStack scroll, int rarity) {
        scroll.getOrCreateTag().putInt(RARITY_TAG, rarity);
    }

    public InteractionResultHolder<ItemStack> destroyScroll(Level world, Player player, ItemStack scroll) {
        world.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);
        scroll.shrink(1);

        NetworkHelper.sendEmptyPacketToClient((ServerPlayer)player, Scrolls.MSG_CLIENT_DESTROY_SCROLL);
        return new InteractionResultHolder<>(InteractionResult.FAIL, scroll);
    }
}
