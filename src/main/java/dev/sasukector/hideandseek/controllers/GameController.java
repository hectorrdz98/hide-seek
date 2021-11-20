package dev.sasukector.hideandseek.controllers;

import dev.sasukector.hideandseek.HideAndSeek;
import dev.sasukector.hideandseek.helpers.ServerUtilities;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class GameController {

    private static GameController instance = null;
    private @Getter Status currentStatus = Status.LOBBY;
    private @Getter @Setter Location seekersSpawn = null;
    private @Getter @Setter Location hidersSpawn = null;
    private @Getter int playersSchedulerTaskID = -1;
    private final @Getter Map<UUID, Integer> playerLastMove;
    private @Getter @Setter boolean infectedMode = false;

    public enum Status {
        LOBBY, WAITING, PLAYING
    }

    public static GameController getInstance() {
        if (instance == null) {
            instance =  new GameController();
        }
        return instance;
    }

    public GameController() {
        this.playerLastMove = new HashMap<>();
    }

    public void restartPlayer(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.setArrowsInBody(0);
        player.setFireTicks(0);
        player.setVisualFire(false);
        player.setCollidable(true);
        player.getActivePotionEffects().forEach(p -> player.removePotionEffect(p.getType()));
        player.getInventory().clear();
        player.updateInventory();
    }

    public void returnPlayerToSpawn(Player player) {
        Location location = ServerUtilities.getLobbySpawn();
        World world = ServerUtilities.getOverworld();
        if (world != null && location != null) {
            player.teleport(location);
        }
    }

    public void reviveAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            this.restartPlayer(player);
            this.returnPlayerToSpawn(player);
            player.setGameMode(GameMode.SURVIVAL);
        });
    }

    public void handlePlayerJoin(Player player) {
        this.restartPlayer(player);
        if (this.currentStatus == Status.LOBBY) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        this.returnPlayerToSpawn(player);
        if (!TeamsController.getInstance().isHiders(player)) {
            TeamsController.getInstance().getHidersTeam().addEntry(player.getName());
        }
        if (!this.playerLastMove.containsKey(player.getUniqueId())) {
            this.playerLastMove.put(player.getUniqueId(), 0);
        }
    }

    public void giveSeekersItems(Player player) {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        itemStack.addUnsafeEnchantment(Enchantment.MENDING, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Regresar al spawn", TextColor.color(0xB298DC)));
        itemMeta.setLocalizedName("seeker_spawn");
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);
        player.getInventory().addItem(itemStack);
        player.updateInventory();
    }

    public void gameStart() {
        if (this.hidersSpawn == null || this.seekersSpawn == null) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            this.restartPlayer(player);
            player.setGameMode(GameMode.SURVIVAL);
        });
        ServerUtilities.playBroadcastSound("minecraft:block.note_block.xylophone",  1f, 1f);
        ServerUtilities.sendBroadcastMessage(Component.text("Ha iniciado la partida", TextColor.color(0xB9FAF8)));
        ServerUtilities.sendBroadcastTitle(
                Component.text("¡Escóndete!", TextColor.color(0xB298DC)),
                Component.text("1 minuto restante", TextColor.color(0xB9FAF8))
        );
        this.currentStatus = Status.WAITING;
        TeamsController.getInstance().getSeekers().forEach(player -> {
            this.giveSeekersItems(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60 * 20, 0, false, false));
            player.teleport(this.seekersSpawn);
            ServerUtilities.sendServerMessage(player,
                    Component.text("En 1 minuto puedes salir a buscar personas", TextColor.color(0xB298DC)));
        });
        TeamsController.getInstance().getHidersPlayers().forEach(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60 * 20, 1, false, false));
            player.teleport(this.hidersSpawn);
            ServerUtilities.sendServerMessage(player,
                    Component.text("Tienes 1 minuto para esconderte, recuerda dejar de moverte para ser invisible cuando empiece la partida", TextColor.color(0xB298DC)));
        });
        this.startHideCountDown();
    }

    public void startHideCountDown() {
        AtomicInteger remainingTime = new AtomicInteger(60);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingTime.get() <= 0) {
                    ServerUtilities.playBroadcastSound("minecraft:block.end_portal.spawn",  1f, 1.4f);
                    ServerUtilities.sendBroadcastMessage(
                            Component.text("3, 2, 1 ¡A buscar! Si estás escondido, recuerda moverte cada 15 segundos, sino podrán verte fácilmente", TextColor.color(0xB9FAF8)));
                    ServerUtilities.sendBroadcastTitle(
                            Component.text("¡A buscar!", TextColor.color(0xB298DC)),
                            Component.empty()
                    );
                    currentStatus = Status.PLAYING;
                    playersScheduler();
                    cancel();
                } else {
                    if (remainingTime.get() <= 3) {
                        ServerUtilities.sendBroadcastTitle(
                                Component.text(remainingTime.get(), TextColor.color(0xB9FAF8)),
                                Component.empty()
                        );
                        ServerUtilities.playBroadcastSound("minecraft:block.note_block.xylophone",  1f, 1f);
                    } else if (remainingTime.get() % 10 == 0) {
                        ServerUtilities.playBroadcastSound("minecraft:block.note_block.xylophone",  1f, 1f);
                    }
                    ServerUtilities.sendBroadcastAction(
                            Component.text("Tiempo para esconderse: " + remainingTime.get() + "s",
                                    TextColor.color(0xB298DC))
                    );
                    remainingTime.addAndGet(-1);
                }
            }
        }.runTaskTimer(HideAndSeek.getInstance(), 0L, 20L);
    }

    public void playersScheduler() {
        this.playersSchedulerTaskID = new BukkitRunnable() {
            @Override
            public void run() {
                Map<UUID, Integer> lastMoves = new HashMap<>(playerLastMove);
                lastMoves.forEach((uuid, lastMove) -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.getGameMode() == GameMode.SURVIVAL) {
                        if (TeamsController.getInstance().isHiders(player)) {
                            playerLastMove.put(player.getUniqueId(), lastMove - 1);
                            if (lastMove - 1 <= -4) {
                                player.removePotionEffect(PotionEffectType.GLOWING);
                                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, false, false));
                                playerLastMove.put(player.getUniqueId(), 15);
                            } else if (lastMove - 1 == 0) {
                                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 999999, 0, false, false));
                                playerLastMove.put(player.getUniqueId(), 1);
                            }
                            if (lastMove - 1 >= 0) {
                                player.sendActionBar(Component.text("Muévete en " + (lastMove - 1) + " s",
                                        TextColor.color(0xB298DC)));
                            }
                        } else if (TeamsController.getInstance().isSeeker(player)) {
                            if (player.getLocation().getNearbyLivingEntities(10).stream()
                                    .anyMatch(livingEntity -> livingEntity instanceof Player near &&
                                            TeamsController.getInstance().isHiders(near) &&
                                            near.getGameMode() == GameMode.SURVIVAL)) {
                                player.playSound(player.getLocation(), "minecraft:entity.shulker_bullet.hit", 1, 2);
                                player.sendActionBar(Component.text("~ ❤ ~", TextColor.color(0xB298DC)));
                            }
                        }
                    }
                });
            }
        }.runTaskTimer(HideAndSeek.getInstance(), 0L , 20L).getTaskId();
    }

    public void gameStop() {
        ServerUtilities.sendBroadcastMessage(Component.text("Se ha detenido el juego", TextColor.color(0xB9FAF8)));
        ServerUtilities.playBroadcastSound("minecraft:entity.wither.death",  1f, 1.4f);
        this.currentStatus = Status.LOBBY;
        this.reviveAllPlayers();
        if (this.playersSchedulerTaskID != -1) {
            Bukkit.getScheduler().cancelTask(this.playersSchedulerTaskID);
            this.playersSchedulerTaskID = -1;
        }
    }

}
