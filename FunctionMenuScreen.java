package com.example.examplemod;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayList;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

/**
 * FunctionMenuScreen class extends the base Screen class to create a custom GUI
 * that displays a menu with toggleable mod functions. This screen serves as the
 * main interface for users to enable/disable various mod features like
 * AutoMiner,
 * FreeCam, FullBright, etc.
 */
public class FunctionMenuScreen extends Screen {
        private final ExampleMod mod;
        private int currentPage = 0;
        private final int buttonsPerPage = 5; // Maximum number of buttons to display per page
        private final List<Button> functionButtons = new ArrayList<>();
        private int totalPages;

        /**
         * Constructor for FunctionMenuScreen
         * 
         * @param mod The main mod instance that contains all the toggleable features
         */
        public FunctionMenuScreen(ExampleMod mod) {
                super(new StringTextComponent("Function Menu"));
                this.mod = mod;
                // Calculate total pages needed based on number of function buttons and buttons
                // per page
                this.totalPages = (int) Math.ceil((double) functionButtons.size() / buttonsPerPage);
        }

        /**
         * Initialize method called when the screen is first displayed.
         * This method sets up all the UI elements including buttons for each mod
         * function.
         * All buttons are positioned relative to the center of the screen.
         */
        @Override
        protected void init() {
                // Calculate center coordinates for button positioning
                int centerX = this.width / 2;
                int centerY = this.height / 2;
                int buttonWidth = 120; // Button width reduced from 150 to 120 for better layout
                int buttonHeight = 18; // Button height reduced from 20 to 18 for compact design
                int spacing = 22; // Vertical spacing between buttons, reduced from 25 to 22

                // AutoMiner toggle button - positioned 3 spaces above center
                // Displays current state (ON/OFF) and toggles the AutoMiner functionality when
                // clicked
                this.addButton(new Button(centerX - buttonWidth / 2, centerY - spacing * 3, buttonWidth, buttonHeight,
                                new StringTextComponent(
                                                "AutoMiner: " + (mod.getAutoMiner().isEnabled() ? "ON" : "OFF")),
                                button -> {
                                        mod.getAutoMiner().toggle();
                                        button.setMessage(
                                                        new StringTextComponent("AutoMiner: "
                                                                        + (mod.getAutoMiner().isEnabled() ? "ON"
                                                                                        : "OFF")));
                                }));

                // FreeCam toggle button - positioned 2 spaces above center
                // Allows players to move the camera freely without moving the player character
                this.addButton(new Button(centerX - buttonWidth / 2, centerY - spacing * 2, buttonWidth, buttonHeight,
                                new StringTextComponent("FreeCam: " + (mod.getFreeCam().isEnabled() ? "ON" : "OFF")),
                                button -> {
                                        mod.getFreeCam().toggle();
                                        button.setMessage(
                                                        new StringTextComponent("FreeCam: "
                                                                        + (mod.getFreeCam().isEnabled() ? "ON"
                                                                                        : "OFF")));
                                }));

                // FullBright toggle button - positioned 1 space above center
                // Removes darkness and makes everything fully lit for better visibility
                this.addButton(new Button(centerX - buttonWidth / 2, centerY - spacing, buttonWidth, buttonHeight,
                                new StringTextComponent("FullBright: "
                                                + (mod.getFullBright().isFullBrightEnabled() ? "ON" : "OFF")),
                                button -> {
                                        mod.getFullBright().toggle();
                                        button.setMessage(new StringTextComponent(
                                                        "FullBright: " + (mod.getFullBright().isFullBrightEnabled()
                                                                        ? "ON"
                                                                        : "OFF")));
                                }));

                // LaserNavigator toggle button - positioned at center
                // Provides laser-based navigation assistance for pathfinding
                this.addButton(new Button(centerX - buttonWidth / 2, centerY, buttonWidth, buttonHeight,
                                new StringTextComponent("LaserNavigator: "
                                                + (mod.getLaserNavigator().isEnabled() ? "ON" : "OFF")),
                                button -> {
                                        mod.getLaserNavigator().toggle();
                                        button.setMessage(new StringTextComponent(
                                                        "LaserNavigator: " + (mod.getLaserNavigator().isEnabled() ? "ON"
                                                                        : "OFF")));
                                }));

                // ESP (Extra Sensory Perception) toggle button - positioned 1 space below
                // center
                // Highlights entities, blocks, or other objects through walls for enhanced
                // visibility
                this.addButton(new Button(centerX - buttonWidth / 2, centerY + spacing, buttonWidth, buttonHeight,
                                new StringTextComponent("ESP: " + (mod.isESPEnabled() ? "ON" : "OFF")),
                                button -> {
                                        mod.toggleESP();
                                        button.setMessage(new StringTextComponent(
                                                        "ESP: " + (mod.isESPEnabled() ? "ON" : "OFF")));
                                }));

                // CombatOptimizer toggle button - positioned 2 spaces below center
                // Optimizes combat mechanics for improved PvP performance
                this.addButton(new Button(centerX - buttonWidth / 2, centerY + spacing * 2, buttonWidth, buttonHeight,
                                new StringTextComponent(
                                                "CombatOptimizer: " + (mod.getCombatOptimizer().isCombatEnabled() ? "ON"
                                                                : "OFF")),
                                button -> {
                                        mod.getCombatOptimizer().toggleCombat();
                                        button.setMessage(new StringTextComponent(
                                                        "CombatOptimizer: "
                                                                        + (mod.getCombatOptimizer().isCombatEnabled()
                                                                                        ? "ON"
                                                                                        : "OFF")));
                                }));
        }

        /**
         * Main rendering method called every frame to draw the screen contents.
         * Renders the background, screen title, and all UI elements.
         * 
         * @param matrixStack  The transformation matrix stack for rendering
         * @param mouseX       Current X position of the mouse cursor
         * @param mouseY       Current Y position of the mouse cursor
         * @param partialTicks Partial tick time for smooth animations (0.0 to 1.0)
         */
        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
                // Render the standard dark background overlay
                this.renderBackground(matrixStack);
                // Draw the screen title centered at the top of the screen in white color
                drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, 20, 0xFFFFFF);
                // Render all child elements (buttons, widgets, etc.)
                super.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        /**
         * Determines whether the game should be paused when this screen is open.
         * Returns false to keep the game running in the background, allowing
         * real-time interaction with mod features while the menu is displayed.
         * 
         * @return false to prevent game pausing
         */
        @Override
        public boolean isPauseScreen() {
                return false;
        }
}