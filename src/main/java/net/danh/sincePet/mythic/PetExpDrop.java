package net.danh.sincePet.mythic;

import io.lumine.mythic.api.config.MythicLineConfig;
import net.danh.sincePet.SincePet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PetExpDrop extends PetMythicDrop {

    private final String xpString;

    public PetExpDrop(@NotNull MythicLineConfig config) {
        super(config);
        // Supports pet-xp{xp=100}, pet-xp{amount=100}, and pet-xp{a=100}.
        this.xpString = config.getString(new String[]{"xp", "amount", "a"}, "0");
    }

    @Override
    public void processDrop(Player p, Location location, int mobLevel, int petLevel) {
        double finalExp = calculateExp(xpString, mobLevel, petLevel);
        if (finalExp > 0) {
            giveReward(p, location, finalExp);
        }
    }

    @Override
    protected void giveReward(Player p, Location loc, double expAmount) {
        SincePet.getPlugin().getPetManager().addExp(p, expAmount);
    }
}
