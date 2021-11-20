package dev.sasukector.hideandseek.controllers;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class TeamsController {

    private static TeamsController instance = null;
    private @Getter Team seekersTeam;
    private @Getter Team hidersTeam;

    public static TeamsController getInstance() {
        if (instance == null) {
            instance = new TeamsController();
        }
        return instance;
    }

    public TeamsController() {
        this.createOrLoadTeams();
    }

    public void createOrLoadTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        seekersTeam = scoreboard.getTeam("seekers");
        hidersTeam = scoreboard.getTeam("hiders");

        if (seekersTeam == null) {
            seekersTeam = scoreboard.registerNewTeam("seekers");
            seekersTeam.color(NamedTextColor.AQUA);
            seekersTeam.prefix(Component.text("[SEEKER] "));
            seekersTeam.setAllowFriendlyFire(false);
        }

        if (hidersTeam == null) {
            hidersTeam = scoreboard.registerNewTeam("hiders");
            hidersTeam.color(NamedTextColor.GRAY);
            hidersTeam.prefix(Component.text("[HIDER] "));
            hidersTeam.setAllowFriendlyFire(false);
        }
    }

    public List<Player> getSeekers() {
        List<Player> players = new ArrayList<>();
        this.seekersTeam.getEntries().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                players.add(player);
            }
        });
        return players;
    }


    public List<Player> getHidersPlayers() {
        List<Player> players = new ArrayList<>();
        this.hidersTeam.getEntries().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                players.add(player);
            }
        });
        return players;
    }

    public List<Player> getAliveHidersPlayers() {
        List<Player> players = new ArrayList<>();
        this.hidersTeam.getEntries().forEach(entry -> {
            Player player = Bukkit.getPlayer(entry);
            if (player != null && player.getGameMode() == GameMode.SURVIVAL) {
                players.add(player);
            }
        });
        return players;
    }

    public long getAliveHiders() {
        return this.getHidersPlayers().stream()
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                .count();
    }

    public boolean isSeeker(Player player) {
        return this.getSeekers().contains(player);
    }

    public boolean isHiders(Player player) {
        return this.getHidersPlayers().contains(player);
    }

}
