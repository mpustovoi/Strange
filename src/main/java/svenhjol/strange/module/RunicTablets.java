package svenhjol.strange.module;

import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.FabricLootSupplierBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.item.Items;
import net.minecraft.loot.ConstantLootTableRange;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.handler.ModuleHandler;
import svenhjol.charm.base.handler.RegistryHandler;
import svenhjol.charm.base.helper.DecorationHelper;
import svenhjol.charm.base.helper.LootHelper;
import svenhjol.charm.base.iface.Module;
import svenhjol.strange.Strange;
import svenhjol.strange.base.StrangeLoot;
import svenhjol.strange.item.RunicTabletItem;
import svenhjol.strange.loot.RunicTabletLootFunction;

@Module(mod = Strange.MOD_ID)
public class RunicTablets extends CharmModule {
    public static final Identifier RUNIC_TABLET_LOOT_ID = new Identifier(Strange.MOD_ID, "runic_tablet_loot");
    public static LootFunctionType RUNIC_TABLET_LOOT_FUNCTION;

    public static RunicTabletItem RUNIC_TABLET;

    public static boolean addRunicTabletsToLoot = true;

    @Override
    public void register() {
        RUNIC_TABLET = new RunicTabletItem(this, "runic_tablet");
        RUNIC_TABLET_LOOT_FUNCTION = RegistryHandler.lootFunctionType(RUNIC_TABLET_LOOT_ID, new LootFunctionType(new RunicTabletLootFunction.Serializer()));
    }

    @Override
    public void init() {
        LootTableLoadingCallback.EVENT.register(this::handleLootTables);

        LootHelper.CUSTOM_LOOT_TABLES.add(StrangeLoot.TABLET);
        DecorationHelper.RARE_CHEST_LOOT_TABLES.add(StrangeLoot.TABLET);
    }

    private void handleLootTables(ResourceManager resourceManager, LootManager lootManager, Identifier id, FabricLootSupplierBuilder supplier, LootTableLoadingCallback.LootTableSetter setter) {
        if (!ModuleHandler.enabled("strange:excavation"))
            return;

        if (addRunicTabletsToLoot) {
            if (id.equals(StrangeLoot.TABLET)) {
                FabricLootPoolBuilder builder = FabricLootPoolBuilder.builder()
                    .rolls(ConstantLootTableRange.create(1))
                    .with(ItemEntry.builder(Items.AIR)
                        .weight(20)
                        .apply(() -> new RunicTabletLootFunction(new LootCondition[0])));

                supplier.pool(builder);
            }
        }
    }
}
