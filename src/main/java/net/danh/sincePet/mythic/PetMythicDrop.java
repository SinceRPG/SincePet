package net.danh.sincePet.mythic;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.ILocationDrop;
import io.lumine.mythic.api.skills.SkillCaster;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.utils.Calculator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public abstract class PetMythicDrop implements ILocationDrop {

    private final RangeType rangeType;
    private int minLvl = 0;
    private int maxLvl = 0;

    public PetMythicDrop(MythicLineConfig config) {
        // Cấu hình range: range=10-20, range=>=50, ...
        String range = config.getString(new String[]{"range", "r"}, null);
        if (range == null || range.isEmpty()) {
            this.rangeType = RangeType.NONE;
        } else {
            try {
                if (range.contains("-")) {
                    this.rangeType = RangeType.BETWEEN;
                    String[] parts = range.split("-");
                    this.minLvl = Integer.parseInt(parts[0]);
                    this.maxLvl = Integer.parseInt(parts[1]);
                } else if (range.contains(">=")) {
                    this.rangeType = RangeType.GTE;
                    this.minLvl = Integer.parseInt(range.replace(">=", ""));
                } else if (range.contains(">")) {
                    this.rangeType = RangeType.GT;
                    this.minLvl = Integer.parseInt(range.replace(">", ""));
                } else if (range.contains("<=")) {
                    this.rangeType = RangeType.LTE;
                    this.maxLvl = Integer.parseInt(range.replace("<=", ""));
                } else if (range.contains("<")) {
                    this.rangeType = RangeType.LT;
                    this.maxLvl = Integer.parseInt(range.replace("<", ""));
                } else {
                    this.rangeType = RangeType.NONE;
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid range format in MythicDrop: " + range);
            }
        }
    }

    protected abstract void giveReward(Player p, Location loc, double expAmount);

    protected double calculateExp(String expFormulaInput, int mobLevel, int petLevel) {
        // Thay thế placeholder trong công thức
        String expFormula = expFormulaInput
                .replace("<mob_level>", String.valueOf(mobLevel))
                .replace("<pet_level>", String.valueOf(petLevel));

        try {
            return Double.parseDouble(Calculator.calculator(expFormula, 2));
        } catch (Exception e) {
            return 0;
        }
    }

    protected boolean checkRange(int petLevel) {
        return switch (rangeType) {
            case NONE -> true;
            case BETWEEN -> petLevel >= minLvl && petLevel <= maxLvl;
            case GTE -> petLevel >= minLvl;
            case GT -> petLevel > minLvl;
            case LTE -> petLevel <= maxLvl;
            case LT -> petLevel < maxLvl;
        };
    }

    @Override
    public void drop(AbstractLocation abstractLocation, DropMetadata dropMetadata, double v) {
        Optional<AbstractEntity> abstractEntity = dropMetadata.getCause();
        Optional<SkillCaster> skillCaster = dropMetadata.getDropper();

        if (abstractEntity.isPresent() && skillCaster.isPresent()) {
            Player p = Bukkit.getPlayer(abstractEntity.get().getName());
            if (p == null) return;

            // Kiểm tra Player có Pet không
            PlayerDataHandler.PlayerSession session = SincePet.getPlugin().getPlayerDataHandler().getSession(p.getUniqueId());
            if (session == null || session.getActivePetId() == null) return;

            // Lấy thông tin Level
            int mobLevel = (int) skillCaster.get().getLevel();
            int petLevel = session.getLevel(session.getActivePetId());

            // Check Range O(1)
            if (!checkRange(petLevel)) return;

            Location location = new Location(
                    Bukkit.getWorld(skillCaster.get().getLocation().getWorld().getName()),
                    skillCaster.get().getLocation().getBlockX(),
                    skillCaster.get().getLocation().getBlockY(),
                    skillCaster.get().getLocation().getBlockZ()
            );

            processDrop(p, location, mobLevel, petLevel);
        }
    }

    public abstract void processDrop(Player p, Location location, int mobLevel, int petLevel);

    private enum RangeType {NONE, BETWEEN, GTE, GT, LTE, LT}
}