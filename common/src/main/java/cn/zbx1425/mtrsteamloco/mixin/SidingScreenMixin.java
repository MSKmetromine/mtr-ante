package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.data.SidingExtraSupplier;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.Icons;
import mtr.client.IDrawing;
import mtr.data.Siding;
import mtr.data.TransportMode;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.*;
import mtr.data.RailType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
#endif

@Mixin(SidingScreen.class)
public abstract class SidingScreenMixin extends SavedRailScreenBase<Siding> implements Icons {
    private static final int SPEED_LIMIT_STEP = 5;

    @Shadow
    @Final
    private static Component MAX_MANUAL_SPEED;

    @Shadow(remap = false)
    protected abstract void setIsSelectingTrain(boolean isSelectingTrain);

    @Shadow(remap = false)
    @Final
    private WidgetBetterCheckbox buttonIsManual;

    @Unique
    private WidgetBetterCheckbox buttonEnableSpeedLimit;

    @Unique
    private WidgetShorterSlider sliderSpeedLimit;

    private SidingScreenMixin(Siding savedRailBase, TransportMode transportMode, DashboardScreen dashboardScreen, Component... additionalTexts) {
        super(savedRailBase, transportMode, dashboardScreen, additionalTexts);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void constructor(Siding siding, TransportMode transportMode, DashboardScreen dashboardScreen, CallbackInfo ci) {
        this.buttonEnableSpeedLimit = new WidgetBetterCheckbox(0, 0, 0, 20, Text.translatable("gui.mtrsteamloco.enable_speed_limit"), checked -> {
            this.setIsSelectingTrain(false);
        });

        this.sliderSpeedLimit = new WidgetShorterSlider(0, 160, 1000 / SPEED_LIMIT_STEP, speed -> String.format("%s km/h", speed * SPEED_LIMIT_STEP), null);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo ci) {
        IDrawing.setPositionAndWidth(this.buttonEnableSpeedLimit, 20, 168, this.width - this.textWidth - 40);
        this.buttonEnableSpeedLimit.setChecked(((SidingExtraSupplier) this.savedRailBase).isSpeedLimitEnabled());

        UtilitiesClient.setWidgetX(this.sliderSpeedLimit, 20 + this.textWidth);
        UtilitiesClient.setWidgetY(this.sliderSpeedLimit, 188);
        this.sliderSpeedLimit.setHeight(20);
        this.sliderSpeedLimit.setValue(((SidingExtraSupplier) this.savedRailBase).getSpeedLimit() / SPEED_LIMIT_STEP);

        if (this.showScheduleControls) {
            this.addDrawableChild(this.buttonEnableSpeedLimit);
            this.addDrawableChild(this.sliderSpeedLimit);
        }
    }

    @Inject(method = "setIsSelectingTrain", at = @At("TAIL"), remap = false)
    private void setIsSelectingTrain(boolean isSelectingTrain, CallbackInfo ci) {
        this.buttonEnableSpeedLimit.visible = !isSelectingTrain && !this.buttonIsManual.selected();
        this.sliderSpeedLimit.visible = !isSelectingTrain && !this.buttonIsManual.selected() && this.buttonEnableSpeedLimit.selected();
    }

    @Inject(method = "onClose", at = @At(value = "INVOKE", target = "Lmtr/data/Siding;setUnlimitedTrains(ZIZIFIZLjava/util/function/Consumer;)V", remap = false))
    public void onClose(CallbackInfo ci) {
        ((SidingExtraSupplier) this.savedRailBase).updateSpeedLimit(
                this.buttonEnableSpeedLimit.selected(),
                this.sliderSpeedLimit.getIntValue() * SPEED_LIMIT_STEP,
                packet -> PacketTrainDataGuiClient.sendUpdate(this.getPacketIdentifier(), packet)
        );
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

#if MC_VERSION >= "12000"
    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 0)
    )
    private void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.showScheduleControls && !this.buttonIsManual.selected() && this.buttonEnableSpeedLimit.selected()) {
            guiGraphics.drawString(this.font, t.translatable("gui.mtrsteamloco.speed_limit"), 20, 194, -1);
        }
    }
#else
    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFI)I", ordinal = 0)
    )
    private void render(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.showScheduleControls && !this.buttonIsManual.selected() && this.buttonEnableSpeedLimit.selected()) {
            this.font.draw(matrices, Text.translatable("gui.mtrsteamloco.speed_limit"), 20.0F, 194.0F, -1);
        }
    }
#endif
}