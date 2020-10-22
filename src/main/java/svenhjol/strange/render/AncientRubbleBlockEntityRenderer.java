package svenhjol.strange.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import svenhjol.strange.blockentity.AncientRubbleBlockEntity;

public class AncientRubbleBlockEntityRenderer<T extends AncientRubbleBlockEntity> extends BlockEntityRenderer<T> {
    public AncientRubbleBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public boolean rendersOutsideBoundingBox(T blockEntity) {
        return true;
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        World world = entity.getWorld();
        if (world == null)
            return;

        ItemStack stack = entity.getItemStack();
        if (stack == null || stack.isEmpty())
            return;

        matrices.push();
        matrices.scale(0.65F, 0.65F, 0.65F);
        matrices.translate(0.75F, 0.75F, 0.75F);

        matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion((float)entity.hashCode() % 45.0F));
        matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion((float)entity.hashCode() % 360.0F));
        matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion((float)entity.hashCode() % 45.0F));

        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers);
        matrices.pop();
    }
}
