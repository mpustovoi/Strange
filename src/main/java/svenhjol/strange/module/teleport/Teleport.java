package svenhjol.strange.module.teleport;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import svenhjol.charm.annotation.CommonModule;
import svenhjol.charm.annotation.Config;
import svenhjol.charm.helper.LogHelper;
import svenhjol.charm.helper.NetworkHelper;
import svenhjol.charm.loader.CharmModule;
import svenhjol.strange.Strange;
import svenhjol.strange.api.event.ActivateRunestoneCallback;
import svenhjol.strange.init.StrangeParticles;
import svenhjol.strange.module.bookmarks.BookmarkBranch;
import svenhjol.strange.module.discoveries.DiscoveryBranch;
import svenhjol.strange.module.knowledge.branch.BiomeBranch;
import svenhjol.strange.module.knowledge.branch.DimensionBranch;
import svenhjol.strange.module.knowledge.branch.StructureBranch;
import svenhjol.strange.module.runes.RuneBranch;
import svenhjol.strange.module.runes.RuneHelper;
import svenhjol.strange.module.runic_tomes.RunicTomeItem;
import svenhjol.strange.module.runic_tomes.event.ActivateRunicTomeCallback;
import svenhjol.strange.module.teleport.handler.*;
import svenhjol.strange.module.teleport.helper.TeleportHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@CommonModule(mod = Strange.MOD_ID, alwaysEnabled = true)
public class Teleport extends CharmModule {
    public static final int TELEPORT_TICKS = 10;
    public static final int REPOSITION_TICKS = 5;

    public static final ResourceLocation MSG_CLIENT_TELEPORT_EFFECT = new ResourceLocation(Strange.MOD_ID, "client_teleport_effect");

    public static List<ITicket> teleportTickets = new ArrayList<>();
    public static List<ITicket> repositionTickets = new ArrayList<>();
    public static List<UUID> noEndPlatform = new ArrayList<>();
    public static ThreadLocal<Entity> entityCreatingPlatform = new ThreadLocal<>();

    @Config(name = "Travel distance in blocks", description = "Maximum number of blocks that you will be teleported via a runestone.")
    public static int maxDistance = 5000;

    @Config(name = "Travel protection time", description = "Number of seconds of regeneration, fire resistance and slow fall after teleporting.")
    public static int protectionDuration = 10;

    @Config(name = "Travel penalty time", description = "Number of seconds of poison, wither, burning, slowness or weakness after teleporting without the correct item.")
    public static int penaltyDuration = 10;

    @Override
    public void runWhenEnabled() {
        ServerTickEvents.END_SERVER_TICK.register(this::handleTick);

        // register the actions that can teleport a player
        ActivateRunestoneCallback.EVENT.register(this::handleActivateRunestone);
        ActivateRunicTomeCallback.EVENT.register(this::handleActivateRunicTome);
    }

    private void handleActivateRunestone(ServerPlayer player, BlockPos origin, String runes, ItemStack sacrifice) {
        if (!tryTeleport(player, runes, sacrifice, origin)) {
            LogHelper.warn(this.getClass(), "Runestone activation failed");
            TeleportHelper.explode((ServerLevel)player.level, origin, 2.0F, Explosion.BlockInteraction.BREAK);
        }
    }

    private void handleActivateRunicTome(ServerPlayer player, BlockPos origin, ItemStack tome, ItemStack sacrifice) {
        String runes = RunicTomeItem.getRunes(tome);
        if (!tryTeleport(player, runes, sacrifice, origin)) {
            LogHelper.warn(this.getClass(), "Runic tome activation failed");
            TeleportHelper.explode((ServerLevel)player.level, origin, 2.0F, Explosion.BlockInteraction.BREAK);
        }
    }

    private boolean tryTeleport(ServerPlayer player, String runes, ItemStack sacrifice, BlockPos origin) {
        TeleportHandler<?> handler;
        ServerLevel level = (ServerLevel)player.level;
        RuneBranch<?, ?> branch = RuneHelper.branch(runes);
        if (branch == null) return false;

        handler = switch (branch.getBranchName()) {
            case BiomeBranch.NAME -> new BiomeTeleportHandler((BiomeBranch)branch);
            case BookmarkBranch.NAME -> new BookmarkTeleportHandler((BookmarkBranch)branch);
            case DimensionBranch.NAME -> new DimensionTeleportHandler((DimensionBranch)branch);
            case DiscoveryBranch.NAME -> new DiscoveryTeleportHandler((DiscoveryBranch)branch);
            case StructureBranch.NAME -> new StructureTeleportHandler((StructureBranch)branch);
            default -> null;
        };

        if (handler == null) {
            LogHelper.debug(this.getClass(), "No teleport handler for branch: " + branch.getBranchName());
            return false;
        }

        boolean result = handler.setup(level, player, sacrifice, runes, origin);
        if (!result) return false;

        handler.process();
        return true;
    }

    private void handleTick(MinecraftServer server) {
        tick(teleportTickets);
        tick(repositionTickets);
    }

    private void tick(List<ITicket> tickets) {
        if (!tickets.isEmpty()) {
            List<ITicket> toRemove = tickets.stream().filter(entry -> !entry.isValid()).collect(Collectors.toList());
            tickets.forEach(entry -> {
                entry.tick();

                if (!entry.isValid()) {
                    entry.onFail();
                    toRemove.add(entry);
                }

                if (entry.isSuccess()) {
                    entry.onSuccess();
                    toRemove.add(entry);
                }
            });

            if (!toRemove.isEmpty()) {
                int size = toRemove.size();
                toRemove.stream().findFirst().ifPresent(first -> LogHelper.debug(this.getClass(), "Removing " + size + " tickets of class " + first.getClass().getSimpleName()));
                toRemove.forEach(tickets::remove);
            }
        }
    }
    /**
     * Inform the client that we have started a teleport ticket. Allows for client effects.
     */
    public static void sendClientTeleportEffect(ServerPlayer player, BlockPos origin, Type type) {
        NetworkHelper.sendPacketToClient(player, MSG_CLIENT_TELEPORT_EFFECT, buf -> {
            buf.writeEnum(type);
            buf.writeBlockPos(origin);
        });
    }

    public enum Type {
        GENERIC(ParticleTypes.PORTAL),
        RUNESTONE(StrangeParticles.ILLAGERALT),
        RUNIC_TOME(StrangeParticles.ILLAGERALT),
        FAIL(ParticleTypes.SMOKE);

        private final ParticleOptions particle;

        Type(ParticleOptions particle) {
            this.particle = particle;
        }

        public ParticleOptions getParticle() {
            return particle;
        }
    }
}
