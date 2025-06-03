package gecko10000.backendservercommand;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;

// Registers a dummy command as a workaround for https://github.com/PaperMC/Paper/issues/12600
public class CommandBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        try {
            context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                commands.registrar().register(Commands.literal("server").build());
            });
        } catch (NoClassDefFoundError e) {
            context.getLogger().info(MiniMessage.miniMessage().deserialize(
                    "<red>Could not register dummy command during bootstrap." +
                            "Aliases in commands.yml that run /server may not work.")
            );
        }
    }
}
