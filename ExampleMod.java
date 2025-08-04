package com.example.examplemod;

// Import for combat optimization functionality (currently commented out)
// import com.CombatOptimizer;
import java.lang.reflect.Field;
import net.minecraftforge.client.event.InputEvent.MouseInputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Main mod class that serves as the entry point for the ExampleMod
 * This mod includes various utilities like FreeCam, AutoMiner, ESP, and ore
 * visualization
 */
@Mod("examplemod")
public class ExampleMod {
    // Logger instance for debugging and information output
    public static final Logger LOGGER = LogManager.getLogger();

    // Static reference to the Minecraft client instance for easy access
    private static final Minecraft mc = Minecraft.getInstance();

    // Module instances - each handles a specific functionality
    private final FreeCam freeCam = new FreeCam(); // Camera movement module
    private final AutoMiner autoMiner = AutoMiner.getInstance(); // Automated mining functionality
    private final FullBright fullBright = new FullBright(); // Full brightness/night vision module

    // ESP (Extra Sensory Perception) related variables
    private boolean espEnabled = false; // Toggle state for ESP functionality
    private final CombatOptimizerFixed combatOptimizer = new CombatOptimizerFixed(); // Combat enhancement module
    private final EntityESP entityESP = new EntityESP(); // Entity highlighting/outlining module
    private final LaserNavigator laserNavigator = new LaserNavigator(); // Navigation assistance module

    /**
     * Constructor - initializes the mod and registers event handlers
     * This method is called when the mod is loaded by Forge
     */
    public ExampleMod() {
        // Register this class to receive Forge events (like key inputs and rendering)
        MinecraftForge.EVENT_BUS.register(this);

        // Alternative instantiation methods (currently commented out)
        // These could be used for different initialization approaches
        // new EntityESP();
        // new LaserNavigator();
        // new FullBright();

        // Register specific modules that need to handle their own events
        MinecraftForge.EVENT_BUS.register(AutoMiner.getInstance()); // AutoMiner needs tick events
        MinecraftForge.EVENT_BUS.register(freeCam); // FreeCam needs input/render events
        MinecraftForge.EVENT_BUS.register(laserNavigator); // LaserNavigator needs render events

        // Alternative instantiation methods (currently commented out)
        // new CombatOptimizerFixed();
        // new FreeCam();
    }

    /**
     * Getter method for accessing the combat optimizer module
     * 
     * @return CombatOptimizerFixed instance for combat enhancements
     */
    public CombatOptimizerFixed getCombatOptimizer() {
        return combatOptimizer;
    }

    /**
     * Toggles the ESP (Extra Sensory Perception) functionality on/off
     * ESP allows players to see entities through walls and with special
     * highlighting
     */
    public void toggleESP() {
        espEnabled = !espEnabled; // Flip the boolean state
        entityESP.setEnabled(espEnabled); // Apply the new state to the ESP module
        // Log the current state for debugging purposes
        LOGGER.info("ESP " + (espEnabled ? "enabled" : "disabled"));
    }

    /**
     * Checks if ESP functionality is currently enabled
     * 
     * @return true if ESP is active, false otherwise
     */
    public boolean isESPEnabled() {
        return espEnabled;
    }

    /**
     * Getter method for accessing the laser navigator module
     * LaserNavigator provides visual pathfinding and navigation assistance
     * 
     * @return LaserNavigator instance for navigation features
     */
    public LaserNavigator getLaserNavigator() {
        return laserNavigator;
    }

    /**
     * Getter method for accessing the auto miner module
     * 
     * @return AutoMiner instance for automated mining functionality
     */
    public AutoMiner getAutoMiner() {
        return autoMiner;
    }

    /**
     * Getter method for accessing the full bright module
     * 
     * @return FullBright instance for brightness/gamma manipulation
     */
    public FullBright getFullBright() {
        return fullBright;
    }

    /**
     * Event handler for keyboard input events
     * Currently configured to open a function menu when Right Shift is pressed
     * 
     * @param event The keyboard input event containing key and action information
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Check if Right Shift key was pressed (not held or released)
        if (event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT && event.getAction() == GLFW.GLFW_PRESS) {
            // Open the function menu screen, passing this mod instance for access to
            // modules
            mc.setScreen(new FunctionMenuScreen(this));
        }
    }

    /**
     * Getter method for accessing the free camera module
     * 
     * @return FreeCam instance for spectator-like camera movement
     */
    public FreeCam getFreeCam() {
        return freeCam;
    }

    /**
     * Event handler for world rendering - called after all world geometry is
     * rendered
     * This is where we implement ore ESP/X-ray functionality by scanning and
     * highlighting ores
     * 
     * @param event The render event containing matrix stack for transformations
     */
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        // Safety check: ensure player and world exist before proceeding
        if (mc.player == null || mc.level == null)
            return;

        // Get references to the current world and player position
        World world = mc.level;
        BlockPos playerPos = mc.player.blockPosition();

        // Scan the area around the player for valuable ore blocks
        List<BlockPos> orePositions = scanForOres(world, playerPos);

        // Debug logging to track ore detection performance
        LOGGER.info("Scanning for ores...");
        LOGGER.info("Found ores: " + orePositions.size());

        // Render visual indicators for all detected ores
        renderOres(orePositions, event.getMatrixStack());
    }

    /**
     * Scans a cubic area around the player for valuable ore blocks
     * This method implements a basic X-ray/ore finder functionality
     * 
     * @param world  The current world instance to scan blocks in
     * @param center The center position (usually player position) to scan around
     * @return List of BlockPos containing positions of all found ore blocks
     */
    private List<BlockPos> scanForOres(World world, BlockPos center) {
        List<BlockPos> orePositions = new ArrayList<>();
        int radius = 16; // Scanning radius in blocks (equivalent to 1 chunk radius)

        // Triple nested loop to scan a cubic area around the center point
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Calculate the absolute position by offsetting from center
                    BlockPos pos = center.offset(x, y, z);

                    // Check if the block at this position is a valuable ore
                    if (isOreBlock(world, pos)) {
                        orePositions.add(pos); // Add to our list of ores to highlight
                    }
                }
            }
        }
        return orePositions;
    }

    /**
     * Determines if a block at the given position is considered a valuable ore
     * Currently detects: Diamond Ore, Emerald Ore, and Nether Quartz Ore
     * 
     * @param world The world instance to check the block in
     * @param pos   The position to check for ore blocks
     * @return true if the block is a valuable ore, false otherwise
     */
    private boolean isOreBlock(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.DIAMOND_ORE || // Diamond ore (ID 56)
                world.getBlockState(pos).getBlock() == Blocks.EMERALD_ORE || // Emerald ore (ID 129)
                world.getBlockState(pos).getBlock() == Blocks.NETHER_QUARTZ_ORE; // Nether quartz ore (ID 153)
    }

    /**
     * Renders wireframe boxes around all detected ore positions
     * This method uses OpenGL to draw red wireframe cubes highlighting ore
     * locations
     * 
     * @param orePositions List of positions where ores were detected
     * @param matrixStack  Matrix stack for 3D transformations and positioning
     */
    private void renderOres(List<BlockPos> orePositions, MatrixStack matrixStack) {
        // Get the current camera position for proper world-to-screen transformation
        double cameraX = mc.gameRenderer.getMainCamera().getPosition().x;
        double cameraY = mc.gameRenderer.getMainCamera().getPosition().y;
        double cameraZ = mc.gameRenderer.getMainCamera().getPosition().z;

        // Push current matrix state to restore later
        matrixStack.pushPose();
        // Translate the rendering origin to account for camera offset
        matrixStack.translate(-cameraX, -cameraY, -cameraZ);

        // Configure OpenGL rendering settings for wireframe boxes
        RenderSystem.disableDepthTest(); // Allow rendering through walls (X-ray effect)
        RenderSystem.disableTexture(); // Disable texture rendering for solid colors
        RenderSystem.lineWidth(2.0F); // Set line thickness for better visibility

        // Apply our matrix transformations to the OpenGL pipeline
        RenderSystem.multMatrix(matrixStack.last().pose());

        // Begin drawing lines for wireframe boxes
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor3f(1.0F, 0.0F, 0.0F); // Set color to red (RGB: 1,0,0)

        // Draw a wireframe cube for each ore position
        for (BlockPos pos : orePositions) {
            float x = pos.getX();
            float y = pos.getY();
            float z = pos.getZ();

            // Draw bottom face of the cube (4 lines forming a square)
            GL11.glVertex3f(x, y, z); // Bottom-left-front to bottom-right-front
            GL11.glVertex3f(x + 1, y, z);

            GL11.glVertex3f(x + 1, y, z); // Bottom-right-front to bottom-right-back
            GL11.glVertex3f(x + 1, y, z + 1);

            GL11.glVertex3f(x + 1, y, z + 1); // Bottom-right-back to bottom-left-back
            GL11.glVertex3f(x, y, z + 1);

            GL11.glVertex3f(x, y, z + 1); // Bottom-left-back to bottom-left-front
            GL11.glVertex3f(x, y, z);

            // Draw top face of the cube (4 lines forming a square)
            GL11.glVertex3f(x, y + 1, z); // Top-left-front to top-right-front
            GL11.glVertex3f(x + 1, y + 1, z);

            GL11.glVertex3f(x + 1, y + 1, z); // Top-right-front to top-right-back
            GL11.glVertex3f(x + 1, y + 1, z + 1);

            GL11.glVertex3f(x + 1, y + 1, z + 1); // Top-right-back to top-left-back
            GL11.glVertex3f(x, y + 1, z + 1);

            GL11.glVertex3f(x, y + 1, z + 1); // Top-left-back to top-left-front
            GL11.glVertex3f(x, y + 1, z);

            // Draw vertical edges connecting top and bottom faces (4 lines)
            GL11.glVertex3f(x, y, z); // Front-left edge
            GL11.glVertex3f(x, y + 1, z);

            GL11.glVertex3f(x + 1, y, z); // Front-right edge
            GL11.glVertex3f(x + 1, y + 1, z);

            GL11.glVertex3f(x + 1, y, z + 1); // Back-right edge
            GL11.glVertex3f(x + 1, y + 1, z + 1);

            GL11.glVertex3f(x, y, z + 1); // Back-left edge
            GL11.glVertex3f(x, y + 1, z + 1);
        }

        // Finish drawing lines
        GL11.glEnd();

        // Restore previous OpenGL rendering state
        RenderSystem.enableTexture(); // Re-enable texture rendering
        RenderSystem.enableDepthTest(); // Re-enable depth testing
        RenderSystem.loadIdentity(); // Reset transformation matrix
        matrixStack.popPose(); // Restore previous matrix state
    }
}