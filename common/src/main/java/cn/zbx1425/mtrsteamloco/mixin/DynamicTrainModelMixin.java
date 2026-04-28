package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.render.integration.DynamicTrainModelLoader;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.client.DoorAnimationType;
import mtr.client.DynamicTrainModel;
import mtr.model.ModelTrainBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(DynamicTrainModel.class)
public class DynamicTrainModelMixin {
    @Unique
    private boolean loaded;

    @Unique
    private JsonObject model;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void ctor(JsonObject model, JsonObject properties, DoorAnimationType doorAnimationType, CallbackInfo ci) {
        this.loaded = false;
        this.model = model;
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void render(PoseStack matrices, VertexConsumer vertices, ModelTrainBase.RenderStage renderStage, int light, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ, int currentCar, int trainCars, boolean head1IsFront, boolean renderDetails, CallbackInfo ci) {
        if (this.loaded) {
            return;
        }
        
        this.loaded = true;

        DynamicTrainModelLoader.loadInto(model, (DynamicTrainModel)(Object)this);
    }
}
