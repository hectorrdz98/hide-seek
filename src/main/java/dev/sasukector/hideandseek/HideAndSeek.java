package dev.sasukector.hideandseek;

import dev.sasukector.hideandseek.commands.GameCommand;
import dev.sasukector.hideandseek.controllers.BoardController;
import dev.sasukector.hideandseek.controllers.GameController;
import dev.sasukector.hideandseek.events.GameEvents;
import dev.sasukector.hideandseek.events.SpawnEvents;
import dev.sasukector.hideandseek.helpers.ServerUtilities;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HideAndSeek extends JavaPlugin {

    private static @Getter HideAndSeek instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info(ChatColor.DARK_PURPLE + "HideAndSeek startup!");
        instance = this;

        // Register events
        this.getServer().getPluginManager().registerEvents(new SpawnEvents(), this);
        this.getServer().getPluginManager().registerEvents(new GameEvents(), this);

        // Refresh scoreboard for online players
        Bukkit.getOnlinePlayers().forEach(player -> {
            BoardController.getInstance().newPlayerBoard(player);
            GameController.getInstance().handlePlayerJoin(player);
        });

        // Register commands
        Objects.requireNonNull(HideAndSeek.getInstance().getCommand("game")).setExecutor(new GameCommand());

        // Load lobby location
        World world = ServerUtilities.getOverworld();
        if (world != null) {
            ServerUtilities.setLobbySpawn(new Location(world, 33, 102, -24));
            GameController.getInstance().setSeekersSpawn(new Location(world, -66, 13, -75));
            GameController.getInstance().setHidersSpawn(new Location(world, 12, 10, 26));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info(ChatColor.DARK_PURPLE + "HideAndSeek shutdown!");
    }
}
