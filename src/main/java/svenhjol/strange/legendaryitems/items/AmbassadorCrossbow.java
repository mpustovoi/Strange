package svenhjol.strange.legendaryitems.items;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import svenhjol.strange.legendaryitems.ILegendaryEnchanted;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmbassadorCrossbow implements ILegendaryEnchanted {
    @Override
    public DyeColor getColor() {
        return DyeColor.BLACK;
    }

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        HashMap<Enchantment, Integer> map = new HashMap<>();
        map.put(Enchantments.QUICK_CHARGE, 5);
        map.put(Enchantments.MENDING, 1);
        map.put(Enchantments.UNBREAKING, 1);
        return map;
    }

    @Override
    public List<String> getValidEnchantments() {
        return null;
    }

    @Override
    public TranslatableText getName(ItemStack itemStack) {
        return new TranslatableText("item.strange.legendary.ambassador_crossbow");
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(Items.CROSSBOW);
    }

    @Override
    public int getMaxAdditionalLevels() {
        return 0;
    }
}
