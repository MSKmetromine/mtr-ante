package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

#if MC_VERSION < "12000"
import top.mcmtr.data.RigidCatenary;
#endif

@Pseudo
@Mixin(targets = "top/mcmtr/data/RigidCatenary")
public class RigidCatenaryMixin {
    #if MC_VERSION < "12000"
    @Redirect(method = "renderSegment", at = @At(value = "INVOKE", target = "Ltop/mcmtr/data/RigidCatenary$RenderRigidCatenary;renderRigidCatenary(DDDDDDDDDDDDDDDDDD)V"), remap = false)
    public void renderCatenary(RigidCatenary.RenderRigidCatenary instance, double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4, double xs1, double zs1, double xs2, double zs2, double xs3, double zs3, double xs4, double zs4, double y1, double y2) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        instance.renderRigidCatenary(
                x1 - cameraPos.x(), z1 - cameraPos.z(),
                x2 - cameraPos.x(), z2 - cameraPos.z(),
                x3 - cameraPos.x(), z3 - cameraPos.z(),
                x4 - cameraPos.x(), z4 - cameraPos.z(),
                xs1 - cameraPos.x(), zs1 - cameraPos.z(),
                xs2 - cameraPos.x(), zs2 - cameraPos.z(),
                xs3 - cameraPos.x(), zs3 - cameraPos.z(),
                xs4 - cameraPos.x(), zs4 - cameraPos.z(),
                y1 - cameraPos.y(), y2 - cameraPos.y()
        );
    }
    #endif
}
