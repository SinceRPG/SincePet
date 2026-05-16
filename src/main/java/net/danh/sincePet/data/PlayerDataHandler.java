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
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load pet data for " + uuid);
            e.printStackTrace();
        }

        return new LoadedPlayerData(petId, petLevels, petXp, petMaxLevels);
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
                    ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels), pet_xp=VALUES(pet_xp), pet_max_levels=VALUES(pet_max_levels)"
                    : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                ps.setString(1, payload.uuid().toString());
                ps.setString(2, payload.activePetId());
                ps.setString(3, payload.petLevelsJson());
                ps.setString(4, payload.petXpJson());
                ps.setString(5, payload.petMaxLevelsJson());
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
            Map<String, Integer> petMaxLevels
    ) {
    }

    private record SavePayload(
            UUID uuid,
            String activePetId,
            String petLevelsJson,
            String petXpJson,
            String petMaxLevelsJson
    ) {
        private static SavePayload from(UUID uuid, PlayerSession session, Gson gson) {
            return new SavePayload(
                    uuid,
                    session.getActivePetId(),
                    gson.toJson(session.getAllLevels()),
                    gson.toJson(session.getAllXp()),
                    gson.toJson(session.getAllMaxLevels())
            );
        }
    }

    public static class PlayerSession {
        private final Map<String, Integer> petLevels;
        private final Map<String, Double> petXp;
        private final Map<String, Integer> petMaxLevels;
        private final int globalDefaultMax;
        private String activePetId;

        public PlayerSession(String id, Map<String, Integer> lvls, Map<String, Double> xps, Map<String, Integer> maxLvls, int defaultMax) {
            this.activePetId = id;
            this.petLevels = new ConcurrentHashMap<>(lvls);
            this.petXp = new ConcurrentHashMap<>(xps);
            this.petMaxLevels = new ConcurrentHashMap<>(maxLvls);
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
    }
}
