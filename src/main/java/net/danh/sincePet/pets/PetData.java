package net.danh.sincePet.pets;

public record PetData(String id, String name, String texture, String stat, String formula, double range,
                      double cooldown, String dmgFormula, double inheritance, boolean rideable, boolean canFly,
                      String maxXpFormula) {
    public String getDamageFormula() {
        return dmgFormula;
    }

    public String getMaxXpFormula() {
        return maxXpFormula;
    }
}
