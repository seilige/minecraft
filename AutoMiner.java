package com.example.examplemod;

import org.lwjgl.glfw.GLFW;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AutoMiner {
    private static final AutoMiner INSTANCE = new AutoMiner();
    private final Minecraft mc = Minecraft.getInstance();

    // Main state variables
    private boolean enabled = false; // Whether the AutoMiner is enabled
    private BlockPos currentTarget = null; // The current block being mined
    private boolean isActiveMining = false; // Whether mining is currently active
    private int miningTicks = 0; // Number of ticks spent mining the current block
    private int delayTicks = 0; // Delay between mining blocks
    private float expectedMiningTime = 0; // Expected time to mine the current block

    // Configuration settings
    private static final int SEARCH_RADIUS = 5; // Radius to search for blocks to mine
    private static final int DELAY_BETWEEN_BLOCKS = 5; // Delay in ticks between mining blocks

    private AutoMiner() {
    }

    public static AutoMiner getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Ensure the event is in the END phase, AutoMiner is enabled, and the player
        // and world are valid
        if (event.phase != TickEvent.Phase.END || !enabled || mc.player == null || mc.level == null) {
            return;
        }

        // Check if the player is connected to a server
        if (mc.getConnection() == null) {
            return;
        }

        // Handle delay between mining blocks
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        // Main mining logic
        if (currentTarget != null && isActiveMining) {
            processMining(); // Continue mining the current block
        } else {
            findAndStartNewTarget(); // Search for a new block to mine
        }
    }

    private void processMining() {
        World world = mc.level;
        BlockState currentState = world.getBlockState(currentTarget);

        // Check if the block has already been destroyed
        if (currentState.isAir(world, currentTarget)) {
            ExampleMod.LOGGER.info("Block destroyed successfully at: " + currentTarget);
            finishMining(); // Finish mining and reset state
            return;
        }

        miningTicks++;

        // Send a packet to simulate holding down the mining button
        sendContinuousDiggingPacket();

        // Check if the expected mining time has been reached
        if (miningTicks >= expectedMiningTime) {
            // Send a final packet to complete the mining process
            sendFinishDiggingPacket();

            // Allow a few extra ticks for the server to process the mining
            if (miningTicks >= expectedMiningTime + 3) {
                ExampleMod.LOGGER.info("Mining timeout reached for: " + currentTarget);
                finishMining();
            }
        }

        // Log mining progress every 20 ticks
        if (miningTicks % 20 == 0) {
            float progress = Math.min(1.0f, miningTicks / expectedMiningTime);
            ExampleMod.LOGGER.debug("Mining progress: " + String.format("%.1f%%", progress * 100) +
                    " (" + miningTicks + "/" + (int) expectedMiningTime + " ticks)");
        }
    }

    private void findAndStartNewTarget() {
        // Ensure the player and world are valid
        if (mc.player == null || mc.level == null)
            return;

        BlockPos playerPos = mc.player.blockPosition();
        World world = mc.level;

        // Search for the nearest suitable block to mine
        BlockPos bestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (isValidMiningTarget(pos)) {
                        double distance = playerPos.distSqr(pos);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            bestTarget = pos;
                        }
                    }
                }
            }
        }

        if (bestTarget != null) {
            startMiningBlock(bestTarget); // Start mining the selected block
        } else {
            // If no suitable blocks are found, wait before searching again
            delayTicks = 20; // 1 second delay
            ExampleMod.LOGGER.debug("No suitable blocks found, waiting...");
        }
    }

    private boolean isValidMiningTarget(BlockPos pos) {
        // Ensure the world is valid
        if (mc.level == null)
            return false;

        World world = mc.level;
        BlockState state = world.getBlockState(pos);

        // Check if the block is not air
        if (state.isAir(world, pos)) {
            return false;
        }

        // Get the block's hardness
        float hardness = state.getDestroySpeed(world, pos);

        // Ignore unbreakable blocks (e.g., bedrock)
        if (hardness < 0) {
            return false;
        }

        // Ignore blocks that are too hard (e.g., obsidian)
        if (hardness > 25.0f) {
            return false;
        }

        // Check if the player can mine the block
        if (mc.player != null) {
            float digSpeed = mc.player.getDigSpeed(state, pos);
            if (digSpeed <= 0) {
                return false;
            }
        }

        return true;
    }

    private void startMiningBlock(BlockPos pos) {
        currentTarget = pos;
        isActiveMining = true;
        miningTicks = 0;

        // Calculate the expected time to mine the block
        expectedMiningTime = calculateMiningTime(pos);

        ExampleMod.LOGGER.info("Started mining block at: " + pos +
                " (expected time: " + (int) expectedMiningTime + " ticks)");

        // Send the initial packet to start mining
        sendStartDiggingPacket(pos);
    }

    private float calculateMiningTime(BlockPos pos) {
        // Ensure the world and player are valid
        if (mc.level == null || mc.player == null) {
            return 100f; // Fallback value
        }

        BlockState state = mc.level.getBlockState(pos);
        float hardness = state.getDestroySpeed(mc.level, pos);

        if (hardness <= 0) {
            return 1f; // Minimum time
        }

        float digSpeed = mc.player.getDigSpeed(state, pos);
        if (digSpeed <= 0) {
            return 200f; // Very long time for unminable blocks
        }

        // Minecraft formula: time = (hardness / speed) * 20 + small buffer
        return Math.max(5f, (hardness / digSpeed) * 20f + 2f);
    }

    private void sendStartDiggingPacket(BlockPos pos) {
        // Ensure the player is connected to the server
        if (mc.getConnection() == null)
            return;

        Direction face = Direction.UP; // Direction of mining
        CPlayerDiggingPacket packet = new CPlayerDiggingPacket(
                CPlayerDiggingPacket.Action.START_DESTROY_BLOCK,
                pos,
                face);
        mc.getConnection().send(packet);

        ExampleMod.LOGGER.debug("Sent START_DESTROY_BLOCK packet for: " + pos);
    }

    private void sendContinuousDiggingPacket() {
        // Ensure the player is connected and there is a target block
        if (mc.getConnection() == null || currentTarget == null)
            return;

        // Send a packet every few ticks to simulate continuous mining
        if (miningTicks % 3 == 0) {
            Direction face = Direction.UP;
            CPlayerDiggingPacket packet = new CPlayerDiggingPacket(
                    CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK,
                    currentTarget,
                    face);
            mc.getConnection().send(packet);

            // Immediately send a START packet to simulate holding the button
            CPlayerDiggingPacket startPacket = new CPlayerDiggingPacket(
                    CPlayerDiggingPacket.Action.START_DESTROY_BLOCK,
                    currentTarget,
                    face);
            mc.getConnection().send(startPacket);
        }
    }

    private void sendFinishDiggingPacket() {
        // Ensure the player is connected and there is a target block
        if (mc.getConnection() == null || currentTarget == null)
            return;

        Direction face = Direction.UP;
        CPlayerDiggingPacket packet = new CPlayerDiggingPacket(
                CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK,
                currentTarget,
                face);
        mc.getConnection().send(packet);

        ExampleMod.LOGGER.debug("Sent STOP_DESTROY_BLOCK packet for: " + currentTarget);
    }

    private void finishMining() {
        // Log and reset mining state
        if (currentTarget != null) {
            ExampleMod.LOGGER.debug("Finished mining at: " + currentTarget);
        }

        currentTarget = null;
        isActiveMining = false;
        miningTicks = 0;
        expectedMiningTime = 0;
        delayTicks = DELAY_BETWEEN_BLOCKS; // Delay before mining the next block
    }

    public void toggle() {
        // Toggle the AutoMiner on or off
        enabled = !enabled;

        if (!enabled) {
            stopMining(); // Stop mining if disabled
            ExampleMod.LOGGER.info("AutoMiner disabled");
        } else {
            ExampleMod.LOGGER.info("AutoMiner enabled");
        }
    }

    public void stopMining() {
        // Stop mining and reset state
        if (isActiveMining && currentTarget != null) {
            // Send a packet to cancel mining
            if (mc.getConnection() != null) {
                Direction face = Direction.UP;
                CPlayerDiggingPacket packet = new CPlayerDiggingPacket(
                        CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK,
                        currentTarget,
                        face);
                mc.getConnection().send(packet);
            }
        }

        currentTarget = null;
        isActiveMining = false;
        miningTicks = 0;
        expectedMiningTime = 0;
        delayTicks = 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMining() {
        return isActiveMining;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Toggle AutoMiner when the "M" key is pressed
        if (event.getKey() == GLFW.GLFW_KEY_M && event.getAction() == GLFW.GLFW_PRESS) {
            toggle();
        }
    }
}