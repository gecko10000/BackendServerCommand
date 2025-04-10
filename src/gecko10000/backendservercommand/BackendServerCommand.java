package gecko10000.backendservercommand;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class BackendServerCommand extends JavaPlugin implements Listener, PluginMessageListener {

    private boolean areServersRetrieved = false;
    private final Set<String> serverNames = new HashSet<>();

    // Uses a player to retrieve servers from the proxy.
    private void getServers(Player player) {
        areServersRetrieved = true;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        if (!subchannel.equals("GetServers")) return;
        String allServers = in.readUTF();
        serverNames.addAll(List.of(allServers.split(", ")));
    }

    // Use any online players onEnable to get servers.
    private void getServersFromOnlinePlayers() {
        Optional<? extends Player> onlinePlayer = getServer().getOnlinePlayers().stream().findFirst();
        onlinePlayer.ifPresent(this::getServers);
    }

    // We use the player to retrieve the list of servers.
    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (areServersRetrieved) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> getServers(event.getPlayer()), 2);
    }

    private void send(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    private int selfServerCommand(final CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) return Command.SINGLE_SUCCESS;
        String serverName = ctx.getArgument("server_name", String.class);
        if (!serverNames.contains(serverName)) {
            player.sendRichMessage("<red>This server doesn't exist!");
            return Command.SINGLE_SUCCESS;
        }
        send(player, serverName);
        return Command.SINGLE_SUCCESS;
    }

    private int playerServerCommand(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String serverName = ctx.getArgument("server_name", String.class);
        if (!serverNames.contains(serverName)) {
            ctx.getSource().getSender().sendRichMessage("<red>This server doesn't exist!");
            return Command.SINGLE_SUCCESS;
        }
        PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        Player target = resolver.resolve(ctx.getSource()).getFirst();
        send(target, serverName);
        return Command.SINGLE_SUCCESS;
    }

    private LiteralCommandNode<CommandSourceStack> buildCommand() {
        RequiredArgumentBuilder<CommandSourceStack, String> serverNameArg = Commands.argument("server_name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String currentArg = builder.getRemaining();
                    serverNames.stream().filter(n -> n.startsWith(currentArg)).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(this::selfServerCommand);
        RequiredArgumentBuilder<CommandSourceStack, PlayerSelectorArgumentResolver> playerArg = Commands.argument("player", ArgumentTypes.player()).requires(s -> s.getSender().hasPermission("backendservercommand.use.other"));
        return Commands.literal("server")
                .requires(s -> s.getSender().hasPermission("backendservercommand.use"))
                .then(serverNameArg.then(playerArg.executes(this::playerServerCommand)))
                .build();
    }

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        getServer().getPluginManager().registerEvents(this, this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildCommand());
        });

        getServersFromOnlinePlayers();
    }

}
