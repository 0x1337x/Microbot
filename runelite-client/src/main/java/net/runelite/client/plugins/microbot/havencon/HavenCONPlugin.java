package net.runelite.client.plugins.microbot.havencon;

import net.runelite.api.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;

import javax.inject.Inject;

import static java.awt.event.KeyEvent.VK_ENTER;

public class HavenCONPlugin {

    @Inject
    private HavenCONConfig config;

    private boolean running = false;

    public void start() {
        running = true;
        System.out.println("HavenCON script started.");
    }

    public void stop() {
        running = false;
        System.out.println("HavenCON script stopped.");
    }

    public void onGameTick() {
        if (!running) return;

        System.out.println("Executing HavenCON onGameTick...");

        if (exchangePlanks() && enterBuildMode() && buildLarder()) {
            System.out.println("Completed one cycle successfully.");
        } else {
            System.out.println("A task failed. Re-evaluating...");
        }
    }

    private boolean exchangePlanks() {
        System.out.println("Attempting to exchange planks...");
        NPC phials = Rs2Npc.getNpc("Phials");
        if (phials == null) {
            System.out.println("Phials not found!");
            Rs2Antiban.actionCooldown();
            return false;
        }

        Rs2Inventory.useItemOnNpc(config.plankId(), phials);
        Rs2Antiban.actionCooldown();

        if (Rs2Dialogue.isInDialogue() && Rs2Dialogue.hasSelectAnOption()) {
            System.out.println("Dialogue detected. Selecting 'Exchange X'...");
            Rs2Dialogue.clickOption("Exchange X");

            Global.awaitExecutionUntil(
                    () -> System.out.println("Dialogue option detected."),
                    () -> Rs2Dialogue.hasDialogueOption("Enter amount"),
                    5000
            );

            Rs2Keyboard.typeString(String.valueOf(config.exchangeAmount()));
            Rs2Keyboard.keyPress(VK_ENTER);
            Rs2Antiban.actionCooldown();
            return true;
        }

        System.out.println("Failed to complete plank exchange.");
        return false;
    }

    private boolean enterBuildMode() {
        System.out.println("Attempting to enter build mode...");
        GameObject portal = (GameObject) Rs2GameObject.findObjectById(4525);
        if (portal == null) {
            System.out.println("Portal not found!");
            Rs2Antiban.actionCooldown();
            return false;
        }

        selectOption("Build Mode", "Portal", portal);
        Rs2Antiban.actionCooldown();
        return true;
    }

    private boolean buildLarder() {
        System.out.println("Attempting to build oak larder...");
        GameObject larderSpace = (GameObject) Rs2GameObject.findObjectById(12345);
        if (larderSpace == null) {
            System.out.println("Larder space not found!");
            Rs2Antiban.actionCooldown();
            return false;
        }

        selectOption("Build", "Larder space", larderSpace);
        Rs2Antiban.actionCooldown();
        selectOption("Oak larder", "Larder space", larderSpace);
        Rs2Antiban.actionCooldown();
        return true;
    }

    private void selectOption(String option, String target, GameObject gameObject) {
        Client client = Microbot.getClient();

        NewMenuEntry menuEntry = new NewMenuEntry(
                gameObject.getWorldLocation().getX(),
                gameObject.getWorldLocation().getY(),
                MenuAction.WIDGET_TARGET_ON_GAME_OBJECT.getId(),
                gameObject.getId(),
                -1,
                target,
                null
        );
        client.setMenuEntries(new MenuEntry[]{menuEntry});
    }
}