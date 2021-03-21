package svenhjol.strange.stonecircles;

import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.handler.ModuleHandler;
import svenhjol.charm.base.handler.RegistryHandler;
import svenhjol.charm.base.helper.BiomeHelper;
import svenhjol.charm.base.iface.Config;
import svenhjol.charm.base.iface.Module;
import svenhjol.strange.Strange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Module(mod = Strange.MOD_ID, description = "Circles of stone columns. Runestones may appear at the top of a column.")
public class
StoneCircles extends CharmModule {
    public static final Identifier STRUCTURE_ID = new Identifier(Strange.MOD_ID, "stone_circle");
    public static final Identifier PIECE_ID = new Identifier(Strange.MOD_ID, "stone_circle_piece");

    public static StructurePieceType STONE_CIRCLE_PIECE;
    public static StructureFeature<DefaultFeatureConfig> STONE_CIRCLE_STRUCTURE;
    public static ConfiguredStructureFeature<?, ?> STONE_CIRCLE;

    @Config(name = "Available biomes", description = "Biomes that stone circles may generate in.")
    public static List<String> configBiomes = new ArrayList<>(Arrays.asList(
        "minecraft:plains",
        "minecraft:desert",
        "minecraft:savanna",
        "minecraft:swamp",
        "minecraft:sunflower_plains",
        "minecraft:flower_forest",
        "minecraft:snowy_tundra",
        "terrestria:cypress_swamp",
        "terrestria:lush_desert",
        "terrestria:volcanic_island",
        "terrestria:dunes",
        "traverse:meadow",
        "traverse:lush_swamp"
    ));

    @Config(name = "Distance between stone circles", description = "Distance between stone circles. As a guide, ruined portals are 25.")
    public static int spacing = 24;

    @Override
    public void register() {
        STONE_CIRCLE_STRUCTURE = new StoneCircleFeature(DefaultFeatureConfig.CODEC);
        STONE_CIRCLE_PIECE = RegistryHandler.structurePiece(PIECE_ID, StoneCircleGenerator::new);

        FabricStructureBuilder.create(STRUCTURE_ID, STONE_CIRCLE_STRUCTURE)
            .step(GenerationStep.Feature.SURFACE_STRUCTURES)
            .defaultConfig(spacing, 8, 515122)
            .register();

        STONE_CIRCLE = RegistryHandler.configuredFeature(STRUCTURE_ID, STONE_CIRCLE_STRUCTURE.configure(DefaultFeatureConfig.DEFAULT));
    }

    @Override
    public boolean depends() {
        return ModuleHandler.enabled("strange:runestones");
    }

    @Override
    public void init() {
        configBiomes.forEach(biomeId -> BuiltinRegistries.BIOME.getOrEmpty(new Identifier(biomeId))
            .flatMap(BuiltinRegistries.BIOME::getKey) // flatmap is shorthand for ifPresent(thing) -> return do(thing)
            .ifPresent(biomeKey -> {
                Charm.LOG.debug("[StoneCircles] Added stone circle to biome: " + biomeId);
                BiomeHelper.addStructureFeature(biomeKey, STONE_CIRCLE);
            }));
    }
}
