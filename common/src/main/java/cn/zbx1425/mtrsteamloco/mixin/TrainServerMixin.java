package cn.zbx1425.mtrsteamloco.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mtr.block.*;
import mtr.data.RailwayData;
import mtr.data.Train;
import mtr.data.TrainServer;
import mtr.data.VehicleRidingServer;
import mtr.path.PathData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(TrainServer.class)
public abstract class TrainServerMixin extends Train {
    @Shadow(remap = false)
    private long routeId;

    private TrainServerMixin(long id, long sidingId, float railLength, String trainId, String baseTrainType, int trainCars, List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2, float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime) {
        super(id, sidingId, railLength, trainId, baseTrainType, trainCars, path, distances, repeatIndex1, repeatIndex2, accelerationConstant, isManualAllowed, maxManualSpeed, manualToAutomaticTime);
    }

    @Shadow
    private static void transferItems(Container inventoryFrom, Container inventoryTo) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow
    protected abstract void checkBlock(BlockPos pos, Consumer<BlockPos> callback);

    @Shadow
    private int manualCoolDown;

    @Redirect(method = "simulateTrain", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object simulateTrainNewHashMap(Map<Object, Object> instance, Object k, Object v) {
        return instance.put(k, Collections.synchronizedList((List<?>) v));
    }

    @Redirect(method = "handlePositions", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;chunkLoaded(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", ordinal = 0))
    public boolean redirectSensorUpdates(Level world, BlockPos pos, @Local(name = "frontPos") BlockPos frontPos) {
        world.getServer().execute(() -> {
            if (RailwayData.chunkLoaded(world, frontPos)) {
                this.checkBlock(frontPos, (checkPos) -> {
                    if (RailwayData.chunkLoaded(world, checkPos)) {
                        BlockState state = world.getBlockState(checkPos);
                        Block block = state.getBlock();
                        if (block instanceof BlockTrainRedstoneSensor && BlockTrainSensorBase.matchesFilter(world, checkPos, this.routeId, this.speed)) {
                            ((BlockTrainRedstoneSensor) block).power(world, state, checkPos);
                        }

                        if ((block instanceof BlockTrainCargoLoader || block instanceof BlockTrainCargoUnloader) && BlockTrainSensorBase.matchesFilter(world, checkPos, this.routeId, this.speed)) {
                            for (Direction direction : Direction.values()) {
                                Container nearbyInventory = HopperBlockEntity.getContainerAt(world, checkPos.relative(direction));
                                if (nearbyInventory != null) {
                                    if (block instanceof BlockTrainCargoLoader) {
                                        transferItems(nearbyInventory, this.inventory);
                                    } else {
                                        transferItems(this.inventory, nearbyInventory);
                                    }
                                }
                            }
                        }
                    }

                });
            }
        });

        return false;
    }

    @Redirect(method = "handlePositions", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;chunkLoaded(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", ordinal = 1))
    public boolean redirectAnnounce(Level world, BlockPos pos, @Local(name = "frontPos") BlockPos frontPos) {
        world.getServer().execute(() -> {
            if (RailwayData.chunkLoaded(world, frontPos)) {
                this.checkBlock(frontPos, (checkPos) -> {
                    if (RailwayData.chunkLoaded(world, checkPos) && world.getBlockState(checkPos).getBlock() instanceof BlockTrainAnnouncer) {
                        BlockEntity entity = world.getBlockEntity(checkPos);
                        if (entity instanceof BlockTrainAnnouncer.TileEntityTrainAnnouncer && ((BlockTrainAnnouncer.TileEntityTrainAnnouncer) entity).matchesFilter(this.routeId, this.speed)) {
                            this.ridingEntities.forEach((uuid) -> ((BlockTrainAnnouncer.TileEntityTrainAnnouncer) entity).announce(world.getPlayerByUUID(uuid)));
                        }
                    }
                });
            }
        });

        return false;
    }

    @Redirect(method = "simulateCar", at = @At(value = "INVOKE", target = "Lmtr/data/VehicleRidingServer;mountRider(Lnet/minecraft/world/level/Level;Ljava/util/Set;JJDDDDDFFZZILnet/minecraft/resources/ResourceLocation;Ljava/util/function/Function;Ljava/util/function/Consumer;)V"))
    private void simulateCarMountRider(Level world, Set<UUID> ridingEntities, long id, long routeId, double carX, double carY, double carZ, double length, double width, float carYaw, float carPitch, boolean doorOpen, boolean canMount, int percentageOffset, ResourceLocation packetId, Function<Player, Boolean> canRide, Consumer<Player> ridingCallback) {
        world.getServer().execute(() -> {
            VehicleRidingServer.mountRider(world, ridingEntities, id, routeId, carX, carY, carZ, length, width, carYaw, carPitch, doorOpen, canMount, percentageOffset, packetId, canRide, ridingCallback);
        });
    }
}
