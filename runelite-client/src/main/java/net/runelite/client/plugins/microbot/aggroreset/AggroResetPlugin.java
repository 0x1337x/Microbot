
package net.runelite.client.plugins.microbot.aggroreset;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "Aggro Reset",
        description = "Automatically resets aggression and returns to the starting point.",
        tags = {"aggro", "reset", "utility"}
)
public class AggroResetPlugin extends Plugin {

    @Inject
    private AggroResetConfig config;

    @Inject
    private AggroResetScript script;

    @Provides
    AggroResetConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AggroResetConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        script.start();
    }

    @Override
    protected void shutDown() throws Exception {
        script.stop();
    }
}
