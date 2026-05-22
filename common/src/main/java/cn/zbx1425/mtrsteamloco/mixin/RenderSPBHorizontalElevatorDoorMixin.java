package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.block.BlockPSDAPGDoorBase;
import mtr.render.RenderPSDAPGDoor;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "ru/weryskok/mtrrumetro/render/RenderSPBHorizontalElevatorDoor")
public class RenderSPBHorizontalElevatorDoorMixin {
    @Redirect(method = "lambda$render$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0))
    private static void lambda$render$0(PoseStack instance, double x, double y, double z, BlockPSDAPGDoorBase.TileEntityPSDAPGDoorBase entity) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        instance.translate(
                0.5 + ((double) entity.getBlockPos().getX()) - cameraPos.x(),
                ((double) entity.getBlockPos().getY()) - cameraPos.y(),
                0.5 + ((double) entity.getBlockPos().getZ()) - cameraPos.z()
        );
    }
}
