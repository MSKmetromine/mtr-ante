package cn.zbx1425.mtrsteamloco.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mtr.data.TrainClient;
import mtr.sound.bve.BveTrainSound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BveTrainSound.class)
public class BveTrainSoundMixin {
    @Shadow(remap = false)
    @Final
    private TrainClient train;

    @ModifyArg(method = "playNearestCar", at = @At(value = "INVOKE", target = "Lmtr/sound/TrainLoopingSoundInstance;setData(FFLnet/minecraft/core/BlockPos;)V", ordinal = 3), index = 0)
    public float playNearestCarVolume(float volume, @Local(name = "carIndex") int carIndex) {
        var carIndexNext = carIndex + 1;

        var tempRailProgress1 = Math.max(this.train.getRailProgress() - (double) ((this.train.isReversed() ? this.train.trainCars - carIndex : carIndex) * this.train.spacing), 0.0F);
        int index1 = this.train.getIndex(tempRailProgress1, false);

        var tempRailProgress2 = Math.max(this.train.getRailProgress() - (double) ((this.train.isReversed() ? this.train.trainCars - carIndexNext : carIndexNext) * this.train.spacing), 0.0F);
        int index2 = this.train.getIndex(tempRailProgress2, false);

        var rail1 = this.train.path.get(index1).rail;
        var rail2 = this.train.path.get(index2).rail;

        if (this.train.getSpeed() < 0.01F) {
            return volume;
        }

        if (rail1.facingStart.isParallel(rail1.facingEnd) && rail1.facingStart.isParallel(rail2.facingEnd)) {
            return volume;
        }

        return Math.max(1.0F, volume);
    }
}
