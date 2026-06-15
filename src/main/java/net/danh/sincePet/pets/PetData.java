package net.danh.sincePet.pets;

import java.util.List;
import java.util.Map;

public record PetData(String id, String name, String texture, Map<String, PetStatData> stats, double range,
                      double cooldown, String dmgFormula, double inheritance, String attackParticle, boolean rideable,
                      boolean canFly,
                      String maxXpFormula, List<PetUpgrade> upgrades, List<PetSkill> skills,
                      List<String> mmocoreClasses, UpgradingPoints upgradingPoints) {
    public record PetStatData(double base, Double maxValue, String formula) {}
    public record UpgradingPoints(int levelsPerPoint, int pointsPerInterval, int maxPoints) {}

    public String getDamageFormula() {
        return dmgFormula;
    }

    public String getMaxXpFormula() {
        return maxXpFormula;
    }
}
