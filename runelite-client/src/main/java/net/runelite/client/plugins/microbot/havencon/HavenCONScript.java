
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

public class HavenCONScript {



    @Inject
    private HavenCONConfig config;

    private boolean running = false;

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void onGameTick() {
        if (!running) return;

        if (exchangePlanks() && enterBuildMode() && buildLarder()) {
            // Loop the process
        }
    }

    private boolean exchangePlanks() {
        NPC phials = Rs2Npc.getNpc("Phials");
        if (phials == null) {
            Rs2Antiban.actionCooldown(); // Randomized wait
            return false;
        }

        Rs2Inventory.useItemOnNpc(-1, phials);
        Rs2Antiban.actionCooldown(); // Delay after using item on NPC

        if (Rs2Dialogue.isInDialogue() && Rs2Dialogue.hasSelectAnOption()) {
            Rs2Dialogue.clickOption("Exchange X");

            // Wait for input dialog to appear
            Global.awaitExecutionUntil(
                    () -> System.out.println("Dialog option detected!"),
                    () -> Rs2Dialogue.hasDialogueOption("Enter amount"),
                    5000
            );

            Rs2Keyboard.typeString(String.valueOf(config.exchangeAmount()));
            Rs2Keyboard.keyPress(VK_ENTER);
            Rs2Antiban.actionCooldown(); // Delay after typing input
            return true;
        }
        return false;
    }

    private boolean enterBuildMode() {
        GameObject portal = (GameObject) Rs2GameObject.findObjectById(4525);// Portal ID
        if (portal == null) {
            Rs2Antiban.actionCooldown(); // Randomized wait
            return false;
        }

        selectOption("Build Mode", "Portal", portal);
        Rs2Antiban.actionCooldown(); // Delay after interaction
        return true;
    }

    private boolean buildLarder() {
        GameObject larderSpace = (GameObject) Rs2GameObject.findObjectById(12345); // Example hotspot ID
        if (larderSpace == null) {
            Rs2Antiban.actionCooldown(); // Randomized wait
            return false;
        }

        selectOption("Build", "Larder space", larderSpace);
        Rs2Antiban.actionCooldown(); // Delay after clicking build
        selectOption("Oak larder", "Larder space", larderSpace);
        Rs2Antiban.actionCooldown(); // Delay after selecting larder type
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
