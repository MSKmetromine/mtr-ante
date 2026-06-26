package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.SidingExtraSupplier;
import cn.zbx1425.mtrsteamloco.util.CarPosition;
import cn.zbx1425.mtrsteamloco.util.CustomTrainType;
import cn.zbx1425.mtrsteamloco.util.PositionRotation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPlatform;
import mtr.client.ClientData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import net.minecraft.world.phys.Vec3;
import mtr.path.PathData;
import cn.zbx1425.mtrsteamloco.data.TrainExtraSupplier;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import org.msgpack.core.MessagePacker;
import cn.zbx1425.mtrsteamloco.network.util.StringMapSerializer;
import org.msgpack.value.Value;
import net.minecraft.nbt.CompoundTag;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;
import mtr.data.*;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Train.class)
public abstract class TrainMixin implements TrainExtraSupplier{

    protected List<Double> distances;
    protected List<PathData> path;

    private Map<String, String> customConfigs = new HashMap<>();
    private Map<String, ConfigResponder> configResponders = new HashMap<>();
    private boolean isConfigsChanged = false;

    private CustomTrainType customTrainType;
    private CarPosition[] carPositions;

    private ConcurrentMap<Integer, Boolean> doorsLeft;
    private ConcurrentMap<Integer, Boolean> doorsRight;

    @Override
    public Map<String, String> getCustomConfigs() {
        return customConfigs;
    }

    @Override
    public void setCustomConfigs(Map<String, String> customConfigs) {
        this.customConfigs = customConfigs;
    }

    @Override
    public boolean isConfigsChanged() {
        return isConfigsChanged;
    }

    @Override
    public void isConfigsChanged(boolean isConfigsChanged) {
        this.isConfigsChanged = isConfigsChanged;
    }

    @Override
    public void setConfigResponders(Map<String, ConfigResponder> configResponders) {
        this.configResponders = configResponders;
    }

    @Override
    public Map<String, ConfigResponder> getConfigResponders() {
        return configResponders;
    }

    @Override
    public CarPosition[] getCarPositions() {
        return carPositions;
    }

    @Shadow(remap = false)
    public abstract int getIndex(double tempRailProgress, boolean roundDown);

    @Shadow(remap = false)
    protected abstract float getModelZOffset();

    @Shadow(remap = false)
    @Final
    public TransportMode transportMode;

    @Shadow(remap = false)
    protected double railProgress;

    @Shadow(remap = false)
    @Final
    public int spacing;

    @Shadow(remap = false)
    protected boolean reversed;

    @Shadow(remap = false)
    @Final
    public int trainCars;

    @Shadow
    protected abstract Vec3 getRoutePosition(int car, int trainSpacing);

    @Shadow(remap = false)
    public static double getAverage(double a, double b) {
        return 0;
    }

    @Shadow(remap = false)
    @Final
    public String baseTrainType;

    @Shadow(remap = false)
    protected abstract double asin(double v);

    @Shadow(remap = false)
    public static RailType convertMaxManualSpeed(int maxManualSpeed) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Shadow(remap = false)
    @Final
    public int maxManualSpeed;

    @Shadow(remap = false)
    @Final
    public long sidingId;

    @Shadow
    protected abstract boolean scanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks);

    @Mutable
    @Shadow(remap = false)
    @Final
    protected Set<UUID> ridingEntities;

    @Override
    public float getRollAngleAt(double value) {
        int i = getIndex(value, true);
        if (i != 0) value -= distances.get(i - 1);
        Rail r = path.get(i).rail;
        float rot = RailExtraSupplier.getRollAngle(r, value);
        return rot;
    }

    @Inject(method = "<init>(JJFLjava/lang/String;Ljava/lang/String;ILjava/util/List;Ljava/util/List;IIFZII)V", at = @At("TAIL"), remap = false)
    private void fromDefinitions(long id, long sidingId, float railLength, String trainId, String baseTrainType, int trainCars, List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2, float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime, CallbackInfo ci) {
        this.customTrainType = CustomTrainType.parse(this.baseTrainType);
        this.carPositions = new CarPosition[this.trainCars];
        this.doorsLeft = new ConcurrentHashMap<>();
        this.doorsRight = new ConcurrentHashMap<>();
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "<init>(JFLjava/util/List;Ljava/util/List;IIFZIILjava/util/Map;)V", at = @At("TAIL"), remap = false)
    private void fromMassagePack(
            long sidingId, float railLength,
            List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
            float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
            Map<String, Value> map, CallbackInfo ci
    ) {
        MessagePackHelper messagePackHelper = new MessagePackHelper(map);
        try {
            customConfigs = StringMapSerializer.deserialize(messagePackHelper.getString("custom_configs"));
        } catch (IOException e) {
            customConfigs = new HashMap<>();
        }

        this.customTrainType = CustomTrainType.parse(this.baseTrainType);
        this.carPositions = new CarPosition[this.trainCars];
        this.doorsLeft = new ConcurrentHashMap<>();
        this.doorsRight = new ConcurrentHashMap<>();
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "<init>(JFLjava/util/List;Ljava/util/List;IIFZIILnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void fromCompoundTag(
            long sidingId, float railLength,
            List<PathData> path, List<Double> distances, int repeatIndex1, int repeatIndex2,
            float accelerationConstant, boolean isManualAllowed, int maxManualSpeed, int manualToAutomaticTime,
            CompoundTag compoundTag, CallbackInfo ci
    ) {
        try {
            customConfigs = StringMapSerializer.deserialize(compoundTag.getString("custom_configs"));
        } catch (IOException e) {
            customConfigs = new HashMap<>();
        }

        this.customTrainType = CustomTrainType.parse(this.baseTrainType);
        this.carPositions = new CarPosition[this.trainCars];
        this.doorsLeft = new ConcurrentHashMap<>();
        this.doorsRight = new ConcurrentHashMap<>();
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void fromFriendlyByteBuf(FriendlyByteBuf buffer, CallbackInfo ci) {
        try {
            customConfigs = StringMapSerializer.deserialize(buffer.readUtf());
        } catch (IOException e) {
            customConfigs = new HashMap<>();
        }

        this.customTrainType = CustomTrainType.parse(this.baseTrainType);
        this.carPositions = new CarPosition[this.trainCars];
        this.doorsLeft = new ConcurrentHashMap<>();
        this.doorsRight = new ConcurrentHashMap<>();
        this.ridingEntities = Collections.synchronizedSet(this.ridingEntities);
    }

    @Inject(method = "toMessagePack", at = @At("TAIL"), remap = false)
    private void toMessagePack(MessagePacker messagePacker, CallbackInfo ci) throws IOException {
        String res;
        try {
            res = StringMapSerializer.serializeToString(customConfigs);
        } catch (IOException e) {
            res = "";
        }
        messagePacker.packString("custom_configs").packString(res);
    }

    @Inject(method = "messagePackLength", at = @At("TAIL"), cancellable = true, remap = false)
    private void messagePackLength(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(cir.getReturnValue() + 1);
    }

    @Inject(method = "writePacket", at = @At("TAIL"))
    private void toPacket(FriendlyByteBuf packet, CallbackInfo ci) {
        String res;
        try {
            res = StringMapSerializer.serializeToString(customConfigs);
        } catch (IOException e) {
            res = "";
        }
        packet.writeUtf(res);
    }

    protected abstract boolean skipScanBlocks(Level world, double trainX, double trainY, double trainZ);

    protected abstract boolean openDoors(Level world, Block block, BlockPos checkPos, int dwellTicks);

    protected float doorValue;

    protected boolean doorTarget;

    private static Class<?> IBlockPlatformClass = Void.class;

    static {
        try {
            IBlockPlatformClass = Class.forName("team.dovecotmc.metropolis.block.interfaces.IBlockPlatform");
            Main.LOGGER.info("Loaded metropolis IBlockPlatformClass");
        } catch (ClassNotFoundException e) {
            Main.LOGGER.error("Failed to load metropolis IBlockPlatformClass");
        }
    }

    @Inject(method = "scanDoors", at = @At("HEAD"), cancellable = true)
    private void onScanDoors(Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks, CallbackInfoReturnable<Boolean> ci) {
        if (skipScanBlocks(world, trainX, trainY, trainZ)) {
            ci.setReturnValue(false);
            return;
        }

        boolean hasPlatform = false;
        boolean isClientSide = world.isClientSide();
        final Vec3 offsetVec = new Vec3(1, 0, 0).yRot(checkYaw).xRot(pitch);
        final Vec3 traverseVec = new Vec3(0, 0, 1).yRot(checkYaw).xRot(pitch);
        Set<BlockPos> OKPos = new HashSet<>();
        for (int checkX = 1; checkX <= 3; checkX++) {
            for (int checkY = -2; checkY <= 3; checkY++) {
                for (double checkZ = -halfSpacing; checkZ <= halfSpacing; checkZ++) {
                    final BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * checkX + traverseVec.x * checkZ, trainY + checkY, trainZ + offsetVec.z * checkX + traverseVec.z * checkZ);

                    if (!RailwayData.chunkLoaded(world, checkPos)) {
                        continue;
                    }

                    final Block block = world.getBlockState(checkPos).getBlock();

                    if (block instanceof BlockPlatform || block instanceof BlockPSDAPGBase || IBlockPlatformClass.isInstance(block)) {
                        openDoors(world, block, checkPos, dwellTicks);
                        hasPlatform = true;
                    }else if (block instanceof BlockEyeCandy) {
                        if (OKPos.contains(checkPos)) continue;
                        int[] dir = new int[]{1, -1};
                        int[] f = new int[]{1, 0, 0, 1, 0, 0};
                        if (checkEyeCandy(world, checkPos, isClientSide)) hasPlatform = true;
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 2; j++) {
                                for (int k = 1; k <= 40; k++) {
                                    int v = dir[j] * k;
                                    BlockPos pos = checkPos.offset(f[i] * v, f[i + 1] * v, f[i + 2] * v);
                                    if (OKPos.contains(pos)) break;
                                    OKPos.add(pos);
                                    if (checkEyeCandy(world, pos, isClientSide)) hasPlatform = true;
                                    else break;
                                }
                            }
                        }

                    }
                }
            }
        }
        ci.setReturnValue(hasPlatform);
        return;
    }

    private boolean checkEyeCandy(Level world, BlockPos pos, boolean isClientSide) {
        final BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BlockEyeCandy.BlockEntityEyeCandy) {
            BlockEyeCandy.BlockEntityEyeCandy e = (BlockEyeCandy.BlockEntityEyeCandy) entity;
            if (e.isPlatform()) {
                if (isClientSide) {
                    e.setDoorTarget(doorTarget);
                    e.setDoorValue(doorValue);
                }
                return true;
            } else return false;
        } else {
            return false;
        }
    }

    private static interface Void {
    }

    @Inject(method = "convertMaxManualSpeed", at = @At("HEAD"), cancellable = true, remap = false)
    private static void convertMaxManualSpeed(int maxManualSpeed, CallbackInfoReturnable<RailType> ci) {
        maxManualSpeed = Math.min(maxManualSpeed, RailType.values().length - 1);
        ci.setReturnValue(RailType.values()[maxManualSpeed]);
        ci.cancel();
        return;
    }

    private PositionRotation getBogiePositionRotation(double carRailProgress, double halfLength, double bogiePosition, double totalVehicleLength) {
        final double bogieProgress = carRailProgress + (reversed ? 1 : -1) * (halfLength + bogiePosition);

        final double lowerBound = railProgress - totalVehicleLength;
        final double clampedValue = Math.min(Math.max(bogieProgress, lowerBound), railProgress);

        final double clamp = Math.min(Math.max(Math.min(Math.abs(clampedValue - lowerBound), Math.abs(clampedValue - railProgress)), 0.1), 1.0);

        final double value1 = Math.min(Math.max(clampedValue + (reversed ? -clamp : clamp), lowerBound), railProgress - 0.001);
        final double value2 = Math.min(Math.max(clampedValue - (reversed ? -clamp : clamp), lowerBound), railProgress - 0.001);

        final int index1 = this.getIndex(value1, false);
        final Vec3 position1 = ((PathData)this.path.get(index1)).rail.getPosition(value1 - (index1 == 0 ? (double)0.0F : (Double)this.distances.get(index1 - 1))).add((double)0.0F, (double)this.transportMode.railOffset, (double)0.0F);

        final int index2 = this.getIndex(value2, false);
        final Vec3 position2 = ((PathData)this.path.get(index2)).rail.getPosition(value2 - (index2 == 0 ? (double)0.0F : (Double)this.distances.get(index2 - 1))).add((double)0.0F, (double)this.transportMode.railOffset, (double)0.0F);

        final float yaw = (float) Mth.atan2(position2.x - position1.x, position2.z - position1.z);
        final float pitch = (float) this.asin((position2.y - position1.y) / position2.distanceTo(position1));

        final Vec3 average = new Vec3(
                getAverage(position1.x(), position2.x()),
                getAverage(position1.y(), position2.y()),
                getAverage(position1.z(), position2.z())
        );

        return new PositionRotation(average.add(0.0, 1.0, 0.0), yaw, pitch);
    }

    @Inject(method = "getRoutePosition", at = @At("HEAD"), remap = false)
    public void getRoutePosition(int car, int trainSpacing, CallbackInfoReturnable<Vec3> cir) {
        if (car < 0 || car >= this.trainCars) {
            return;
        }

        final double totalVehicleLength = trainCars * spacing;

        final double carRailProgress = (railProgress - (reversed ? totalVehicleLength : 0)) + ((reversed ? 1 : -1) * car * spacing);

        final double halfLength = spacing / 2.0;

        PositionRotation bogie1 = this.getBogiePositionRotation(
                carRailProgress,
                halfLength,
                customTrainType.getBogiePosition1(),
                totalVehicleLength
        );

        PositionRotation bogie2 = this.getBogiePositionRotation(
                carRailProgress,
                halfLength,
                customTrainType.getBogiePosition2(),
                totalVehicleLength
        );

        Vec3 carPosition = new Vec3(
                getAverage(bogie1.position().x(), bogie2.position().x()),
                getAverage(bogie1.position().y(), bogie2.position().y()),
                getAverage(bogie1.position().z(), bogie2.position().z())
        );

        double pivotOffset = getAverage(
                customTrainType.getBogiePosition1(),
                customTrainType.getBogiePosition2()
        );

        carPosition = carPosition.add(
                bogie2.position().subtract(bogie1.position()).scale(pivotOffset)
        );

        final float yaw = (float) Mth.atan2(bogie2.position().x() - bogie1.position().x(), bogie2.position().z() - bogie1.position().z());
        final float pitch = (float) this.asin((bogie2.position().y() - bogie1.position().y()) / bogie2.position().distanceTo(bogie1.position()));

        final PositionRotation carPositionRotation = new PositionRotation(carPosition, yaw, pitch);

        this.carPositions[car] = new CarPosition(carPositionRotation, Arrays.asList(bogie1, bogie2));
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "x", ordinal = 0)
    public double modifyCalculateCarX(double value, Level world, Vec3[] positions, int index, int dwellTicks) {
        return this.carPositions[index].car().position().x();
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "y", ordinal = 1)
    public double modifyCalculateCarY(double value, Level world, Vec3[] positions, int index, int dwellTicks) {
        return this.carPositions[index].car().position().y();
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "z", ordinal = 2)
    public double modifyCalculateCarZ(double value, Level world, Vec3[] positions, int index, int dwellTicks) {
        return this.carPositions[index].car().position().z();
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "realSpacing", ordinal = 3)
    public double modifyCalculateCarRealSpacing(double value, Level world, Vec3[] positions, int index, int dwellTicks) {
        Vec3 bogie1 = this.carPositions[index].bogies().get(0).position();
        Vec3 bogie2 = this.carPositions[index].bogies().get(1).position();

        return bogie1.distanceTo(bogie2)
                + (spacing / 2.0 - Math.abs(customTrainType.getBogiePosition1()))
                + (spacing / 2.0 - Math.abs(customTrainType.getBogiePosition2()));
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "yaw", ordinal = 0)
    public float modifyCalculateCarYaw(float value, Level world, Vec3[] positions, int index, int dwellTicks) {
        return this.carPositions[index].car().yaw();
    }

    @ModifyVariable(method = "calculateCar", at = @At(value = "INVOKE_ASSIGN", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", shift = At.Shift.BEFORE), name = "pitch", ordinal = 1)
    public float modifyCalculateCarPitch(float value, Level world, Vec3[] positions, int index, int dwellTicks) {
        return this.carPositions[index].car().pitch();
    }

    @WrapOperation(method = "simulateTrain", at = @At(value = "INVOKE", target = "Lmtr/data/Train;getRailSpeed(I)F"), remap = false)
    public float wrapRailSpeed(Train instance, int railIndex, Operation<Float> original, Level world) {
        DataCache cache;

        if (world.isClientSide()) {
            cache = ClientData.DATA_CACHE;
        } else {
            cache = RailwayData.getInstance(world).dataCache;
        }

        var siding = cache.sidingIdMap.get(this.sidingId);

        var originalValue = original.call(instance, railIndex);

        if (siding == null) {
            return originalValue;
        }

        if (!((SidingExtraSupplier) siding).isSpeedLimitEnabled()) {
            return originalValue;
        }

        float speedLimit = ((SidingExtraSupplier) siding).getSpeedLimit() / 3.6F / 20.0F;

        if (originalValue > speedLimit) {
            return speedLimit;
        }

        return originalValue;
    }

    @WrapOperation(method = "simulateTrain", at = @At(value = "FIELD", target = "Lmtr/data/Train;accelerationConstant:F", ordinal = 1), remap = false)
    public float wrapDeceleration(Train instance, Operation<Float> original, Level world) {
        DataCache cache;

        if (world.isClientSide()) {
            cache = ClientData.DATA_CACHE;
        } else {
            cache = RailwayData.getInstance(world).dataCache;
        }

        var siding = cache.sidingIdMap.get(this.sidingId);

        if (siding == null) {
            return original.call(instance);
        }

        return ((SidingExtraSupplier) siding).isDecelerationConstantEnabled() ? ((SidingExtraSupplier) siding).getDecelerationConstant() : original.call(instance);
    }

    @Redirect(method = "calculateCar", at = @At(value = "INVOKE", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", ordinal = 0))
    public boolean calculateCarScanDoors0(Train instance, Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks, Level world2, Vec3[] positions, int index) {
        Runnable runnable = () -> this.doorsLeft.put(
                index,
                this.scanDoors(world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks)
        );

        if (!world.isClientSide()) {
            world.getServer().execute(runnable);
        } else {
            runnable.run();
        }

        return this.doorsLeft.getOrDefault(index, false);
    }

    @Redirect(method = "calculateCar", at = @At(value = "INVOKE", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", ordinal = 1))
    public boolean calculateCarScanDoors1(Train instance, Level world, double trainX, double trainY, double trainZ, float checkYaw, float pitch, double halfSpacing, int dwellTicks, Level world2, Vec3[] positions, int index) {
        Runnable runnable = () -> this.doorsRight.put(
                index,
                this.scanDoors(world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks)
        );

        if (!world.isClientSide()) {
            world.getServer().execute(runnable);
        } else {
            runnable.run();
        }

        return this.doorsRight.getOrDefault(index, false);
    }
}