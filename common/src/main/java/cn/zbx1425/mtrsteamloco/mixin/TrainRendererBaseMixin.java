package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.render.TrainRendererBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrainRendererBase.class)
public class TrainRendererBaseMixin {
    // Camera relative patches
    @Redirect(method = "applyAverageTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private static void applyAverageTransform(PoseStack instance, double x, double y, double z) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        instance.translate(x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z());
    }

    @Redirect(method = "renderRidingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private static <E extends Entity> void renderRidingPlayer(EntityRenderDispatcher instance, E entity, double x, double y, double z, float rotationYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, Vec3 viewOffset) {
        if (viewOffset != null) {
            instance.render(entity, x, y, z, rotationYaw, partialTicks, matrixStack, buffer, packedLight);
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        instance.render(entity, x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z(), rotationYaw, partialTicks, matrixStack, buffer, packedLight);
    }
}
