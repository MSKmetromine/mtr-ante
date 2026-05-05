package cn.zbx1425.mtrsteamloco.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mtr.screen.WidgetMap;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I", ordinal = 0))
    private static void mouseOnSavedRailPre(CallbackInfo ci, @Local(name = "savedRailCount") int savedRailCount, @Local(name = "i") int i, @Local(name = "savedRailPos") BlockPos savedRailPos) {
        mtrSteamLoco$left = savedRailPos.getX();
        mtrSteamLoco$right = savedRailPos.getX() + 1;
        mtrSteamLoco$top = (double) savedRailPos.getZ() + ((double) i) / (double) savedRailCount;
        mtrSteamLoco$bottom = (double) savedRailPos.getZ() + ((double) i + 1.0) / (double) savedRailCount;
    }

    @ModifyArgs(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 0))
    private static void isBetween0(Args args) {
        args.set(1, mtrSteamLoco$left);
        args.set(2, mtrSteamLoco$right);
    }

    @ModifyArgs(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/data/RailwayData;isBetween(DDD)Z", ordinal = 1))
    private static void isBetween1(Args args) {
        args.set(1, mtrSteamLoco$top);
        args.set(2, mtrSteamLoco$bottom);
    }

    @ModifyArgs(method = "lambda$mouseOnSavedRail$11", at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetMap$MouseOnSavedRailCallback;mouseOnSavedRailCallback(Lmtr/data/SavedRailBase;DDDD)V"))
    private static void mouseOnSavedRailCallback(Args args) {
        args.set(1, mtrSteamLoco$left);
        args.set(2, mtrSteamLoco$top);
        args.set(3, mtrSteamLoco$right);
        args.set(4, mtrSteamLoco$bottom);
    }
}
