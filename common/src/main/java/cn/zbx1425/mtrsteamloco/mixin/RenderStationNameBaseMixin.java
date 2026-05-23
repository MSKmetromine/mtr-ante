package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockPSDTop;
import mtr.block.BlockStationNameBase;
import mtr.render.RenderRouteBase;
import mtr.render.RenderStationNameBase;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderStationNameBase.class)
public class RenderStationNameBaseMixin {
    @Redirect(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private static void lambda$render$0(PoseStack instance, double x, double y, double z, BlockStationNameBase.TileEntityStationNameBase entity) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        instance.translate(
                0.5 + ((double) entity.getBlockPos().getX()) - cameraPos.x(),
                0.5 + entity.yOffset + ((double) entity.getBlockPos().getY()) - cameraPos.y(),
                0.5 + ((double) entity.getBlockPos().getZ()) - cameraPos.z()
        );
    }
}
