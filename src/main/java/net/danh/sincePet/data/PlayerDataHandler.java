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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String petId = null;
            Map<String, Integer> petLevels = new HashMap<>();

            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String query = "SELECT * FROM " + plugin.getDatabaseManager().getUsersTable() + " WHERE uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            petId = rs.getString("active_pet");
                            String json = rs.getString("pet_levels");
                            if (json != null && !json.isEmpty()) {
                                Type type = new TypeToken<Map<String, Integer>>() {
                                }.getType();
                                petLevels = gson.fromJson(json, type);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String finalPetId = petId;
            Map<String, Integer> finalPetLevels = (petLevels != null) ? petLevels : new HashMap<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) {
                    sessionMap.put(uuid, new PlayerSession(finalPetId, finalPetLevels));
                }
            });
        });
    }

    public void saveData(UUID uuid, boolean removeDataFromMemory) {
        if (!sessionMap.containsKey(uuid)) return;
        PlayerSession session = sessionMap.get(uuid);
        String activePet = session.getActivePetId();
        String jsonLevels = gson.toJson(session.getAllLevels());

        if (removeDataFromMemory) sessionMap.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String upsert = plugin.getDatabaseManager().isMySQL()
                        ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels)"
                        : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, activePet);
                    ps.setString(3, jsonLevels);
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
                        ? "INSERT INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE active_pet=VALUES(active_pet), pet_levels=VALUES(pet_levels)"
                        : "INSERT OR REPLACE INTO " + plugin.getDatabaseManager().getUsersTable() + " (uuid, active_pet, pet_levels) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, session.getActivePetId());
                    ps.setString(3, gson.toJson(session.getAllLevels()));
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
        private String activePetId;

        public PlayerSession(String id, Map<String, Integer> lvls) {
            this.activePetId = id;
            this.petLevels = lvls;
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

        public Map<String, Integer> getAllLevels() {
            return petLevels;
        }
    }
}