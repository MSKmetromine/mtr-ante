package cn.zbx1425.mtrsteamloco.mixin;

import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.render.RailPicker;
import cn.zbx1425.mtrsteamloco.render.RenderUtil;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import cn.zbx1425.mtrsteamloco.scripting.ScriptContextManager;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import com.mojang.blaze3d.vertex.PoseStack;
import cn.zbx1425.sowcer.math.Matrix4f;
import mtr.entity.EntitySeat;
import net.minecraft.resources.ResourceLocation;
import cn.zbx1425.mtrsteamloco.render.block.BlockEntityEyeCandyRenderer;
import cn.zbx1425.mtrsteamloco.render.block.BlockEntityDirectNodeRenderer;
import mtr.render.RenderTrains;
import net.minecraft.client.Minecraft;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.render.RailDistanceRenderer;
import mtr.mappings.EntityRendererMapper;
import cn.zbx1425.mtrsteamloco.data.Rolling;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.client.*;
import mtr.data.*;
import mtr.mappings.Utilities;
import mtr.path.PathData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import cn.zbx1425.mtrsteamloco.item.RoutePathCreator;

import java.util.*;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTrains.class)
public class RenderTrainsMixin extends EntityRendererMapper<EntitySeat> implements IGui{

    RenderTrainsMixin() {
        super(null);
    }

    @Override
	public ResourceLocation getTextureLocation(EntitySeat entity) {
		return null;
	}

    @Shadow(remap = false) private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    @Shadow(remap = false) private static void renderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth, String texture, float u1, float v1, float u2, float v2) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    @Shadow(remap = false) private static void renderSignalsStandard(Level world, PoseStack matrices, MultiBufferSource vertexConsumers, Rail rail, BlockPos startPos, BlockPos endPos) {
        throw new IllegalStateException("Mixin failed to apply");
    }

    private static void lambda$render$8(net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos startPos, int maxRailDistance, java.util.Map<UUID, RailType> renderedRailMap, net.minecraft.world.level.Level world, boolean renderColors, com.mojang.blaze3d.vertex.PoseStack matrices, net.minecraft.client.renderer.MultiBufferSource vertexConsumers, net.minecraft.core.BlockPos endPos, mtr.data.Rail rail) {

        if (!((RailExtraSupplier) (Object) rail).isBetween(player.getX(), player.getY(), player.getZ(), maxRailDistance)) {
			return;
		}

		final UUID railProduct = PathData.getRailProduct(startPos, endPos);
		if (renderedRailMap.containsKey(railProduct)) {
			if (renderedRailMap.get(railProduct) == rail.railType) {
				return;
			}
		} else {
			renderedRailMap.put(railProduct, rail.railType);
		}

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		switch (rail.transportMode) {
			case TRAIN:
				renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, renderColors, 1);
				if (renderColors) {
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				}
				break;
			case BOAT:
				if (renderColors) {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 0.5F);
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				}
				break;
			case CABLE_CAR:
				if (rail.railType.hasSavedRail || rail.railType == RailType.CABLE_CAR_STATION) {
					renderRailStandard(world, rail, 0.25F + SMALL_OFFSET, renderColors, 0.25F, "mtr:textures/block/metal.png", 0.25F, 0, 0.75F, 1);
				}
				if (renderColors && !rail.railType.hasSavedRail) {
					renderRailStandard(world, rail, 0.5F + SMALL_OFFSET, true, 1, "mtr:textures/block/one_way_rail_arrow.png", 0, 0.75F, 1, 0.25F);
				}

				if (rail.railType != RailType.NONE) {
					rail.render((x1, z1, x2, z2, x3, z3, x4, z4, y1, y2) -> {
						final int r = renderColors ? (rail.railType.color >> 16) & 0xFF : 0;
						final int g = renderColors ? (rail.railType.color >> 8) & 0xFF : 0;
						final int b = renderColors ? rail.railType.color & 0xFF : 0;

                        IDrawing.drawLine(
                                matrices,
                                vertexConsumers,
                                (float) (x1 - cameraPos.x()),
                                (float) (y1 + 0.5F - cameraPos.y()),
                                (float) (z1 - cameraPos.z()),
                                (float) (x3 - cameraPos.x()),
                                (float) (y2 + 0.5F - cameraPos.y()),
                                (float) (z3 - cameraPos.z()),
                                r,
                                g,
                                b
                        );
                    }, 0, 0);
				}

				break;
			case AIRPLANE:
				if (renderColors) {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, true, 1);
					renderSignalsStandard(world, matrices, vertexConsumers, rail, startPos, endPos);
				} else {
					renderRailStandard(world, rail, 0.0625F + SMALL_OFFSET, false, 0.25F, "textures/block/iron_block.png", 0.25F, 0, 0.75F, 1);
				}
				break;
		}
    }

    @Inject(at = @At("HEAD"),
            method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    private static void renderHead(EntitySeat entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        matrices.popPose();
        matrices.pushPose();

        Minecraft.getInstance().level.getProfiler().popPush("MTRRailwayData");
        RenderUtil.commonVertexConsumers = vertexConsumers;
        RenderUtil.commonPoseStack = matrices;
        RenderUtil.updateElapsedTicks();
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0), method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    private static void renderTranslateCam(PoseStack instance, double x, double y, double z) {}

    @Inject(at = @At("TAIL"),
            method = "render(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")
    private static void renderTail(EntitySeat entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        // Already once per frame, since TAIL
        Rolling.update();
        Minecraft.getInstance().level.getProfiler().popPush("NTERailwayData");
        Matrix4f viewMatrix = new Matrix4f(matrices.last().pose());
        MainClient.railRenderDispatcher.prepareDraw();
        if (ClientConfig.getRailRenderLevel() >= 2) {
            GlStateTracker.capture();
            MainClient.railRenderDispatcher.drawRails(Minecraft.getInstance().level, MainClient.drawScheduler, viewMatrix);
            MainClient.drawScheduler.commitRaw(MainClient.drawContext);

            GlStateTracker.restore();
            if (Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes() && !Minecraft.getInstance().showOnlyReducedInfo()) {
                MainClient.railRenderDispatcher.drawBoundingBoxes(matrices, vertexConsumers.getBuffer(RenderType.lines()));
            }

            MainClient.railRenderDispatcher.drawRailNodes(Minecraft.getInstance().level, MainClient.drawScheduler, viewMatrix);
        }

        // if (ShadersModHandler.isRenderingShadowPass()) {
        //     Main.LOGGER.info("0 shadow pass" + System.currentTimeMillis() + " " + entity);
        // } else {
        //     Main.LOGGER.info("0 normal pass" + System.currentTimeMillis() + " " + entity);
        // }

        BlockEntityEyeCandyRenderer.commit(matrices, vertexConsumers);
        BlockEntityDirectNodeRenderer.commit(matrices, vertexConsumers);

        MainClient.drawContext.drawWithBlaze = !ClientConfig.useRenderOptimization();
        MainClient.drawContext.sortTranslucentFaces = ClientConfig.translucentSort;
        BufferSourceProxy vertexConsumersProxy = new BufferSourceProxy(vertexConsumers);
        MainClient.drawScheduler.commit(vertexConsumersProxy, MainClient.drawContext);
        vertexConsumersProxy.commit();

        if (Minecraft.getInstance().player != null && RailRenderDispatcher.isHoldingBrush) {
            RailPicker.pick();
            RailPicker.render(matrices, vertexConsumers);
            RailDistanceRenderer.render(matrices, vertexConsumers);
        } else {
            RailPicker.pickedRail = null;
        }

        ScriptContextManager.disposeDeadContexts();
    }

    @Inject(at = @At("HEAD"), cancellable = true,
            method = "renderRailStandard(Lnet/minecraft/world/level/Level;Lmtr/data/Rail;FZFLjava/lang/String;FFFF)V")
    private static void onRenderRailStandard(Level world, Rail rail, float yOffset, boolean renderColors, float railWidth, String texture, float u1, float v1, float u2, float v2, CallbackInfo ci) {
        if (ClientConfig.getRailRenderLevel() == 0) {
            ci.cancel();
            return;
        }
        if (ClientConfig.getRailRenderLevel() >= 2) {
            boolean railAccepted = MainClient.railRenderDispatcher.registerRail(rail);
            if (railAccepted) ci.cancel();
        }
    }

    @Inject(method = "isHoldingRailRelated", at = @At("TAIL"), remap = false, cancellable = true) 
    private static void onIsHoldingRailRelated(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
            cir.getReturnValue() ||
            Utilities.isHolding(player, item -> item instanceof RoutePathCreator)
        );
    }

    // Camera relative patches
    @Redirect(method = "lambda$render$5", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0), remap = false)
    private static void renderLiftTranslate(PoseStack instance, double x, double y, double z, LiftClient lift) {
        if (lift.getViewOffset() != null) {
            instance.translate(x, y, z);
            return;
        }

        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        instance.translate(x - cameraPos.x(), y - cameraPos.y(), z - cameraPos.z());
    }

    @Redirect(method = "lambda$renderRailStandard$16", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 0))
    private static void renderRailStandardDrawTextureLambda16A(
            PoseStack matrices, VertexConsumer vertexConsumer,
            float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            double x1, double y1, float yOffset, double z1, double x2, double z2, double x3, double y2, double z3, double x4, double z4
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x1 - cameraPos.x()),
                (float) (y1 + yOffset - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                (float) (x2 - cameraPos.x()),
                (float) (y1 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 + yOffset - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                (float) (x4 - cameraPos.x()),
                (float) (y2 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "lambda$renderRailStandard$16", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 1))
    private static void renderRailStandardDrawTextureLambda16B(
            PoseStack matrices, VertexConsumer vertexConsumer,
            float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            double x1, double y1, float yOffset, double z1, double x2, double z2, double x3, double y2, double z3, double x4, double z4
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x2 - cameraPos.x()),
                (float) (y1 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x1 - cameraPos.x()),
                (float) (y1 + yOffset - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                (float) (x4 - cameraPos.x()),
                (float) (y2 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 + yOffset - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "lambda$renderRailStandard$15", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 0))
    private static void renderRailStandardDrawTextureLambda15A(
            PoseStack matrices, VertexConsumer vertexConsumer,
            float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            double x1, double y1, float yOffset, double z1, double x2, double z2, double x3, double y2, double z3, double x4, double z4
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x1 - cameraPos.x()),
                (float) (y1 + yOffset - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                (float) (x2 - cameraPos.x()),
                (float) (y1 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 + yOffset - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                (float) (x4 - cameraPos.x()),
                (float) (y2 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "lambda$renderRailStandard$15", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 1))
    private static void renderRailStandardDrawTextureLambda15B(
            PoseStack matrices, VertexConsumer vertexConsumer,
            float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            double x1, double y1, float yOffset, double z1, double x2, double z2, double x3, double y2, double z3, double x4, double z4
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x2 - cameraPos.x()),
                (float) (y1 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x1 - cameraPos.x()),
                (float) (y1 + yOffset - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                (float) (x4 - cameraPos.x()),
                (float) (y2 + yOffset + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 + yOffset - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "lambda$renderSignalsStandard$18", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 0))
    private static void renderSignalsStandard0(
            PoseStack matrices, VertexConsumer vertexConsumer, float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            int maxRailDistance, boolean shouldGlow, Level world, PoseStack matrices2, VertexConsumer vertexConsumer2, float u1b, float u2b, int color2,
            double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4, double y1, double y2
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x1 - cameraPos.x()),
                (float) (y1 - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                (float) (x2 - cameraPos.x()),
                (float) (y1 + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                (float) (x4 - cameraPos.x()),
                (float) (y2 + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }

    @Redirect(method = "lambda$renderSignalsStandard$18", at = @At(value = "INVOKE", target = "Lmtr/client/IDrawing;drawTexture(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFFFFFFFFFLnet/minecraft/core/Direction;II)V", ordinal = 1))
    private static void renderSignalsStandard1(
            PoseStack matrices, VertexConsumer vertexConsumer, float x1o, float y1o, float z1o, float x2o, float y2o, float z2o, float x3o, float y3o, float z3o, float x4o, float y4o, float z4o, float u1, float v1, float u2, float v2, Direction facing, int color, int light,
            int maxRailDistance, boolean shouldGlow, Level world, PoseStack matrices2, VertexConsumer vertexConsumer2, float u1b, float u2b, int color2,
            double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4, double y1, double y2
    ) {
        var cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        IDrawing.drawTexture(
                matrices,
                vertexConsumer,
                (float) (x4 - cameraPos.x()),
                (float) (y2 + 0.003125F - cameraPos.y()),
                (float) (z4 - cameraPos.z()),
                (float) (x3 - cameraPos.x()),
                (float) (y2 - cameraPos.y()),
                (float) (z3 - cameraPos.z()),
                (float) (x2 - cameraPos.x()),
                (float) (y1 + 0.003125F - cameraPos.y()),
                (float) (z2 - cameraPos.z()),
                (float) (x1 - cameraPos.x()),
                (float) (y1 - cameraPos.y()),
                (float) (z1 - cameraPos.z()),
                u1,
                v1,
                u2,
                v2,
                facing,
                color,
                light
        );
    }
}
