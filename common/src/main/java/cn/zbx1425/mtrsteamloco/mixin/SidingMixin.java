package cn.zbx1425.mtrsteamloco.mixin;

import mtr.data.RailwayDataDriveTrainModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import mtr.data.*;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Siding.class)
public abstract class SidingMixin {

    @Shadow(remap = false)
    private int maxManualSpeed;

    @Shadow(remap = false)
    private boolean isManual;

    // @Inject(method = "simulateTrain", at = @At("TAIL"), remap = false)
    private void __onSimulateTrain(DataCache dataCache, RailwayDataDriveTrainModule railwayDataDriveTrainModule, List<Map<UUID, Long>> trainPositions, SignalBlocks signalBlocks, Map<Player, Set<TrainServer>> trainsInPlayerRange, Set<TrainServer> trainsToSync, Map<Long, List<ScheduleEntry>> schedulesForPlatform, Map<Long, Map<BlockPos, TrainDelay>> trainDelays, CallbackInfo ci) {
        for (TrainServer train : trainsToSync) {
            if (!((TrainExtraSupplier) train).isConfigsChanged()) continue;
            trainsToSync.add(train);
            ((TrainExtraSupplier) train).isConfigsChanged(false);
        }
    }

    @Inject(method = "toReducedMessagePack", at = @At("HEAD"), remap = false)
    public void toReducedMessagePack(MessagePacker messagePacker, CallbackInfo ci) {
        try {
            messagePacker.packString("applied_default_max_speed").packBoolean(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void init(Map<String, Value> map, CallbackInfo ci) {
        var helper = new MessagePackHelper(map);

        if (helper.getBoolean("applied_default_max_speed")) {
            return;
        }

        if (!this.isManual) {
            this.maxManualSpeed = RailType.valueOf("P10000").ordinal();
        }
    }

    @Inject(method = "<init>(JLmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;F)V", at = @At("TAIL"))
    public void init2(long id, TransportMode transportMode, BlockPos pos1, BlockPos pos2, float railLength, CallbackInfo ci) {
        this.maxManualSpeed = RailType.valueOf("P10000").ordinal();
    }

    @Inject(method = "<init>(Lmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;F)V", at = @At("TAIL"))
    public void init3(TransportMode transportMode, BlockPos pos1, BlockPos pos2, float railLength, CallbackInfo ci) {
        this.maxManualSpeed = RailType.valueOf("P10000").ordinal();
    }
}
