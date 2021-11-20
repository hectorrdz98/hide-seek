package dev.sasukector.hideandseek.controllers;

import dev.sasukector.hideandseek.HideAndSeek;
import dev.sasukector.hideandseek.helpers.FastBoard;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class BoardController {

    private static BoardController instance = null;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private @Setter @Getter boolean hideDays;

    public static BoardController getInstance() {
        if (instance == null) {
            instance = new BoardController();
        }
        return instance;
    }

    public BoardController() {
        Bukkit.getScheduler().runTaskTimer(HideAndSeek.getInstance(), this::updateBoards, 0L, 20L);
        this.hideDays = false;
    }

    public void newPlayerBoard(Player player) {
        FastBoard board = new FastBoard(player);
        this.boards.put(player.getUniqueId(), board);
    }

    public void removePlayerBoard(Player player) {
        FastBoard board = this.boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void updateBoards() {
        boards.forEach((uuid, board) -> {
            Player player = Bukkit.getPlayer(uuid);
            assert player != null;

            board.updateTitle("§d§lHide & Seek");

            List<String> lines = new ArrayList<>();
            lines.add("");

            switch (GameController.getInstance().getCurrentStatus()) {
                case LOBBY ->
                    lines.add("§7En espera");
                case WAITING ->
                    lines.add("§5¡Escóndete!");
                case PLAYING ->
                    lines.add("§3En partida");
            }

            lines.add("");
            lines.add("Online: §6" + Bukkit.getOnlinePlayers().size());
            lines.add("Restantes: §6" + TeamsController.getInstance().getAliveHiders());
            if (player.isOp() && !player.getName().equals("Conterstine")) {
                lines.add("TPS: §6" + String.format("%.2f", Bukkit.getTPS()[0]));
            }
            lines.add("");

            board.updateLines(lines);
        });
    }

}
