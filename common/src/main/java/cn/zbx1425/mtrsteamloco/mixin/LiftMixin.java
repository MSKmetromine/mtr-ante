package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.Lift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(Lift.class)
public abstract class LiftMixin {
    @Shadow
    protected abstract boolean checkDoor(Level world, boolean front);

    @Shadow(remap = false)
    protected boolean frontCanOpen;

    @Shadow(remap = false)
    protected boolean backCanOpen;

    @Mutable
    @Shadow(remap = false)
    @Final
    protected Set<UUID> ridingEntities;

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void init(FriendlyByteBuf packet, CallbackInfo ci) {
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void init(Map map, CallbackInfo ci) {
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V", at = @At("TAIL"))
    private void init(BlockPos pos, Direction facing, CallbackInfo ci) {
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

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
