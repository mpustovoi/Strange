package svenhjol.strange.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import svenhjol.charm.helper.RegistryHelper;
import svenhjol.strange.Strange;

import java.util.HashMap;
import java.util.Map;

public class StrangeSounds {
    public static Map<ResourceLocation, SoundEvent> REGISTER = new HashMap<>();

    public static final SoundEvent SCREENSHOT = createSound("screenshot");

    public static void init() {
        REGISTER.forEach(RegistryHelper::sound);
    }

    public static SoundEvent createSound(String name) {
        ResourceLocation id = new ResourceLocation(Strange.MOD_ID, name);
        SoundEvent sound = new SoundEvent(id);
        REGISTER.put(id, sound);
        return sound;
    }
}
