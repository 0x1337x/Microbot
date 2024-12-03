package net.runelite.client.plugins.microbot.aggroreset;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;

public class AggroResetScript {

    @Inject
    private Client client;

    @Inject
    private AggroResetConfig config;

    private boolean running = false;

    public void start() {
        running = true;
        new Thread(this::execute).start();
    }

    public void stop() {
        running = false;
    }

    private void execute() {
        try {
            // Parse and validate safe point
            String[] safeCoords = config.safePoint().split(",");
            if (safeCoords.length != 3) {
                System.err.println("Invalid Safe Point configuration: Expected format x,y,z");
                return;
            }

            int safeX = Integer.parseInt(safeCoords[0].trim());
            int safeY = Integer.parseInt(safeCoords[1].trim());
            int safeZ = Integer.parseInt(safeCoords[2].trim());
            WorldPoint safePoint = new WorldPoint(safeX, safeY, safeZ);

            // Parse and validate home point
            String[] homeCoords = config.homePoint().split(",");
            if (homeCoords.length != 3) {
                System.err.println("Invalid Home Point configuration: Expected format x,y,z");
                return;
            }

            int homeX = Integer.parseInt(homeCoords[0].trim());
            int homeY = Integer.parseInt(homeCoords[1].trim());
            int homeZ = Integer.parseInt(homeCoords[2].trim());
            WorldPoint homePoint = new WorldPoint(homeX, homeY, homeZ);

            // Navigate to the safe point
            Rs2Walker.walkTo(safePoint);
            while (Rs2Player.distanceTo(safePoint) > 1 && running) {
                pause(100); // Wait until reaching the safe point
            }

            // Wait at the safe point
            pause(config.waitTime() * 1000);

            // Navigate back to the home point
            Rs2Walker.walkTo(homePoint);
            while (Rs2Player.distanceTo(homePoint) > 1 && running) {
                pause(100); // Wait until reaching the home point
            }

        } catch (NumberFormatException e) {
            System.err.println("Invalid coordinate format: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An error occurred during aggro reset: " + e.getMessage());
        }
    }

    /**
     * Utility method for safely pausing execution.
     * Uses Thread.sleep but catches interruptions to avoid crashes.
     *
     * @param millis the number of milliseconds to pause
     */
    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Aggro reset script interrupted: " + e.getMessage());
        }
    }
}