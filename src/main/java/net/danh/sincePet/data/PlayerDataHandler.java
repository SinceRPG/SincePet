package net.danh.sincePet.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.danh.sincePet.SincePet;
import org.bukkit.Bukkit;
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
    private final SincePet plugin;
    private final Map<UUID, PlayerSession> sessionMap = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public PlayerDataHandler(SincePet plugin) {
        this.plugin = plugin;
    }

    public void loadData(@NotNull Player p) {
        UUID uuid = p.getUniqueId();
        if (sessionMap.containsKey(uuid)) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String petId = null;
            Map<String, Integer> petLevels = new HashMap<>();
            Map<String, Double> petXp = new HashMap<>();
            Map<String, Integer> petMaxLevels = new HashMap<>(); // Map lưu Max Level từng pet

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT * FROM " + plugin.getDatabaseManager().getUsersTable() + " WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            petId = rs.getString("active_pet");

                            // Load Levels
                            String jsonLv = rs.getString("pet_levels");
                            if (jsonLv != null && !jsonLv.isEmpty()) {
                                Type type = new TypeToken<Map<String, Integer>>() {
                                }.getType();
                                petLevels = gson.fromJson(jsonLv, type);
                            }

                            // Load XP
                            String jsonXp = rs.getString("pet_xp");
                            if (jsonXp != null && !jsonXp.isEmpty()) {
                                Type type = new TypeToken<Map<String, Double>>() {
                                }.getType();
                                petXp = gson.fromJson(jsonXp, type);
                            }

                            // Load Max Levels (NEW)
                            // Lưu ý: Cần đảm bảo cột 'pet_max_levels' đã tồn tại trong DB
                            try {
                                String jsonMax = rs.getString("pet_max_levels");
                                if (jsonMax != null && !jsonMax.isEmpty()) {
                                    Type type = new TypeToken<Map<String, Integer>>() {
                                    }.getType();
                                    petMaxLevels = gson.fromJson(jsonMax, type);
                                }
                            } catch (SQLException ignored) {
                                // Bỏ qua nếu cột chưa được tạo (cho trường hợp update plugin nhưng chưa alter table)
                                plugin.getLogger().warning("Column 'pet_max_levels' missing in database! Please update your schema.");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String finalPetId = petId;
            Map<String, Integer> finalPetLevels = (petLevels != null) ? petLevels : new HashMap<>();
            Map<String, Double> finalPetXp = (petXp != null) ? petXp : new HashMap<>();
            Map<String, Integer> finalPetMaxLevels = (petMaxLevels != null) ? petMaxLevels : new HashMap<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline() && !sessionMap.containsKey(uuid)) {
                    // Truyền thêm Config mặc định vào Session để xử lý fallback
                    int defaultMax = plugin.getConfigFile().getConfig().getInt("pet.default_max_level", 100);
                    sessionMap.put(uuid, new PlayerSession(finalPetId, finalPetLevels, finalPetXp, finalPetMaxLevels, defaultMax));
                }
            });
        });
    }

    public void saveData(UUID uuid, boolean removeDataFromMemory) {
        if (!sessionMap.containsKey(uuid)) return;
        PlayerSession session = sessionMap.get(uuid);

        final String activePet = session.getActivePetId();
        final String jsonLevels = gson.toJson(session.getAllLevels());
        final String jsonXp = gson.toJson(session.getAllXp());
        final String jsonMaxLevels = gson.toJson(session.getAllMaxLevels()); // JSON Max Levels

        if (removeDataFromMemory) sessionMap.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Cập nhật câu lệnh SQL thêm cột thứ 5
                String upsert = plugin.getDatabaseManager().isMySQL()
                        ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels), pet_xp=VALUES(pet_xp), pet_max_levels=VALUES(pet_max_levels)"
                        : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, activePet);
                    ps.setString(3, jsonLevels);
                    ps.setString(4, jsonXp);
                    ps.setString(5, jsonMaxLevels); // Set Max Levels
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void saveAllSync() {
        for (UUID uuid : new HashSet<>(sessionMap.keySet())) {
            PlayerSession session = sessionMap.get(uuid);
            if (session == null) continue;
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String upsert = plugin.getDatabaseManager().isMySQL()
                        ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels), pet_xp=VALUES(pet_xp), pet_max_levels=VALUES(pet_max_levels)"
                        : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels, pet_xp, pet_max_levels) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, session.getActivePetId());
                    ps.setString(3, gson.toJson(session.getAllLevels()));
                    ps.setString(4, gson.toJson(session.getAllXp()));
                    ps.setString(5, gson.toJson(session.getAllMaxLevels()));
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAllAsync() {
        new HashSet<>(sessionMap.keySet()).forEach(uuid -> saveData(uuid, false));
    }

    public PlayerSession getSession(UUID uuid) {
        return sessionMap.get(uuid);
    }

    public static class PlayerSession {
        private final Map<String, Integer> petLevels;
        private final Map<String, Double> petXp;
        private final Map<String, Integer> petMaxLevels; // Map Max Level riêng từng pet
        private final int globalDefaultMax;
        private String activePetId;

        public PlayerSession(String id, Map<String, Integer> lvls, Map<String, Double> xps, Map<String, Integer> maxLvls, int defaultMax) {
            this.activePetId = id;
            this.petLevels = lvls;
            this.petXp = xps;
            this.petMaxLevels = maxLvls;
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

        // --- LOGIC MAX LEVEL MỚI ---
        public int getMaxPetLevel(String petId) {
            // Nếu có set riêng cho pet này thì lấy, không thì lấy mặc định config
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