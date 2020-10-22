package svenhjol.strange.legendary.items;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import svenhjol.strange.iface.ILegendaryEnchanted;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EldritchBow implements ILegendaryEnchanted {

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        HashMap<Enchantment, Integer> map = new HashMap<>();
        map.put(Enchantments.INFINITY, 1);
        map.put(Enchantments.MENDING, 1);
        return map;
    }

    @Override
    public List<String> getValidEnchantments() {
        return null;
    }

    @Override
    public TranslatableText getName(ItemStack itemStack) {
        return new TranslatableText("item.strange.legendary.eldritch_bow");
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(Items.BOW);
    }

    @Override
    public int getMaxAdditionalLevels() {
        return 0;
    }
}
