package net.danh.sincePet.hooks;

import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.entity.Player;

public class MMOCoreHook {
    public static String getPlayerClass(Player player) {
        try {
            PlayerData data = PlayerData.get(player.getUniqueId());
            if (data != null && data.getProfess() != null) {
                return data.getProfess().getId();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
