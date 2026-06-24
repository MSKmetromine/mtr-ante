package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.LiftServer;
import mtr.data.VehicleRidingServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(LiftServer.class)
public abstract class LiftServerMixin {
    private Set<UUID> newRidingEntities;

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lmtr/data/VehicleRidingServer;mountRider(Lnet/minecraft/world/level/Level;Ljava/util/Set;JJDDDDDFFZZILnet/minecraft/resources/ResourceLocation;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"))
    public void tickServerMountRider(Level world, Set<UUID> ridingEntities, long id, long routeId, double carX, double carY, double carZ, double length, double width, float carYaw, float carPitch, boolean doorOpen, boolean canMount, int percentageOffset, ResourceLocation packetId, Function<Player, Boolean> canRide, Consumer<Player> ridingCallback) {
        if (this.newRidingEntities != null) {
            ridingEntities.clear();
            ridingEntities.addAll(this.newRidingEntities);
        } else {
            this.newRidingEntities = Collections.synchronizedSet(new HashSet<>(ridingEntities));
        }

        world.getServer().execute(() -> {
            VehicleRidingServer.mountRider(world, this.newRidingEntities, id, routeId, carX, carY, carZ, length, width, carYaw, carPitch, doorOpen, canMount, percentageOffset, packetId, canRide, ridingCallback);
        });
    }
}
