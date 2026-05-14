package cn.zbx1425.mtrsteamloco.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

#if MC_VERSION < "12000"
import top.mcmtr.data.TransCatenary;
#endif

@Pseudo
@Mixin(targets = "top/mcmtr/data/TransCatenary")
public class TransCatenaryMixin {
    #if MC_VERSION < "12000"
    @Redirect(method = "renderSegment", at = @At(value = "INVOKE", target = "Ltop/mcmtr/data/TransCatenary$RenderTransCatenary;renderTransCatenary(DDDDDDDDDDDD)V"), remap = false)
    public void renderCatenary(TransCatenary.RenderTransCatenary instance, double x1, double y1, double z1, double x2, double y2, double z2, double count, double i, double base, double sinX, double sinY, double increment) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        instance.renderTransCatenary(
                x1 - cameraPos.x(), y1 - cameraPos.y(), z1 - cameraPos.z(),
                x2 - cameraPos.x(), y2 - cameraPos.y(), z2 - cameraPos.z(),
                count, i, base, sinX, sinY, increment
        );
    }
    #endif
}
