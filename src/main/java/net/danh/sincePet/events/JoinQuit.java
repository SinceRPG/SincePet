package net.danh.sincePet.events;

import net.danh.sincePet.SincePet;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuit implements Listener {
    private final SincePet plugin;

    public JoinQuit(SincePet plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getPetManager().onPlayerJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getPetManager().onPlayerQuit(e.getPlayer());
        plugin.getPlayerDataHandler().saveData(e.getPlayer().getUniqueId(), true);
    }
}