package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * FullBright class provides functionality to manipulate Minecraft's gamma
 * settings
 * to create a "fullbright" effect, allowing players to see clearly in dark
 * environments.
 * This is commonly used as a quality-of-life feature in Minecraft mods.
 */
public class FullBright {
    // Reference to the Minecraft client instance for accessing game settings and
    // state
    private final Minecraft mc = Minecraft.getInstance();

    // The standard gamma value that Minecraft uses by default (normal brightness)
    private static final double DEFAULT_GAMMA = 1.0;

    // Flag to track whether the fullbright feature is currently active
    private boolean isEnabled = false;

    /**
     * Constructor that registers this class with MinecraftForge's event bus
     * to receive and handle game events (specifically client tick events)
     */
    public FullBright() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Enables the fullbright effect by setting the gamma value to maximum (100.0)
     * This makes everything appear fully lit, eliminating darkness and shadows
     * Only activates if fullbright is not already enabled to prevent redundant
     * operations
     */
    public void enableFullBright() {
        if (!isEnabled) {
            // Set gamma to maximum value for fullbright effect
            mc.options.gamma = 100.0F;
            isEnabled = true;
        }
    }

    /**
     * Disables the fullbright effect by restoring the gamma value to its default
     * setting
     * Includes safety checks to ensure the options object exists before attempting
     * to modify it
     * Only executes if fullbright is currently enabled to prevent unnecessary
     * operations
     */
    public void disableFullBright() {
        if (isEnabled && mc.options != null) {
            // Always restore to the standard gamma value to ensure consistent lighting
            mc.options.gamma = DEFAULT_GAMMA;
            isEnabled = false;
        }
    }

    /**
     * Returns the current state of the fullbright feature
     * 
     * @return true if fullbright is currently enabled, false otherwise
     */
    public boolean isFullBrightEnabled() {
        return isEnabled;
    }

    /**
     * Toggles the fullbright feature on or off based on its current state
     * If currently enabled, it will be disabled; if disabled, it will be enabled
     * Provides a convenient single method for switching between states
     */
    public void toggle() {
        if (isEnabled) {
            disableFullBright();
        } else {
            enableFullBright();
        }
    }

    /**
     * Event handler that runs on every client tick (game frame)
     * Ensures the gamma setting remains at maximum when fullbright is enabled
     * This prevents other mods or game mechanics from overriding our gamma setting
     * 
     * @param event The client tick event containing phase information
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Only execute during the END phase of the tick and when player and world are
        // loaded
        if (mc.player != null && mc.level != null && event.phase == TickEvent.Phase.END) {
            if (isEnabled) {
                // Continuously enforce maximum gamma to maintain fullbright effect
                // This prevents other game systems from resetting our brightness setting
                mc.options.gamma = 100.0F;
            }
        }
    }
}