package svenhjol.strange.legendary.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import svenhjol.strange.iface.ILegendaryEnchanted;

import java.util.Arrays;
import java.util.List;

public class LegendaryBow implements ILegendaryEnchanted {
    @Override
    public List<String> getValidEnchantments() {
        return Arrays.asList(
            "minecraft:power",
            "minecraft:punch"
        );
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(Items.BOW);
    }

    @Override
    public int getMaxAdditionalLevels() {
        return 3;
    }
}
