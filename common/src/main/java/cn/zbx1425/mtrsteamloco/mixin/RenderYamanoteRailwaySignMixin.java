package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockRailwaySign;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

#if MC_VERSION < "12000"
import top.mcmtr.blocks.BlockYamanoteRailwaySign;
#endif

@Pseudo
@Mixin(targets = "top/mcmtr/render/RenderYamanoteRailwaySign")
public class RenderYamanoteRailwaySignMixin {
    #if MC_VERSION < "12000"
    @Redirect(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private static void lambdaRender$0(PoseStack instance, double x, double y, double z, BlockYamanoteRailwaySign.TileEntityRailwaySign entity) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        instance.translate(
                0.5 + (double) entity.getBlockPos().getX() - cameraPos.x(),
                0.53125 + (double) entity.getBlockPos().getY() - cameraPos.y(),
                0.5 + (double) entity.getBlockPos().getZ() - cameraPos.z()
        );
    }
    #endif
}
