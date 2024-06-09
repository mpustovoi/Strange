package svenhjol.strange.feature.runestones;

import net.minecraft.util.Mth;
import svenhjol.charm.charmony.annotation.Configurable;
import svenhjol.charm.charmony.annotation.Feature;
import svenhjol.charm.charmony.common.CommonFeature;
import svenhjol.charm.charmony.common.CommonLoader;
import svenhjol.strange.feature.runestones.common.*;

@Feature(description = """
    Runestones are blocks that teleport players to interesting structures and biomes.
    Each runestone will show what item is needed as a sacrifice. Drop the item near the runestone to activate it.
    Once the runestone is activated, throw an enderpearl at it to teleport.""")
public final class Runestones extends CommonFeature {
    public final Registers registers;
    public final Handlers handlers;
    public final Networking networking;
    public final Providers providers;
    public final Advancements advancements;

    @Configurable(
        name = "Dizziness effect",
        description = "If true, the player's view will distort while teleporting as if travelling via a nether portal."
    )
    private static boolean dizzyEffect = true;

    @Configurable(
        name = "Protection duration",
        description = "Duration (in seconds) of protection given to the player while they teleport via a runestone."
    )
    private static int protectionDuration = 3;

    public Runestones(CommonLoader loader) {
        super(loader);

        registers = new Registers(this);
        handlers = new Handlers(this);
        networking = new Networking(this);
        providers = new Providers(this);
        advancements = new Advancements(this);
    }

    public boolean dizzyEffect() {
        return dizzyEffect;
    }

    public int protectionDuration() {
        return Mth.clamp(protectionDuration, 0, 60) * 20;
    }
}
