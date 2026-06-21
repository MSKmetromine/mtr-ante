package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.Lift;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Lift.class)
public abstract class LiftMixin {
    @Shadow
    protected abstract boolean checkDoor(Level world, boolean front);

    @Shadow(remap = false)
    protected boolean frontCanOpen;

    @Shadow(remap = false)
    protected boolean backCanOpen;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lmtr/data/Lift;checkDoor(Lnet/minecraft/world/level/Level;Z)Z", ordinal = 0))
    private boolean checkDoor0(Lift instance, Level world, boolean front) {
        if (world.isClientSide()) {
            return this.checkDoor(world, front);
        }

        world.getServer().execute(() -> {
            this.frontCanOpen = this.checkDoor(world, front);
        });

        return this.frontCanOpen;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lmtr/data/Lift;checkDoor(Lnet/minecraft/world/level/Level;Z)Z", ordinal = 1))
    private boolean checkDoor1(Lift instance, Level world, boolean front) {
        if (world.isClientSide()) {
            return this.checkDoor(world, front);
        }

        world.getServer().execute(() -> {
            this.backCanOpen = this.checkDoor(world, front);
        });

        return this.backCanOpen;
    }
}
