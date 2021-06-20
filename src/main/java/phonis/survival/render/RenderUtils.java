package phonis.survival.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.dimension.DimensionType;
import phonis.survival.State;
import phonis.survival.networking.*;

import java.util.Arrays;
import java.util.List;

// adapted from Masa's malilib

public class RenderUtils {

    private static final RGBAColor red = new RGBAColor(255, 0, 0, 255);
    private static final RGBAColor blue = new RGBAColor(0, 0, 255, 255);
    private static final RGBAColor yellow = new RGBAColor(255, 255, 0, 255);
    private static final RGBAColor white = new RGBAColor(255, 255, 255, 255);
    private static final RGBAColor plateBackground = new RGBAColor(0x40000000);

    private static boolean compareDimension(RTDimension dimension, DimensionType currentDimension) {
        return (dimension == RTDimension.OVERWORLD && currentDimension.getSkyProperties().equals(DimensionType.OVERWORLD_ID))
            || (dimension == RTDimension.NETHER && currentDimension.getSkyProperties().equals(DimensionType.THE_NETHER_ID))
            || (dimension == RTDimension.END && currentDimension.getSkyProperties().equals(DimensionType.THE_END_ID));
    }

    public static void renderChestFindSession(DimensionType currentDimension) {
        RTChestFindSession chestFindState = State.chestFindState;

        if (chestFindState == null) return;

        if (chestFindState.best == null || chestFindState.closest == null || chestFindState.most == null) return;

        if (chestFindState.most.equals(chestFindState.best)) { // most and best equal
            if (chestFindState.best.equals(chestFindState.closest)) { // transitive all 3 equal
                RenderUtils.renderChestFindLocation(currentDimension, chestFindState.best, RenderUtils.blue, RenderUtils.red, RenderUtils.yellow);
            } else { // just most and best equal
                RenderUtils.renderChestFindLocation(currentDimension, chestFindState.best, RenderUtils.blue, RenderUtils.red);
                RenderUtils.renderChestFindLocation(currentDimension, chestFindState.closest, RenderUtils.yellow);
            }
        } else if (chestFindState.best.equals(chestFindState.closest)) { // just best and closest equal
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.best, RenderUtils.blue, RenderUtils.yellow);
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.most, RenderUtils.red);
        } else if (chestFindState.most.equals(chestFindState.closest)) { // just most and closest equal
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.best, RenderUtils.blue);
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.most, RenderUtils.red, RenderUtils.yellow);
        } else { // none equal
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.best, RenderUtils.blue);
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.most, RenderUtils.red);
            RenderUtils.renderChestFindLocation(currentDimension, chestFindState.closest, RenderUtils.yellow);
        }
    }

    private static void renderChestFindLocation(DimensionType currentDimension, RTChestFindLocation chestFindLocation, RGBAColor... colors) {
        if (RenderUtils.compareDimension(chestFindLocation.dimension, currentDimension))
            RenderUtils.drawTether(chestFindLocation.x, chestFindLocation.y, chestFindLocation.z, colors);
        else if (chestFindLocation.dimension == RTDimension.OVERWORLD && currentDimension.getSkyProperties().equals(DimensionType.THE_NETHER_ID))
            RenderUtils.drawTether(chestFindLocation.x / 8d, 128d, chestFindLocation.z / 8d, colors);
    }

    public static void renderTethers(DimensionType currentDimension) {
        List<RTTether> tetherState = State.tetherState;

        if (tetherState == null) return;

        for (RTTether tether : tetherState) {
            if (RenderUtils.compareDimension(tether.dimension, currentDimension))
                RenderUtils.drawTether(tether.getX(), tether.getY(), tether.getZ(), Double.MAX_VALUE, RenderUtils.white, RenderUtils.red);
            else if (tether.dimension == RTDimension.OVERWORLD && currentDimension.getSkyProperties().equals(DimensionType.THE_NETHER_ID))
                RenderUtils.drawTether(tether.getX() / 8d, 128d, tether.getZ() / 8d, Double.MAX_VALUE, RenderUtils.white, RenderUtils.red);
        }
    }

    private static void drawTether(double x, double y, double z, double length, RGBAColor... lineColors) {
        RenderUtils.drawTether(x, y, z, length, lineColors.length > 1 ? 4 : 1, lineColors);
    }

    private static void drawTether(double x, double y, double z, RGBAColor... lineColors) {
        RenderUtils.drawTether(x, y, z, 10d, lineColors.length > 1 ? 4 : 1, lineColors);
    }

    private static void drawTether(double x, double y, double z, double length, int frequency, RGBAColor... lineColors) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double cx = cameraPos.x;
        double cy = cameraPos.y;
        double cz = cameraPos.z;
        double rotX = camera.getYaw();
        double rotY = camera.getPitch();
        double xz = Math.cos(Math.toRadians(rotY));
        double cxD = -xz * Math.sin(Math.toRadians(rotX));
        double cyD = -Math.sin(Math.toRadians(rotY));
        double czD = xz * Math.cos(Math.toRadians(rotX));
        Vec3d cameraDirection = new Vec3d(cxD, cyD, czD);
        Vec3d focalPoint = cameraDirection.normalize().multiply(2);
        cx = cx + focalPoint.x;
        cy = cy + focalPoint.y;
        cz = cz + focalPoint.z;
        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));
        double realX;
        double realY;
        double realZ;
        Vec3d direction;

        if (distance > length) {
            direction = new Vec3d(dx, dy, dz).normalize().multiply(length);
            realX = cx + direction.x;
            realY = cy + direction.y;
            realZ = cz + direction.z;
        } else {
            direction = new Vec3d(dx, dy, dz);
            realX = x;
            realY = y;
            realZ = z;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        MatrixStack matrixStack = RenderSystem.getModelViewStack();

        matrixStack.push();
        matrixStack.translate(realX - cameraPos.x, realY - cameraPos.y, realZ - cameraPos.z);
        RenderSystem.applyModelViewMatrix();
        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        Vec3d stepVec = direction.multiply(1d / (frequency * lineColors.length));
        int s = 0;

        for (int f = 0; f < frequency; f++) {
            for (RGBAColor lineColor : lineColors) {
                bufferBuilder.vertex(-(stepVec.x * s), -(stepVec.y * s), -(stepVec.z * s)).color(lineColor.r, lineColor.g, lineColor.b, lineColor.a).next();
                bufferBuilder.vertex(-(stepVec.x * (s + 1)), -(stepVec.y * (s + 1)), -(stepVec.z * (s + 1))).color(lineColor.r, lineColor.g, lineColor.b, lineColor.a).next();

                s++;
            }
        }

        tessellator.draw();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
    }

    public static void renderWaypoints(DimensionType currentDimension) {
        List<RTWaypoint> waypointState = State.waypointState;

        if (waypointState == null) return;

        for (RTWaypoint waypoint : waypointState) {
            if (RenderUtils.compareDimension(waypoint.dimension, currentDimension))
                RenderUtils.drawTextPlate(Arrays.asList(waypoint.name.split("\n")), waypoint.x, waypoint.y, waypoint.z);
            else if (waypoint.dimension == RTDimension.OVERWORLD && currentDimension.getSkyProperties().equals(DimensionType.THE_NETHER_ID))
                RenderUtils.drawTextPlate(Arrays.asList(waypoint.name.split("\n")), waypoint.x / 8.0, 128, waypoint.z / 8.0);
        }
    }

    private static void setupBlend()
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
    }

    private static void color(float r, float g, float b, float a)
    {
        RenderSystem.setShaderColor(r, g, b, a);
    }

    private static void drawTextPlate(List<String> text, double x, double y, double z)
    {
        Entity entity = MinecraftClient.getInstance().getCameraEntity();

        if (entity != null)
        {
            RenderUtils.drawTextPlate(text, x, y, z, entity.getYaw(), entity.getPitch(), RenderUtils.white, RenderUtils.plateBackground);
        }
    }

    private static void drawTextPlate(List<String> text, double x, double y, double z, float yaw, float pitch,
                                      RGBAColor textColor, RGBAColor bgColor)
    {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double cx = cameraPos.x;
        double cy = cameraPos.y;
        double cz = cameraPos.z;
        double dx = x - cx;
        double dy = y - cy;
        double dz = z - cz;
        double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));
        text.set(text.size() - 1, text.get(text.size() - 1) + " (" + (int) distance + "m)");
        float scale;
        double realX;
        double realY;
        double realZ;
        double maxDistance = 10;
        float targetScale = .04f;

        if (distance > maxDistance) {
            Vec3d direction = new Vec3d(dx, dy, dz).normalize().multiply(maxDistance);
            realX = cx + direction.x;
            realY = cy + direction.y;
            realZ = cz + direction.z;
            scale = targetScale;
        } else {
            realX = x;
            realY = y;
            realZ = z;
            scale = (float) (distance * targetScale / maxDistance);
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MatrixStack globalStack = RenderSystem.getModelViewStack();

        globalStack.push();
        globalStack.translate(realX - cx, realY - cy, realZ - cz);

        Quaternion rot = Vec3f.POSITIVE_Y.getDegreesQuaternion(-yaw);
        rot.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(pitch));
        globalStack.multiply(rot);

        globalStack.scale(-scale, -scale, scale);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableCull();

        RenderUtils.setupBlend();
        RenderSystem.disableTexture();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int maxLineLen = 0;

        for (String line : text)
        {
            maxLineLen = Math.max(maxLineLen, textRenderer.getWidth(line));
        }

        int strLenHalf = maxLineLen / 2;
        int textHeight = textRenderer.fontHeight * text.size() - 1;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(-strLenHalf - 1,          -1, 0.0D).color(bgColor.r, bgColor.g, bgColor.b, bgColor.a).next();
        buffer.vertex(-strLenHalf - 1,  textHeight, 0.0D).color(bgColor.r, bgColor.g, bgColor.b, bgColor.a).next();
        buffer.vertex( strLenHalf    ,  textHeight, 0.0D).color(bgColor.r, bgColor.g, bgColor.b, bgColor.a).next();
        buffer.vertex( strLenHalf    ,          -1, 0.0D).color(bgColor.r, bgColor.g, bgColor.b, bgColor.a).next();
        tessellator.draw();

        RenderSystem.enableTexture();
        int textY = 0;
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.loadIdentity();

        for (String line : text)
        {
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(buffer);
            textRenderer.draw(line, -strLenHalf, textY, 0x20000000 | (textColor.toInt() & 0xFFFFFF), false, modelMatrix, immediate, true, 0, 15728880);
            immediate.draw();

            immediate = VertexConsumerProvider.immediate(buffer);
            textRenderer.draw(line, -strLenHalf, textY, textColor.toInt(), false, modelMatrix, immediate, true, 0, 15728880);
            immediate.draw();
            textY += textRenderer.fontHeight;
        }

        RenderUtils.color(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        globalStack.pop();
        RenderSystem.applyModelViewMatrix();
    }

}