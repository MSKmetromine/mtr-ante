package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.client.IDrawing;
import mtr.data.TrainClient;
import mtr.render.TrainRendererBase;
import mtr.render.JonModelTrainRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import cn.zbx1425.sowcer.math.PoseStackUtil;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = JonModelTrainRenderer.class, remap = false)
public abstract class JonModelTrainRendererMixin extends TrainRendererBase{

    @Shadow @Final private TrainClient train;

    @Inject(method = "renderCar", at = @At("HEAD"), remap = false, cancellable = true)
    public void renderCar(int carIndex, double x, double y, double z, float yaw, float pitch, boolean doorLeftOpen, boolean doorRightOpen, CallbackInfo ci) {
        if (RenderUtil.shouldSkipRenderTrain(train)) ci.cancel();
    }

    @Inject(method = "renderCar", at = @At(value = "INVOKE", target = "Lmtr/mappings/UtilitiesClient;rotateX(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", ordinal = 0, shift = At.Shift.AFTER), remap = true)
    private void injectRenderCar(int carIndex, double x, double y, double z, float yaw, float pitch, boolean doorLeftOpen, boolean doorRightOpen, CallbackInfo ci) {
        float roll = TrainExtraSupplier.getRollAngleAt(train, carIndex);
        boolean isReversed = train.isReversed();
        matrices.translate(0D, 1D, 0D);
        PoseStackUtil.rotZ(matrices, isReversed ? roll : -roll);
        matrices.translate(0D, -1D, 0D);
    }

    @Inject(method = "renderConnection", at = @At("HEAD"), cancellable = true)
    public void renderConnection(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (RenderUtil.shouldSkipRenderTrain(train)) ci.cancel();
    }

    @Inject(method = "renderBarrier", at = @At("HEAD"), cancellable = true)
    public void renderBarrier(Vec3 prevPos1, Vec3 prevPos2, Vec3 prevPos3, Vec3 prevPos4, Vec3 thisPos1, Vec3 thisPos2, Vec3 thisPos3, Vec3 thisPos4, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (RenderUtil.shouldSkipRenderTrain(train)) ci.cancel();
    }

    // Camera relative patches
    @Redirect(method = "renderCar", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private void renderCarTranslate(PoseStack instance, double x, double y, double z) {
        if (this.train.getViewOffset() != null) {
            instance.translate(x, y, z);
            return;
        }

        var cameraPos = camera.getPosition();
        instance.translate(x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z());
    }

    @Redirect(method = "drawTexture", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V"))
    private static void drawTexture(
            PoseStack matrices, VertexConsumer vertexConsumer,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            PoseStack matrices2, VertexConsumer vertexConsumer2, Vec3 pos1, Vec3 pos2, Vec3 pos3, Vec3 pos4, int light2
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        var pos1Rel = pos1.subtract(cameraPos);
        var pos2Rel = pos2.subtract(cameraPos);
        var pos3Rel = pos3.subtract(cameraPos);
        var pos4Rel = pos4.subtract(cameraPos);

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) pos1Rel.x,
                (float) pos1Rel.y,
                (float) pos1Rel.z,
                (float) pos2Rel.x,
                (float) pos2Rel.y,
                (float) pos2Rel.z,
                (float) pos3Rel.x,
                (float) pos3Rel.y,
                (float) pos3Rel.z,
                (float) pos4Rel.x,
                (float) pos4Rel.y,
                (float) pos4Rel.z,
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "renderConnection", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    public void renderConnectionTranslate(PoseStack instance, double x, double y, double z) {
        if (this.train.getViewOffset() != null) {
            instance.translate(x, y, z);
            return;
        }

        var cameraPos = camera.getPosition();
        instance.translate(x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z());
    }


}
