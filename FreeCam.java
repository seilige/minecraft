package com.example.examplemod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.MathHelper;

/**
 * FreeCam class implements a spectator-like camera mode that allows the player
 * to move the camera freely through the world while keeping their actual player
 * position fixed at the original location.
 */
public class FreeCam {
    // Stores the real world position of the player while FreeCam is active
    private Vector3d realPlayerPos;

    // Camera render position coordinates - where the camera is visually positioned
    private double renderX, renderY, renderZ;

    // Camera render rotation angles - yaw (horizontal) and pitch (vertical)
    // rotation
    private float renderYaw, renderPitch;

    // Minecraft client instance for accessing game state and player data
    private static final Minecraft mc = Minecraft.getInstance();

    // Flag indicating whether FreeCam mode is currently active
    private boolean enabled = false;

    // Original player position before FreeCam was activated - used for restoration
    private Vector3d originalPlayerPos;

    // Original player rotation angles before FreeCam was activated
    private float originalPlayerYaw, originalPlayerPitch;

    // Original player's noClip/physics state before FreeCam was activated
    private boolean originalNoClip;

    // Current position of the free-moving camera in 3D space
    private Vector3d freeCamPos;

    // Current rotation angles of the free-moving camera
    private float freeCamYaw, freeCamPitch;

    // Stored real player position for maintaining player's actual world location
    private Vector3d storedPlayerPos;

    /**
     * Returns whether FreeCam mode is currently enabled
     * 
     * @return true if FreeCam is active, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Constructor that registers this class to listen for Minecraft Forge events
     */
    public FreeCam() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Event handler for keyboard input - toggles FreeCam when F6 key is pressed
     * 
     * @param event The keyboard input event containing key press information
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Check if F6 key was pressed (not held or released)
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_F6) {
            toggle();
        }
    }

    /**
     * Toggles FreeCam mode on/off, saving and restoring player state as needed
     */
    public void toggle() {
        // Don't do anything if player doesn't exist
        if (mc.player == null)
            return;

        // Flip the enabled state
        enabled = !enabled;

        if (enabled) {
            // === ENTERING FREECAM MODE ===

            // Save the player's current state so we can restore it later
            originalPlayerPos = mc.player.position();
            originalPlayerYaw = mc.player.yRot;
            originalPlayerPitch = mc.player.xRot;
            originalNoClip = mc.player.noPhysics;

            // Store the player's real world position for tracking
            storedPlayerPos = new Vector3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            realPlayerPos = storedPlayerPos;

            // Initialize the free camera at the player's eye level position
            freeCamPos = mc.player.getEyePosition(1.0f);
            freeCamYaw = mc.player.yRot;
            freeCamPitch = mc.player.xRot;

            // Record the starting player position for reference
            lastPlayerX = mc.player.getX();
            lastPlayerY = mc.player.getY();
            lastPlayerZ = mc.player.getZ();

            // Enable no-clip physics and stop all player movement
            mc.player.noPhysics = true;
            mc.player.setDeltaMovement(Vector3d.ZERO);
        } else {
            // === EXITING FREECAM MODE ===

            // Restore the player to their original state and position
            // Important: restore to realPlayerPos (actual player location), not freecam
            // position
            mc.player.noPhysics = originalNoClip;
            mc.player.setPos(realPlayerPos.x, realPlayerPos.y, realPlayerPos.z);
            mc.player.yRot = originalPlayerYaw;
            mc.player.xRot = originalPlayerPitch;

            // Reset fall distance to prevent fall damage when exiting FreeCam
            mc.player.fallDistance = 0;
        }
    }

    /**
     * Event handler called every client tick - manages FreeCam movement and player
     * state
     * 
     * @param event The client tick event
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Only process during END phase, when FreeCam is enabled, and player exists
        if (event.phase != TickEvent.Phase.END || !enabled || mc.player == null)
            return;

        // Update the free camera's position based on player input
        updateFreeCamMovement();

        // Keep the player frozen at their original location
        // Note: These lines are commented out to prevent player entity from moving
        // mc.player.setPos(freeCamPos.x, freeCamPos.y, freeCamPos.z);
        // mc.player.yRot = freeCamYaw;
        // mc.player.xRot = freeCamPitch;

        // Ensure player remains stationary and has no physics interactions
        mc.player.setDeltaMovement(Vector3d.ZERO); // Stop all movement
        mc.player.noPhysics = true; // Disable collision detection
        mc.player.fallDistance = 0; // Prevent fall damage accumulation
    }

    // Variables to track the player's last known position
    private double lastPlayerX, lastPlayerY, lastPlayerZ;

    /**
     * Updates the free camera's position based on current keyboard input
     * Handles WASD movement, space/shift for vertical movement, and sprint for
     * speed boost
     */
    private void updateFreeCamMovement() {
        // Safety check - ensure game options are available
        if (mc.options == null)
            return;

        // Sync camera rotation with player's current look direction
        if (mc.player != null) {
            freeCamYaw = mc.player.yRot;
            freeCamPitch = mc.player.xRot;
        }

        // Calculate directional vectors based on current camera rotation
        Vector3d forward = getForwardVector(); // Direction camera is looking
        Vector3d right = getRightVector(); // Direction to the right of camera
        Vector3d up = new Vector3d(0, 1, 0); // Straight up (Y-axis)

        // Initialize movement vector and base movement speed
        Vector3d movement = Vector3d.ZERO;
        float speed = 0.3f;

        // Apply speed boost when sprint key is held
        if (mc.options.keySprint.isDown()) {
            speed *= 3.0f;
        }

        // Handle movement input for each direction:

        // Forward movement (W key)
        if (mc.options.keyUp.isDown()) {
            movement = movement.add(forward.scale(speed));
        }

        // Backward movement (S key)
        if (mc.options.keyDown.isDown()) {
            movement = movement.add(forward.scale(-speed));
        }

        // Left strafe movement (A key)
        if (mc.options.keyLeft.isDown()) {
            movement = movement.add(right.scale(-speed));
        }

        // Right strafe movement (D key)
        if (mc.options.keyRight.isDown()) {
            movement = movement.add(right.scale(speed));
        }

        // Upward movement (Spacebar)
        if (mc.options.keyJump.isDown()) {
            movement = movement.add(up.scale(speed));
        }

        // Downward movement (Shift key)
        if (mc.options.keyShift.isDown()) {
            movement = movement.add(up.scale(-speed));
        }

        // Apply the calculated movement to update camera position
        freeCamPos = freeCamPos.add(movement);
    }

    /**
     * Calculates the forward direction vector based on current camera rotation
     * 
     * @return Vector3d representing the direction the camera is facing
     */
    private Vector3d getForwardVector() {
        // Convert rotation angles from degrees to radians for trigonometric functions
        float yawRad = (float) Math.toRadians(freeCamYaw);
        float pitchRad = (float) Math.toRadians(freeCamPitch);

        // Calculate 3D direction components using spherical coordinate conversion
        double x = -Math.sin(yawRad) * Math.cos(pitchRad); // East-West component
        double y = -Math.sin(pitchRad); // Up-Down component
        double z = Math.cos(yawRad) * Math.cos(pitchRad); // North-South component

        return new Vector3d(x, y, z);
    }

    /**
     * Calculates the right direction vector (90 degrees from forward direction)
     * 
     * @return Vector3d representing the direction to the right of the camera
     */
    private Vector3d getRightVector() {
        // Add 90 degrees to yaw to get perpendicular (right) direction
        float yawRad = (float) Math.toRadians(freeCamYaw + 90);

        // Right vector only needs horizontal components (no vertical movement)
        return new Vector3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
    }

    /**
     * Event handler for camera setup - redirects the game camera to the FreeCam
     * position
     * Uses reflection to access private camera methods in Minecraft 1.16.5
     * 
     * @param event The camera setup event containing camera transformation data
     */
    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        // Only modify camera when FreeCam is active and player exists
        if (!enabled || mc.player == null)
            return;

        // Use Java reflection to access private camera methods (required for 1.16.5)
        try {
            // Get the main camera renderer object
            ActiveRenderInfo camera = mc.gameRenderer.getMainCamera();

            // Access the private setPosition method to change camera location
            java.lang.reflect.Method setPositionMethod = ActiveRenderInfo.class.getDeclaredMethod("setPosition",
                    double.class, double.class, double.class);
            setPositionMethod.setAccessible(true);
            setPositionMethod.invoke(camera, freeCamPos.x, freeCamPos.y, freeCamPos.z);

            // Access and modify the private yaw rotation field
            java.lang.reflect.Field yawField = ActiveRenderInfo.class.getDeclaredField("yRot");
            yawField.setAccessible(true);
            yawField.set(camera, freeCamYaw);

            // Access and modify the private pitch rotation field
            java.lang.reflect.Field pitchField = ActiveRenderInfo.class.getDeclaredField("xRot");
            pitchField.setAccessible(true);
            pitchField.set(camera, freeCamPitch);

        } catch (Exception e) {
            // Fallback method if reflection fails - use event setters instead
            // This approach may not work as reliably but provides a backup option
            event.setYaw(freeCamYaw);
            event.setPitch(freeCamPitch);
        }
    }
}