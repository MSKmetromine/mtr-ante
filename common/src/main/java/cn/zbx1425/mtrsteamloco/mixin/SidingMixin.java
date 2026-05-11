package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.data.SidingExtraSupplier;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.Unpooled;
import mtr.data.RailwayDataDriveTrainModule;
import mtr.packet.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import mtr.data.*;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Siding.class)
public abstract class SidingMixin extends SavedRailBase implements IPacket, IReducedSaveData, SidingExtraSupplier {
    private SidingMixin(long id, TransportMode transportMode, BlockPos pos1, BlockPos pos2) {
        super(id, transportMode, pos1, pos2);
    }

    @Unique
    private boolean isSpeedLimitEnabled;

    @Unique
    private int speedLimit;

    // @Inject(method = "simulateTrain", at = @At("TAIL"), remap = false)
    private void __onSimulateTrain(DataCache dataCache, RailwayDataDriveTrainModule railwayDataDriveTrainModule, List<Map<UUID, Long>> trainPositions, SignalBlocks signalBlocks, Map<Player, Set<TrainServer>> trainsInPlayerRange, Set<TrainServer> trainsToSync, Map<Long, List<ScheduleEntry>> schedulesForPlatform, Map<Long, Map<BlockPos, TrainDelay>> trainDelays, CallbackInfo ci) {
        for (TrainServer train : trainsToSync) {
            if (!((TrainExtraSupplier) train).isConfigsChanged()) continue;
            trainsToSync.add(train);
            ((TrainExtraSupplier) train).isConfigsChanged(false);
        }
    }

    @Inject(method = "<init>(JLmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;F)V", at = @At("TAIL"))
    public void init(long id, TransportMode transportMode, BlockPos pos1, BlockPos pos2, float railLength, CallbackInfo ci) {
        this.isSpeedLimitEnabled = false;
        this.speedLimit = 0;
    }

    @Inject(method = "<init>(Lmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;F)V", at = @At("TAIL"))
    public void init(TransportMode transportMode, BlockPos pos1, BlockPos pos2, float railLength, CallbackInfo ci) {
        this.isSpeedLimitEnabled = false;
        this.speedLimit = 0;
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("TAIL"), remap = false)
    public void init(Map<String, Value> map, CallbackInfo ci, @Local(name = "messagePackHelper") MessagePackHelper helper) {
        this.isSpeedLimitEnabled = helper.getBoolean("is_speed_limit_enabled");
        this.speedLimit = helper.getInt("speed_limit");
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    public void init(CompoundTag compoundTag, CallbackInfo ci) {
        this.isSpeedLimitEnabled = compoundTag.getBoolean("is_speed_limit_enabled");
        this.speedLimit = compoundTag.getInt("speed_limit");
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    public void init(FriendlyByteBuf packet, CallbackInfo ci) {
        this.isSpeedLimitEnabled = packet.readBoolean();
        this.speedLimit = packet.readVarInt();
    }

    @Inject(method = "toReducedMessagePack", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;writeMessagePackDataset(Lorg/msgpack/core/MessagePacker;Ljava/util/Collection;Ljava/lang/String;)V"), remap = false)
    public void toReducedMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        messagePacker.packString("is_speed_limit_enabled").packBoolean(this.isSpeedLimitEnabled);
        messagePacker.packString("speed_limit").packInt(this.speedLimit);
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    public void writePacket(FriendlyByteBuf packet, CallbackInfo ci) {
        packet.writeBoolean(this.isSpeedLimitEnabled);
        packet.writeVarInt(this.speedLimit);
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    public void update(String key, FriendlyByteBuf packet, CallbackInfo ci) {
        if (key.equals("speed_limit")) {
            this.isSpeedLimitEnabled = packet.readBoolean();
            this.speedLimit = packet.readVarInt();

            ci.cancel();
        }
    }

    @Override
    public boolean isSpeedLimitEnabled() {
        return this.isSpeedLimitEnabled;
    }

    @Override
    public void setSpeedLimitEnabled(boolean speedLimitEnabled) {
        this.isSpeedLimitEnabled = speedLimitEnabled;
    }

    @Override
    public int getSpeedLimit() {
        return this.speedLimit;
    }

    @Override
    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }

    @Override
    public void updateSpeedLimit(boolean enabled, int speedLimit, Consumer<FriendlyByteBuf> sendPacket) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());

        packet.writeLong(this.id);
        packet.writeUtf(this.transportMode.toString());
        packet.writeUtf("speed_limit");

        packet.writeBoolean(enabled);
        packet.writeVarInt(speedLimit);

        sendPacket.accept(packet);

        this.isSpeedLimitEnabled = enabled;
        this.speedLimit = speedLimit;
    }
}
