package net.danh.sincePet.pets;

import java.util.List;

public record PetData(String id, String name, String texture, String stat, String formula, double range,
                      double cooldown, String dmgFormula, double inheritance, String attackParticle, boolean rideable, boolean canFly,
                      String maxXpFormula, List<PetUpgrade> upgrades) {
    public String getDamageFormula() {
        return dmgFormula;
    }

    public String getMaxXpFormula() {
        return maxXpFormula;
    }
}
