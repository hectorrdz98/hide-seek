package dev.sasukector.hideandseek.events;

import dev.sasukector.hideandseek.HideAndSeek;
import dev.sasukector.hideandseek.controllers.GameController;
import dev.sasukector.hideandseek.controllers.TeamsController;
import dev.sasukector.hideandseek.helpers.ServerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class GameEvents implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (GameController.getInstance().getCurrentStatus() == GameController.Status.PLAYING) {
            if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player) {
                if (TeamsController.getInstance().isSeeker(damager) && TeamsController.getInstance().isHiders(player)) {
                    ServerUtilities.sendBroadcastMessage(ServerUtilities.getMiniMessage().parse(
                            "<bold><color:#B9FAF8>" + player.getName()
                                    + "</color></bold> <color:#B298DC>fue capturado</color>"
                    ));
                    GameController.getInstance().restartPlayer(player);
                    ServerUtilities.playBroadcastSound("minecraft:block.end_portal_frame.fill",  1f, 0.4f);
                    Bukkit.getScheduler().runTaskLater(HideAndSeek.getInstance(), () -> {
                        if (GameController.getInstance().isInfectedMode()) {
                            TeamsController.getInstance().getSeekersTeam().addEntry(player.getName());
                            GameController.getInstance().giveSeekersItems(player);
                            player.setGameMode(GameMode.SURVIVAL);
                            player.teleport(GameController.getInstance().getSeekersSpawn());
                        }
                        if (TeamsController.getInstance().getAliveHiders() == 1) {
                            Player winner = TeamsController.getInstance().getAliveHidersPlayers().get(0);
                            if (winner != null) {
                                ServerUtilities.sendBroadcastTitle(
                                        Component.text("¡Ganador " + winner.getName() + "!", TextColor.color(0xB298DC)),
                                        Component.text("Muchas felicidades~", TextColor.color(0xB9FAF8))
                                );
                                ServerUtilities.sendBroadcastMessage(ServerUtilities.getMiniMessage().parse(
                                        "<color:#B298DC>¡Ganador</color> <bold><color:#B9FAF8>" + winner.getName()
                                                + "</color></bold><color:#B298DC>!</color>"
                                ));
                                GameController.getInstance().gameStop();
                            }
                        } else if (TeamsController.getInstance().getAliveHiders() < 1) {
                            ServerUtilities.sendBroadcastTitle(
                                    Component.text("Sin ganador", TextColor.color(0xB298DC)),
                                    Component.text("Wtf no hay ganador", TextColor.color(0xB9FAF8))
                            );
                            GameController.getInstance().gameStop();
                        }
                    }, 5L);
                } else {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (GameController.getInstance().getCurrentStatus() == GameController.Status.PLAYING) {
            Player player = event.getPlayer();
            if (player.getGameMode() != GameMode.SURVIVAL ||
                    !TeamsController.getInstance().isHiders(player)) {
                return;
            }
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
                GameController.getInstance().getPlayerLastMove().put(player.getUniqueId(), -1);
            }
        }
    }

    @EventHandler
    public void blockChestInteract(PlayerInteractEvent event) {
        if (GameController.getInstance().getCurrentStatus() == GameController.Status.PLAYING) {
            Player player = event.getPlayer();
            if (TeamsController.getInstance().isSeeker(player)) {
                if(event.getAction() == Action.RIGHT_CLICK_AIR) {
                    ItemStack itemStack = player.getEquipment().getItemInMainHand();
                    if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLocalizedName()) {
                        if (itemStack.getItemMeta().getLocalizedName().equals("seeker_spawn")) {
                            Location location = GameController.getInstance().getSeekersSpawn();
                            if (location != null) {
                                player.teleport(location);
                            }
                        }
                    }
                }
            }
        }
    }

}
