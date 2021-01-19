package svenhjol.strange.ruins;

import com.mojang.serialization.Codec;
import net.minecraft.world.gen.feature.JigsawFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

public class SurfaceRuinFeature extends JigsawFeature {
    public SurfaceRuinFeature(Codec<StructurePoolFeatureConfig> codec) {
        super(codec, 64, false, true);
    }
}
