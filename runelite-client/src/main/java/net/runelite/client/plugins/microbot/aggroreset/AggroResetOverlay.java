
package net.runelite.client.plugins.microbot.aggroreset;

import javax.inject.Inject;
import java.awt.*;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class AggroResetOverlay extends Overlay {

    @Inject
    private Client client;

    @Inject
    private AggroResetConfig config;

    public AggroResetOverlay() {
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        graphics.setColor(Color.WHITE);
        graphics.drawString("Safe Point: " + config.safePoint(), 10, 20);
        graphics.drawString("Home Point: " + config.homePoint(), 10, 40);
        graphics.drawString("Wait Time: " + config.waitTime() + "s", 10, 60);
        return null;
    }
}
