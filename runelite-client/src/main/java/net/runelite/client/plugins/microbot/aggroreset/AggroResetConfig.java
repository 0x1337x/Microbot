
package net.runelite.client.plugins.microbot.aggroreset;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("aggroreset")
public interface AggroResetConfig extends Config {

    @ConfigItem(
            keyName = "safePoint",
            name = "Safe Point",
            description = "Coordinates to run to (format: x,y,z)."
    )
    default String safePoint() {
        return "3200,3200,0";
    }

    @ConfigItem(
            keyName = "homePoint",
            name = "Home Point",
            description = "Coordinates to return to (format: x,y,z)."
    )
    default String homePoint() {
        return "3200,3200,0";
    }

    @ConfigItem(
            keyName = "waitTime",
            name = "Wait Time (seconds)",
            description = "How long to wait at the safe point before returning."
    )
    default int waitTime() {
        return 5;
    }
}
