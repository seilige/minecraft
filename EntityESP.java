package com.example.examplemod;

import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class EntityESP {
    public EntityESP() {
        // Register this class to listen for Forge events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private boolean enabled = false;

    public void setEnabled(boolean enabled) {
        // Enable or disable the ESP functionality
        this.enabled = enabled;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Trigger the ESP rendering logic if it is enabled
        if (enabled) {
            renderEntityESP(event);
        }
    }

    private void renderEntityESP(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        // Ensure the player and world are not null before proceeding
        if (mc.player == null || mc.level == null)
            return;

        MatrixStack matrixStack = event.getMatrixStack();
        // Get the camera position for rendering relative to the player's view
        Vector3d cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // Retrieve all living entities within a 100-block radius, excluding the player
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class,
                mc.player.getBoundingBox().inflate(100), // Reduced radius for performance stability
                livingEntity -> livingEntity != mc.player && livingEntity.isAlive())) {

            // Render the entity's outline
            drawEntityOutline(entity, matrixStack, cameraPos, mc);
            // Render additional information about the entity
            renderEntityInfo(entity, matrixStack, cameraPos, mc);
        }
    }

    private void drawEntityOutline(LivingEntity entity, MatrixStack matrixStack, Vector3d cameraPos, Minecraft mc) {
        // Get the bounding box of the entity
        AxisAlignedBB boundingBox = entity.getBoundingBox();

        matrixStack.pushPose();
        // Translate the matrix stack to align with the camera's position
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Configure rendering settings for drawing the outline
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrixStack.last().pose());

        RenderSystem.disableDepthTest(); // Disable depth testing to ensure the outline is always visible
        RenderSystem.disableTexture(); // Disable textures for pure color rendering
        RenderSystem.enableBlend(); // Enable blending for transparency
        RenderSystem.defaultBlendFunc(); // Use default blending function
        RenderSystem.disableCull(); // Disable face culling to render all sides of the box
        RenderSystem.lineWidth(3.0F); // Set the line width for the outline

        // Extract the bounding box coordinates
        double minX = boundingBox.minX;
        double minY = boundingBox.minY;
        double minZ = boundingBox.minZ;
        double maxX = boundingBox.maxX;
        double maxY = boundingBox.maxY;
        double maxZ = boundingBox.maxZ;

        // Use Tessellator to draw the outline instead of direct GL11 calls
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuilder();

        // Set the outline color based on the entity type
        if (entity instanceof net.minecraft.entity.monster.MonsterEntity) {
            RenderSystem.color4f(1.0F, 0.0F, 0.0F, 0.8F); // Red for hostile entities
        } else {
            RenderSystem.color4f(0.0F, 1.0F, 0.0F, 0.8F); // Green for passive entities
        }

        buffer.begin(GL11.GL_LINES, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION);

        // Draw the bottom edges of the bounding box
        buffer.vertex(minX, minY, minZ).endVertex();
        buffer.vertex(maxX, minY, minZ).endVertex();
        buffer.vertex(maxX, minY, minZ).endVertex();
        buffer.vertex(maxX, minY, maxZ).endVertex();
        buffer.vertex(maxX, minY, maxZ).endVertex();
        buffer.vertex(minX, minY, maxZ).endVertex();
        buffer.vertex(minX, minY, maxZ).endVertex();
        buffer.vertex(minX, minY, minZ).endVertex();

        // Draw the top edges of the bounding box
        buffer.vertex(minX, maxY, minZ).endVertex();
        buffer.vertex(maxX, maxY, minZ).endVertex();
        buffer.vertex(maxX, maxY, minZ).endVertex();
        buffer.vertex(maxX, maxY, maxZ).endVertex();
        buffer.vertex(maxX, maxY, maxZ).endVertex();
        buffer.vertex(minX, maxY, maxZ).endVertex();
        buffer.vertex(minX, maxY, maxZ).endVertex();
        buffer.vertex(minX, maxY, minZ).endVertex();

        // Draw the vertical edges of the bounding box
        buffer.vertex(minX, minY, minZ).endVertex();
        buffer.vertex(minX, maxY, minZ).endVertex();
        buffer.vertex(maxX, minY, minZ).endVertex();
        buffer.vertex(maxX, maxY, minZ).endVertex();
        buffer.vertex(maxX, minY, maxZ).endVertex();
        buffer.vertex(maxX, maxY, maxZ).endVertex();
        buffer.vertex(minX, minY, maxZ).endVertex();
        buffer.vertex(minX, maxY, maxZ).endVertex();

        tessellator.end();

        // Restore rendering settings
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();

        matrixStack.popPose();
    }

    private void renderEntityInfo(LivingEntity entity, MatrixStack matrixStack, Vector3d cameraPos, Minecraft mc) {
        // Calculate the distance between the player and the entity
        double distance = mc.player.distanceTo(entity);
        // Get the entity's display name
        String entityName = entity.getDisplayName().getString();
        // Format the information string to display name, health, and distance
        String info = String.format("%s | ❤%.1f | %.1fm", entityName, entity.getHealth(), distance);

        matrixStack.pushPose();
        // Translate the matrix stack to position the text above the entity
        matrixStack.translate(
                entity.getX() - cameraPos.x,
                entity.getY() + entity.getBbHeight() + 0.5 - cameraPos.y,
                entity.getZ() - cameraPos.z);

        // Rotate the text to face the camera
        matrixStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        // Scale the text to make it readable
        matrixStack.scale(-0.025F, -0.025F, 0.025F);

        RenderSystem.disableDepthTest(); // Ensure the text is always visible
        RenderSystem.enableTexture(); // Enable textures for text rendering
        RenderSystem.enableBlend(); // Enable blending for transparency
        RenderSystem.defaultBlendFunc(); // Use default blending function

        int textWidth = mc.font.width(info);

        // Draw the background rectangle behind the text
        drawBackground(matrixStack, -textWidth / 2 - 2, -9, textWidth / 2 + 2, 0);

        // Draw the text itself
        mc.font.draw(matrixStack, info, -textWidth / 2, -8, 0xFFFFFF);

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest(); // Re-enable depth testing
        matrixStack.popPose();
    }

    private void drawBackground(MatrixStack matrixStack, int x1, int y1, int x2, int y2) {
        RenderSystem.disableTexture(); // Disable textures for pure color rendering

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        // Set the background color to semi-transparent black
        RenderSystem.color4f(0.0F, 0.0F, 0.0F, 0.5F);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        // Draw a rectangle using the provided coordinates
        buffer.vertex(x1, y2, 0).endVertex();
        buffer.vertex(x2, y2, 0).endVertex();
        buffer.vertex(x2, y1, 0).endVertex();
        buffer.vertex(x1, y1, 0).endVertex();
        tessellator.end();

        RenderSystem.enableTexture(); // Re-enable textures
    }
}