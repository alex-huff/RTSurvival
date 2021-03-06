package dev.phonis.rtsurvival.mixin;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.util.math.*;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import dev.phonis.rtsurvival.RTConfig;
import dev.phonis.rtsurvival.render.RenderUtils;

// adapted from Masa's malilib

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render",
        at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V"))
    private void onRenderWorldLastNormal(
        MatrixStack matrices,
        float tickDelta, long limitTime, boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projMatrix,
        CallbackInfo ci)
    {
        this.onRenderWorldLast();
    }

    @Inject(method = "render",
        slice = @Slice(from = @At(value = "FIELD", ordinal = 1, // start from the endDrawing() call
            target = "Lnet/minecraft/client/render/RenderPhase;WEATHER_TARGET:Lnet/minecraft/client/render/RenderPhase$Target;"),
            to = @At(value = "INVOKE", ordinal = 1, // end at the second renderWeather call
                target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V")),
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gl/ShaderEffect;render(F)V"))
    private void onRenderWorldLastFabulous(
        MatrixStack matrices,
        float tickDelta, long limitTime, boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projMatrix,
        CallbackInfo ci)
    {
        this.onRenderWorldLast();
    }

    private void onRenderWorldLast() {
        MinecraftClient mc = this.client;

        if (mc.world == null) return;

        DimensionType currentDimension = mc.world.getDimension();
        Framebuffer fb = MinecraftClient.isFabulousGraphicsOrBetter() ? mc.worldRenderer.getTranslucentFramebuffer() : null;

        if (fb != null) fb.beginWrite(false);

        RenderUtils.renderTethers(currentDimension);

        if (RTConfig.INSTANCE.renderWaypoints) RenderUtils.renderWaypoints(currentDimension);

        RenderUtils.renderChestFindSession(currentDimension);

        if (fb != null) mc.getFramebuffer().beginWrite(false);
    }

}