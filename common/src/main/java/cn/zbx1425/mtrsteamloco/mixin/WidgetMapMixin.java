package cn.zbx1425.mtrsteamloco.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mtr.screen.WidgetMap;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = WidgetMap.class, remap = false)
public class WidgetMapMixin {
    @Unique
    private static double mtrSteamLoco$left;

    @Unique
    private static double mtrSteamLoco$right;

    @Unique
    private static double mtrSteamLoco$top;

    @Unique
    private static double mtrSteamLoco$bottom;

    @Inject(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I", ordinal = 0), remap = true)
    private static void mouseOnSavedRailPre(CallbackInfo ci, @Local(name = "savedRailCount") int savedRailCount, @Local(name = "i") int i, @Local(name = "savedRailPos") BlockPos savedRailPos) {
        mtrSteamLoco$left = savedRailPos.getX();
        mtrSteamLoco$right = savedRailPos.getX() + 1;
        mtrSteamLoco$top = (double) savedRailPos.getZ() + ((double) i) / (double) savedRailCount;
        mtrSteamLoco$bottom = (double) savedRailPos.getZ() + ((double) i + 1.0) / (double) savedRailCount;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 0), index = 1)
    private static double isBetween0Arg1(double value) {
        return mtrSteamLoco$left;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 0), index = 2)
    private static double isBetween0Arg2(double value) {
        return mtrSteamLoco$right;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 1), index = 1)
    private static double isBetween1Arg1(double value) {
        return mtrSteamLoco$top;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 1), index = 2)
    private static double isBetween1Arg2(double value) {
        return mtrSteamLoco$bottom;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetMap$MouseOnSavedRailCallback;mouseOnSavedRailCallback(Lmtr/data/SavedRailBase;DDDD)V"), index = 1)
    private static double mouseOnSavedRailCallbackArg1(double value) {
        return mtrSteamLoco$left;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetMap$MouseOnSavedRailCallback;mouseOnSavedRailCallback(Lmtr/data/SavedRailBase;DDDD)V"), index = 2)
    private static double mouseOnSavedRailCallbackArg2(double value) {
        return mtrSteamLoco$top;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetMap$MouseOnSavedRailCallback;mouseOnSavedRailCallback(Lmtr/data/SavedRailBase;DDDD)V"), index = 3)
    private static double mouseOnSavedRailCallbackArg3(double value) {
        return mtrSteamLoco$right;
    }

    @ModifyArg(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetMap$MouseOnSavedRailCallback;mouseOnSavedRailCallback(Lmtr/data/SavedRailBase;DDDD)V"), index = 4)
    private static double mouseOnSavedRailCallbackArg4(double value) {
        return mtrSteamLoco$bottom;
    }
}
