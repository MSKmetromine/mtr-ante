package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.RailwayDataCoolDownModule;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RailwayDataCoolDownModule.class)
public class RailwayDataCooldownModuleMixin {
    @Redirect(method = "lambda$tick$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean tickAddFreshEntity(Level instance, Entity entity) {
        instance.getServer().execute(() -> {
            instance.addFreshEntity(entity);
        });

        return true;
    }
}
