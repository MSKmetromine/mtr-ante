package cn.zbx1425.mtrsteamloco.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.Icons;
import mtr.data.Siding;
import mtr.data.TransportMode;
import mtr.screen.DashboardScreen;
import mtr.screen.SavedRailScreenBase;
import mtr.screen.SidingScreen;
import mtr.data.RailType;

import mtr.screen.WidgetBetterCheckbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SidingScreen.class)
public abstract class SidingScreenMixin extends SavedRailScreenBase<Siding> implements Icons {
    @Shadow
    @Final
    private static Component MAX_MANUAL_SPEED;

    private SidingScreenMixin(Siding savedRailBase, TransportMode transportMode, DashboardScreen dashboardScreen, Component... additionalTexts) {
        super(savedRailBase, transportMode, dashboardScreen, additionalTexts);
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lmtr/data/RailType;ordinal()I",
            remap = false
        ),
        remap = false
    )
    private int modifySliderMaxValue(RailType railType) {
        return RailType.valueOf("P10000").ordinal();
    }

    @Redirect(
            method = "setIsSelectingTrain",
            at = @At(value = "INVOKE", target = "Lmtr/screen/WidgetBetterCheckbox;selected()Z", ordinal = 0)
    )
    private boolean alwaysShowMaxManualSpeed(WidgetBetterCheckbox instance) {
        return true;
    }

#if MC_VERSION >= "11200"
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 3)
    )
    private int hideMaxManualSpeedText(GuiGraphics instance, Font font, Component text, int x, int y, int color) {
        return x;
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 0)
    )
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        guiGraphics.drawString(this.font, MAX_MANUAL_SPEED, 20, 154, -1);
    }
#else
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I", ordinal = 3)
    )
    private int hideMaxManualSpeedText(Font instance, PoseStack poseStack, Component text, float x, float y, int color) {
        return (int) x;
    }

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I", ordinal = 0)
    )
    private void render(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft.getInstance().font.draw(matrices, MAX_MANUAL_SPEED, 20.0F, 154.0F, -1);
    }
#endif
}