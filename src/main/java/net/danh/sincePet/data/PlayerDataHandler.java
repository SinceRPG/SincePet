package net.danh.sincePet.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.SchedulerUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataHandler {
    private static final Type LEVEL_MAP_TYPE = new TypeToken<Map<String, Integer>>() {
    }.getType();
    private static final Type XP_MAP_TYPE = new TypeToken<Map<String, Double>>() {
    }.getType();
    private static final Type NESTED_INT_MAP_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {
    }.getType();
    private static final Type NESTED_BOOL_MAP_TYPE = new TypeToken<Map<String, Map<String, Boolean>>>() {
    }.getType();

    private final SincePet plugin;
    private final Map<UUID, PlayerSession> sessionMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public PlayerDataHandler(SincePet plugin) {
        this.plugin = plugin;
    }

    public void loadData(@NotNull Player p) {
        UUID uuid = p.getUniqueId();
        if (sessionMap.containsKey(uuid)) return;

        SchedulerUtils.runAsync(plugin, () -> {
            LoadedPlayerData loadedData = loadDataFromDatabase(uuid);
            SchedulerUtils.runEntityDelayed(plugin, p, () -> {
                if (p.isOnline() && !sessionMap.containsKey(uuid)) {
                    int defaultMax = plugin.getConfigFile().getConfig().getInt("pet.default_max_level", 100);
                    sessionMap.put(uuid, new PlayerSession(
                            loadedData.activePetId(),
                            loadedData.petLevels(),
                            loadedData.petXp(),
                            loadedData.petMaxLevels(),
                            loadedData.petUpgrades(),
                            loadedData.petSettings(),
                            defaultMax
                    ));
                }
            }, 1L);
        });
    }

    private LoadedPlayerData loadDataFromDatabase(UUID uuid) {
        String petId = null;
        Map<String, Integer> petLevels = new HashMap<>();
        Map<String, Double> petXp = new HashMap<>();
        Map<String, Integer> petMaxLevels = new HashMap<>();
        Map<String, Map<String, Integer>> petUpgrades = new HashMap<>();
        Map<String, Map<String, Boolean>> petSettings = new HashMap<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String query = "SELECT * FROM " + plugin.getDatabaseManager().getUsersTable() + " WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        petId = rs.getString("active_pet");
                        petLevels = parseMap(rs.getString("pet_levels"), LEVEL_MAP_TYPE, new HashMap<>());
                        petXp = parseMap(rs.getString("pet_xp"), XP_MAP_TYPE, new HashMap<>());
                        try {
                            petMaxLevels = parseMap(rs.getString("pet_max_levels"), LEVEL_MAP_TYPE, new HashMap<>());
                        } catch (SQLException ignored) {
                            plugin.getLogger().warning("Column 'pet_max_levels' is missing. Run the plugin once after updating to migrate the database.");
                        }
                        try {
                            petUpgrades = parseMap(rs.getString("pet_upgrades"), NESTED_INT_MAP_TYPE, new HashMap<>());
                        } catch (SQLException ignored) {
                            plugin.getLogger().warning("Column 'pet_upgrades' is missing. Run the plugin once after updating to migrate the database.");
                        }
                        try {
                            petSettings = parseMap(rs.getString("pet_settings"), NESTED_BOOL_MAP_TYPE, new HashMap<>());
                        } catch (SQLException ignored) {
                            plugin.getLogger().warning("Column 'pet_settings' is missing. Run the plugin once after updating to migrate the database.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load pet data for " + uuid);
            e.printStackTrace();
        }

        return new LoadedPlayerData(petId, petLevels, petXp, petMaxLevels, petUpgrades, petSettings);
    }

    private <T> T parseMap(String json, Type type, T fallback) {
        if (json == null || json.isEmpty()) return fallback;
        T parsed = gson.fromJson(json, type);
        return parsed == null ? fallback : parsed;
    }

    public void saveData(UUID uuid, boolean removeDataFromMemory) {
        PlayerSession session = sessionMap.get(uuid);
        if (session == null) return;

        SavePayload payload = SavePayload.from(uuid, session, gson);
        if (removeDataFromMemory) sessionMap.remove(uuid);

        SchedulerUtils.runAsync(plugin, () -> savePayload(payload));
    }

    public void saveAllSync() {
        for (UUID uuid : new HashSet<>(sessionMap.keySet())) {
            PlayerSession session = sessionMap.get(uuid);
            if (session == null) continue;
            savePayload(SavePayload.from(uuid, session, gson));
        }
    }

    public void saveAllAsync() {
        new HashSet<>(sessionMap.keySet()).forEach(uuid -> saveData(uuid, false));
    }

    private void savePayload(SavePayload payload) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String upsert = plugin.getDatabaseManager().isMySQL()
                    ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels, pet_upgrades, pet_settings) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels), pet_xp=VALUES(pet_xp), pet_max_levels=VALUES(pet_max_levels), pet_upgrades=VALUES(pet_upgrades), pet_settings=VALUES(pet_settings)"
                    : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels, pet_upgrades, pet_settings) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, payload.uuid().toString());
                ps.setString(2, payload.activePetId());
                ps.setString(3, payload.petLevelsJson());
                ps.setString(4, payload.petXpJson());
                ps.setString(5, payload.petMaxLevelsJson());
                ps.setString(6, payload.petUpgradesJson());
                ps.setString(7, payload.petSettingsJson());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save pet data for " + payload.uuid());
            e.printStackTrace();
        }
    }

    public PlayerSession getSession(UUID uuid) {
        return sessionMap.get(uuid);
    }

    private record LoadedPlayerData(
            String activePetId,
            Map<String, Integer> petLevels,
            Map<String, Double> petXp,
            Map<String, Integer> petMaxLevels,
            Map<String, Map<String, Integer>> petUpgrades,
            Map<String, Map<String, Boolean>> petSettings
    ) {
    }

    private record SavePayload(
            UUID uuid,
            String activePetId,
            String petLevelsJson,
            String petXpJson,
            String petMaxLevelsJson,
            String petUpgradesJson,
            String petSettingsJson
    ) {
        private static SavePayload from(UUID uuid, PlayerSession session, Gson gson) {
            return new SavePayload(
                    uuid,
                    session.getActivePetId(),
                    gson.toJson(session.getAllLevels()),
                    gson.toJson(session.getAllXp()),
                    gson.toJson(session.getAllMaxLevels()),
                    gson.toJson(session.getAllUpgradeLevels()),
                    gson.toJson(session.getAllSettings())
            );
        }
    }

    public static class PlayerSession {
        private final Map<String, Integer> petLevels;
        private final Map<String, Double> petXp;
        private final Map<String, Integer> petMaxLevels;
        private final Map<String, Map<String, Integer>> petUpgrades;
        private final Map<String, Map<String, Boolean>> petSettings;
        private final int globalDefaultMax;
        private String activePetId;

        public PlayerSession(String id, Map<String, Integer> lvls, Map<String, Double> xps, Map<String, Integer> maxLvls,
                             Map<String, Map<String, Integer>> upgrades, Map<String, Map<String, Boolean>> settings, int defaultMax) {
            this.activePetId = id;
            this.petLevels = new ConcurrentHashMap<>(lvls);
            this.petXp = new ConcurrentHashMap<>(xps);
            this.petMaxLevels = new ConcurrentHashMap<>(maxLvls);
            this.petUpgrades = new ConcurrentHashMap<>(upgrades);
            this.petSettings = new ConcurrentHashMap<>(settings);
            this.globalDefaultMax = defaultMax;
        }

        public String getActivePetId() {
            return activePetId;
        }

        public void setActivePetId(String id) {
            this.activePetId = id;
        }

        public int getLevel(String petId) {
            return petLevels.getOrDefault(petId, 1);
        }

        public void setLevel(String petId, int level) {
            petLevels.put(petId, level);
        }

        public double getXp(String petId) {
            return petXp.getOrDefault(petId, 0.0);
        }

        public void setXp(String petId, double xp) {
            petXp.put(petId, xp);
        }

        public Map<String, Integer> getAllLevels() {
            return petLevels;
        }

        public Map<String, Double> getAllXp() {
            return petXp;
        }

        public int getMaxPetLevel(String petId) {
            return petMaxLevels.getOrDefault(petId, globalDefaultMax);
        }

        public void setMaxPetLevel(String petId, int level) {
            petMaxLevels.put(petId, level);
        }

        public Map<String, Integer> getAllMaxLevels() {
            return petMaxLevels;
        }

        public int getUpgradeLevel(String petId, String upgradeId) {
            return petUpgrades.getOrDefault(petId, Map.of()).getOrDefault(upgradeId, 0);
        }

        public void setUpgradeLevel(String petId, String upgradeId, int level) {
            petUpgrades.computeIfAbsent(petId, ignored -> new ConcurrentHashMap<>()).put(upgradeId, level);
        }

        public Map<String, Map<String, Integer>> getAllUpgradeLevels() {
            return petUpgrades;
        }

        public boolean getSetting(String petId, String settingId, boolean fallback) {
            return petSettings.getOrDefault(petId, Map.of()).getOrDefault(settingId, fallback);
        }

        public void setSetting(String petId, String settingId, boolean value) {
            petSettings.computeIfAbsent(petId, ignored -> new ConcurrentHashMap<>()).put(settingId, value);
        }

        public Map<String, Map<String, Boolean>> getAllSettings() {
            return petSettings;
        }
    }
}
