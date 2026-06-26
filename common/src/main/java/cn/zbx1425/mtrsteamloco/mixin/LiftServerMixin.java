package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(LiftServer.class)
public abstract class LiftServerMixin extends Lift {
    @Unique
    private int mtrSteamLoco$oldPassengerCount;

    private LiftServerMixin(BlockPos pos, Direction facing) {
        super(pos, facing);
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lmtr/data/VehicleRidingServer;mountRider(Lnet/minecraft/world/level/Level;Ljava/util/Set;JJDDDDDFFZZILnet/minecraft/resources/ResourceLocation;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"))
    public void tickServerMountRider(Level world, Set<UUID> ridingEntities, long id, long routeId, double carX, double carY, double carZ, double length, double width, float carYaw, float carPitch, boolean doorOpen, boolean canMount, int percentageOffset, ResourceLocation packetId, Function<Player, Boolean> canRide, Consumer<Player> ridingCallback) {
        world.getServer().execute(() -> {
            VehicleRidingServer.mountRider(world, ridingEntities, id, routeId, carX, carY, carZ, length, width, carYaw, carPitch, doorOpen, canMount, percentageOffset, packetId, canRide, ridingCallback);
        });
    }

    @Redirect(method = "lambda$tickServer$0", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object handlePositionsNewHashSet(Map<Object, Object> instance, Object k, Object v) {
        return instance.put(k, Collections.synchronizedSet((Set<?>) v));
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I", ordinal = 0))
    private int getOldPassengerCount(Set instance) {
        return this.mtrSteamLoco$oldPassengerCount;
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void simulateTrainEnd(Level world, Map<Player, Set<LiftServer>> liftsInPlayerRange, Set<LiftServer> liftsToSync, CallbackInfo ci) {
        this.mtrSteamLoco$oldPassengerCount = this.ridingEntities.size();
    }
}
