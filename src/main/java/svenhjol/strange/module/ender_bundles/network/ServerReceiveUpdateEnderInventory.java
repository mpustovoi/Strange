package svenhjol.strange.module.ender_bundles.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import svenhjol.charm.network.Id;
import svenhjol.charm.network.ServerReceiver;

@Id("strange:update_ender_inventory")
public class ServerReceiveUpdateEnderInventory extends ServerReceiver {
    @Override
    public void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buffer) {

    }
}
