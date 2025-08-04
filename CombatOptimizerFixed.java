package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CombatOptimizerFixed {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final int ATTACK_RADIUS = 4;

    // Create a custom key binding for toggling the combat optimizer
    private static final KeyBinding TOGGLE_COMBAT = new KeyBinding(
            "key.examplemod.toggle_combat", // Localization key for the key binding name
            GLFW.GLFW_KEY_R, // Default key is 'R' (can be changed to any other key)
            "key.categories.examplemod" // Category under which the key binding will appear in settings
    );

    private boolean combatEnabled = false; // Tracks whether the combat optimizer is enabled
    private int attackCooldown = 0; // Cooldown timer for attacks

    public CombatOptimizerFixed() {
        // Register this class to listen for events on the MinecraftForge event bus
        MinecraftForge.EVENT_BUS.register(this);
        // Register the custom key binding
        ClientRegistry.registerKeyBinding(TOGGLE_COMBAT);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Ensure the combat optimizer is enabled, the player and world are valid, and
        // the event is for the local player
        if (!combatEnabled || mc.player == null || mc.level == null || !event.player.equals(mc.player)) {
            return;
        }

        // Decrease the attack cooldown if it's greater than 0
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }

        // Find the best target to attack using ray tracing logic
        LivingEntity target = findBestTarget();

        // If a valid target is found and the player can attack, perform the attack
        if (target != null && canAttackNow()) {
            attackEntity(target);
            attackCooldown = calculateAttackCooldown(); // Set the cooldown based on the weapon/tool
        }
    }

    private LivingEntity findBestTarget() {
        // Get a list of entities within the attack radius
        List<Entity> entities = mc.level.getEntities(mc.player, new AxisAlignedBB(
                mc.player.getX() - ATTACK_RADIUS, mc.player.getY() - ATTACK_RADIUS, mc.player.getZ() - ATTACK_RADIUS,
                mc.player.getX() + ATTACK_RADIUS, mc.player.getY() + ATTACK_RADIUS, mc.player.getZ() + ATTACK_RADIUS));

        // Filter the entities to find the best target
        return entities.stream()
                .filter(entity -> entity instanceof LivingEntity) // Only consider living entities
                .filter(entity -> entity != mc.player) // Exclude the player themselves
                .filter(entity -> entity.distanceTo(mc.player) <= ATTACK_RADIUS) // Ensure the entity is within range
                .filter(this::canSeeTarget) // Check if the target is visible
                .map(entity -> (LivingEntity) entity) // Cast to LivingEntity
                .findFirst() // Return the first valid target
                .orElse(null); // Return null if no valid target is found
    }

    private boolean canSeeTarget(Entity target) {
        // Perform a simple visibility check using ray tracing
        Vector3d start = mc.player.getEyePosition(1.0F); // Player's eye position
        Vector3d end = target.getEyePosition(1.0F); // Target's eye position

        RayTraceResult result = mc.level.clip(new net.minecraft.util.math.RayTraceContext(
                start, end,
                net.minecraft.util.math.RayTraceContext.BlockMode.COLLIDER, // Check for blocks
                net.minecraft.util.math.RayTraceContext.FluidMode.NONE, // Ignore fluids
                mc.player)); // Exclude the player from the ray trace

        return result.getType() == RayTraceResult.Type.MISS; // Return true if no obstruction is found
    }

    public boolean isCombatEnabled() {
        // Return whether the combat optimizer is currently enabled
        return combatEnabled;
    }

    public void toggleCombat() {
        // Toggle the combat optimizer on or off
        combatEnabled = !combatEnabled;
    }

    private boolean canAttackNow() {
        // Get the item currently held in the player's main hand
        ItemStack heldItem = mc.player.getMainHandItem();

        if (heldItem.isEmpty()) {
            // If the player's hand is empty, allow frequent attacks
            return true;
        }

        Item item = heldItem.getItem();

        // For weapons and tools, check if the attack strength is fully charged
        if (isWeaponOrTool(item)) {
            return mc.player.getAttackStrengthScale(0.0F) >= 1.0F; // Ensure 100% readiness for full damage
        }

        return true; // Allow attacks for other items
    }

    private boolean isWeaponOrTool(Item item) {
        // Check if the item is a weapon or tool
        return item instanceof SwordItem ||
                item instanceof AxeItem ||
                item instanceof PickaxeItem ||
                item instanceof ShovelItem ||
                item instanceof HoeItem ||
                item instanceof TridentItem;
    }

    private int calculateAttackCooldown() {
        // Get the item currently held in the player's main hand
        ItemStack heldItem = mc.player.getMainHandItem();

        if (heldItem.isEmpty()) {
            return 2; // Short cooldown for empty hand attacks
        }

        Item item = heldItem.getItem();

        // Determine the cooldown based on the type of weapon/tool
        if (item instanceof SwordItem) {
            return 3; // Short cooldown for swords
        } else if (item instanceof AxeItem) {
            return 5; // Medium cooldown for axes
        } else if (item instanceof PickaxeItem || item instanceof ShovelItem) {
            return 4; // Cooldown for tools like pickaxes and shovels
        } else if (item instanceof TridentItem) {
            return 6; // Long cooldown for tridents
        }

        return 2; // Default cooldown for other items
    }

    private void attackEntity(LivingEntity target) {
        // Perform the attack if the player's attack strength is fully charged
        if (mc.player.getAttackStrengthScale(0.0F) >= 1.0F) {
            mc.gameMode.attack(mc.player, target); // Attack the target
            // The attack timer will reset automatically after calling attack()
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        // Check if the custom key binding was pressed
        if (TOGGLE_COMBAT.consumeClick()) {
            combatEnabled = !combatEnabled; // Toggle the combat optimizer

            // Display a message to the player indicating the current state
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        new StringTextComponent(combatEnabled ? "§a[Combat] Combat optimizer enabled"
                                : "§c[Combat] Combat optimizer disabled"),
                        true);
            }
        }
    }
}