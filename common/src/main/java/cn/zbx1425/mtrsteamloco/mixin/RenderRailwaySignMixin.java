package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockRailwaySign;
import mtr.render.RenderRailwaySign;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderRailwaySign.class)
public class RenderRailwaySignMixin {
    @Redirect(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private static void lambdaRender$0(PoseStack instance, double x, double y, double z, BlockRailwaySign.TileEntityRailwaySign entity, Direction facing, BlockRailwaySign block, PoseStack matricesNew) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        matricesNew.translate(
                0.5 + (double) entity.getBlockPos().getX() - cameraPos.x(),
                0.53125 + (double) entity.getBlockPos().getY() - cameraPos.y(),
                0.5 + (double) entity.getBlockPos().getZ() - cameraPos.z()
        );
    }
}
