package svenhjol.strange.foundations;

import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.handler.RegistryHandler;
import svenhjol.charm.base.helper.BiomeHelper;
import svenhjol.charm.base.helper.PosHelper;
import svenhjol.charm.base.iface.Module;
import svenhjol.charm.module.PlayerState;
import svenhjol.strange.Strange;
import svenhjol.strange.foundations.builds.StoneRoomFoundation;

import static svenhjol.charm.base.helper.BiomeHelper.addStructureFeatureToBiomes;

@Module(mod = Strange.MOD_ID)
public class Foundations extends CharmModule {
    public static final Identifier FOUNDATION_ID = new Identifier(Strange.MOD_ID, "foundation");

    public static StructureFeature<StructurePoolFeatureConfig> FEATURE;
    public static ConfiguredStructureFeature<?, ?> CONFIGURED_FEATURE;

    public static int foundationSize = 2;

    @Override
    public void register() {
        FEATURE = new FoundationFeature(StructurePoolFeatureConfig.CODEC);

        FabricStructureBuilder.create(FOUNDATION_ID, FEATURE)
            .step(GenerationStep.Feature.UNDERGROUND_STRUCTURES)
            .defaultConfig(48, 24, 4231521)
            .register();

        Identifier FOUNDATION_OVERWORLD_ID = new Identifier(Strange.MOD_ID, "foundation_overworld");
        CONFIGURED_FEATURE = RegistryHandler.configuredFeature(FOUNDATION_OVERWORLD_ID, FEATURE.configure(new StructurePoolFeatureConfig(() -> FoundationGenerator.FOUNDATION_POOL, foundationSize)));
    }

    @Override
    public void init() {
        // register all custom foundations here
        FoundationGenerator.FOUNDATIONS.add(new StoneRoomFoundation());

        // builds and registers all foundations into pools
        FoundationGenerator.init();

        if (!FoundationGenerator.FOUNDATIONS.isEmpty()) {
            addStructureFeatureToBiomes(BiomeHelper.MOUNTAINS, CONFIGURED_FEATURE);
            addStructureFeatureToBiomes(BiomeHelper.PLAINS, CONFIGURED_FEATURE);
        }

        // add player location callback
        PlayerState.listeners.add((player, tag) -> {
            if (player != null && player.world != null && !player.world.isClient)
                tag.putBoolean("ruin", PosHelper.isInsideStructure((ServerWorld)player.world, player.getBlockPos(), FEATURE));
        });
    }
}
