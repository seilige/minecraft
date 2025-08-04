package com.example.examplemod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * LaserNavigator class provides visual laser tracers to nearby entities in
 * Minecraft.
 * This mod feature draws colored lines from the player's eye position to
 * detected entities,
 * helping with navigation and entity tracking in the game world.
 */
public class LaserNavigator {
    private boolean isEnabled = false; // Current state of the LaserNavigator feature
    private final Minecraft mc = Minecraft.getInstance(); // Minecraft client instance

    /**
     * Constructor initializes the LaserNavigator and registers it to the
     * MinecraftForge event bus
     * to receive rendering events when the world is being drawn.
     */
    public LaserNavigator() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Returns the current enabled/disabled state of the LaserNavigator.
     * 
     * @return true if the laser navigator is currently active, false otherwise
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Enables the LaserNavigator feature, allowing laser tracers to be rendered.
     */
    public void enable() {
        isEnabled = true;
    }

    /**
     * Disables the LaserNavigator feature, stopping all laser tracer rendering.
     */
    public void disable() {
        isEnabled = false;
    }

    /**
     * Toggles the LaserNavigator state between enabled and disabled.
     * If currently enabled, it will be disabled, and vice versa.
     */
    public void toggle() {
        isEnabled = !isEnabled;
    }

    /**
     * Event handler that executes during the world rendering phase.
     * This method is called every frame after the world has been rendered but
     * before the GUI.
     * It draws laser tracers from the player to nearby entities if the feature is
     * enabled.
     * 
     * @param event The RenderWorldLastEvent containing rendering context and timing
     *              information
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Early return if LaserNavigator is disabled - no rendering needed
        if (!isEnabled) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        // Safety check: ensure player and world exist before attempting to render
        if (mc.player == null || mc.level == null) {
            return;
        }

        MatrixStack matrixStack = event.getMatrixStack();

        // Get camera position for proper world-to-screen coordinate transformation
        // This ensures lasers are rendered relative to the camera's current viewpoint
        double cameraX = mc.gameRenderer.getMainCamera().getPosition().x;
        double cameraY = mc.gameRenderer.getMainCamera().getPosition().y;
        double cameraZ = mc.gameRenderer.getMainCamera().getPosition().z;

        Vector3d cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // Calculate the laser starting point based on player's eye position and look
        // direction
        // This creates a slight offset from the player's exact eye position along their
        // view vector
        Vector3f lookVector = mc.gameRenderer.getMainCamera().getLookVector();
        Vector3d viewVector = new Vector3d(lookVector.x(), lookVector.y(), lookVector.z());
        Vector3d tracerStart = mc.player.getEyePosition(event.getPartialTicks()).add(viewVector.scale(1.0));

        // Debug output: count entities within detection range for troubleshooting
        int entityCount = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(20.0D),
                entity -> entity != mc.player && entity.isAlive()).size();
        System.out.println("Found entities: " + entityCount);

        // Find all living entities within a 50-block radius of the player
        // Filter out the player themselves and only target alive entities
        // For each detected entity, draw a laser tracer line
        mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(50.0D),
                entity -> entity != mc.player && entity.isAlive()).forEach(entity -> {
                    // Get the entity's interpolated position for smooth rendering during movement
                    Vector3d entityPos = entity.getPosition(event.getPartialTicks());
                    // Draw the actual laser line from player to this entity
                    drawLaser(matrixStack, tracerStart, entityPos, entity.getBbHeight(), cameraX, cameraY,
                            cameraZ);
                });
    }

    /**
     * Renders a colored laser line between two 3D points in the world.
     * The laser consists of a line from the starting point to the target entity's
     * center.
     * Uses OpenGL rendering with custom colors: red for entity end, green for
     * player end.
     * 
     * @param matrixStack  The transformation matrix stack for 3D positioning
     * @param start        The starting position of the laser (typically player's
     *                     eye position)
     * @param end          The ending position of the laser (target entity's
     *                     position)
     * @param entityHeight The height of the target entity (used to aim at center
     *                     mass)
     * @param cameraX      Camera's X coordinate for proper world-space translation
     * @param cameraY      Camera's Y coordinate for proper world-space translation
     * @param cameraZ      Camera's Z coordinate for proper world-space translation
     */
    private void drawLaser(MatrixStack matrixStack, Vector3d start, Vector3d end, float entityHeight, double cameraX,
            double cameraY, double cameraZ) {
        // Save current matrix state to restore later
        matrixStack.pushPose();
        // Translate coordinates to world space relative to camera position
        matrixStack.translate(-cameraX, -cameraY, -cameraZ);

        // Initialize OpenGL tessellator for custom geometry rendering
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        // Configure OpenGL render state for laser line drawing
        RenderSystem.disableTexture(); // Disable texture mapping for solid colors
        RenderSystem.enableBlend(); // Enable alpha blending for transparency
        RenderSystem.defaultBlendFunc(); // Use standard alpha blending function
        RenderSystem.disableDepthTest(); // Render through walls/blocks
        RenderSystem.lineWidth(3.0F); // Set laser line thickness to 3 pixels

        // Apply the current transformation matrix to OpenGL's matrix stack
        RenderSystem.multMatrix(matrixStack.last().pose());

        // Begin building line geometry with position and color data
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Define the laser line vertices with color gradients
        // First vertex: entity position (center mass) with red color and transparency
        buffer.vertex(end.x, end.y + entityHeight / 2, end.z).color(255, 0, 0, 200).endVertex();
        // Second vertex: player crosshair position with green color and transparency
        buffer.vertex(start.x, start.y, start.z).color(0, 255, 0, 200).endVertex();

        // Execute the render command to draw the line
        tessellator.end();

        // Restore previous OpenGL render state
        RenderSystem.enableDepthTest(); // Re-enable depth testing
        RenderSystem.disableBlend(); // Disable alpha blending
        RenderSystem.enableTexture(); // Re-enable texture mapping
        RenderSystem.loadIdentity(); // Reset transformation matrix

        // Restore previous matrix state
        matrixStack.popPose();
    }
}