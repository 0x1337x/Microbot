package net.runelite.client.plugins.microbot.havencon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("havencon")
public interface HavenCONConfig extends Config {

    @ConfigItem(
            keyName = "plankId",
            name = "Noted Plank Item ID",
            description = "The item ID of the noted planks to use for construction"
    )
    default int plankId() {
        return 960; // Example default value
    }

    @ConfigItem(
            keyName = "exchangeAmount",
            name = "Exchange Amount",
            description = "The amount of planks to exchange"
    )
    default int exchangeAmount() {
        return 50; // Default exchange amount
    }
}