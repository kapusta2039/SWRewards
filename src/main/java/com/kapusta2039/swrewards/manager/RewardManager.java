package com.kapusta2039.swrewards.manager;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.database.DatabaseManager;
import com.kapusta2039.swrewards.model.PlayerData;
import com.kapusta2039.swrewards.model.PlayerData.RewardProgress;
import com.kapusta2039.swrewards.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

// Загружает награды, отслеживает время, проверяет условия и выдает награды.
public final class RewardManager {

    private static final long TICK_INTERVAL = 1200L; // 60 секунд
    private static final long SAVE_INTERVAL = 6000L; // 5 минут

    private final SWRewards plugin;
    private final DatabaseManager database;
    private final List<Reward> rewards = new ArrayList<>();
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final AFKManager afkManager;
    private final WelcomeBackManager welcomeBackManager;

    private ZoneId zoneId = ZoneId.systemDefault();

    public RewardManager(SWRewards plugin, DatabaseManager database,
                         AFKManager afkManager, WelcomeBackManager welcomeBackManager) {
        this.plugin = plugin;
        this.database = database;
        this.afkManager = afkManager;
        this.welcomeBackManager = welcomeBackManager;
    }

    public void loadRewards() {
        rewards.clear();

        FileConfiguration rewardsConfig = plugin.getRewardsConfig();
        ConfigurationSection section = rewardsConfig.getConfigurationSection("rewards");

        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection rs = section.getConfigurationSection(key);
            if (rs == null) continue;
            Reward reward = Reward.fromConfig(key, rs);
            rewards.add(reward);
            plugin.getLogger().info("Загружена награда: " + key);
        }

        String tz = plugin.getConfig().getString("options.time_zone", "Europe/Moscow");
        try {
            zoneId = ZoneId.of(tz);
        } catch (Exception e) {
            zoneId = ZoneId.of("Europe/Moscow");
        }
    }

    // Запускает периодическую проверку вознаграждений и сохранение
    public void start() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
            try {
                tick();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка в тике наград", e);
            }
        }, TICK_INTERVAL, TICK_INTERVAL);

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> {
            try {
                saveAll();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка автосохранения", e);
            }
        }, SAVE_INTERVAL * 50L, SAVE_INTERVAL * 50L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) continue;
            PlayerData data = cache.get(player.getUniqueId());
            if (data == null) continue;

            // Обновление статуса AFK
            afkManager.checkAfk(player, data);

            long elapsed = TICK_INTERVAL * 50L;

            for (Reward reward : rewards) {
                RewardProgress rp = data.getProgress(reward.getId());

                if (!reward.isRepetitive() && rp.isGranted()) continue;

                if (reward.hasPermission() && !player.hasPermission(reward.getPermission())) continue;

                if (reward.hasTimeRange() && !isInTimeRange(reward.getTimeRange())) continue;

                if (reward.getDailyLimit() > 0 && rp.getTodayClaims() >= reward.getDailyLimit()) {
                    continue;
                }

                if (!reward.isDetectAfk() || !data.isAfk()) {
                    rp.addAccumulatedTime(elapsed);
                }

                if (rp.getAccumulatedTime() >= reward.getPeriodMillis()) {
                    grantReward(player, reward, rp);
                }
            }
        }
    }

    private boolean isInTimeRange(String timeRange) {
        String[] parts = timeRange.split("-");
        if (parts.length != 2) return true;

        try {
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            int nowMinutes = now.getHour() * 60 + now.getMinute();

            String[] startParts = parts[0].trim().split(":");
            String[] endParts   = parts[1].trim().split(":");

            int startMinutes = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMinutes   = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

            if (startMinutes <= endMinutes) {
                return nowMinutes >= startMinutes && nowMinutes <= endMinutes;
            } else {
                return nowMinutes >= startMinutes || nowMinutes <= endMinutes;
            }
        } catch (Exception e) {
            return true;
        }
    }

    private void grantReward(Player player, Reward reward, RewardProgress rp) {
        for (String command : reward.getCommands()) {
            String cmd = command
                    .replace("%player_name%", player.getName())
                    .replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        player.sendMessage(reward.getColoredMessage());

        rp.setLastClaim(System.currentTimeMillis());
        rp.setAccumulatedTime(0);
        rp.incrementTodayClaims();

        if (!reward.isRepetitive()) {
            rp.setGranted(true);
        }
    }

    // Загружает данные игрока из базы данных в кэш
    public PlayerData loadPlayer(UUID uuid) {
        PlayerData data = database.loadPlayer(uuid);
        cache.put(uuid, data);

        if (data.isNewPlayer()) {
            data.setNewPlayer(false);
        }
        return data;
    }

    // Сохраняет данные игрока и удаляет их из кэша
    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            database.savePlayer(data);
        }
    }

    // Сохраняет все кэшированные данные игрока (периодически вызывается и отключается).
    public void saveAll() {
        for (PlayerData data : cache.values()) {
            database.savePlayer(data);
        }
    }

    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public List<Reward> getRewards() {
        return List.copyOf(rewards);
    }

    // Находит награду по ее id или по null

    public Reward getReward(String id) {
        for (Reward r : rewards) {
            if (r.getId().equals(id)) return r;
        }
        return null;
    }

    // Загружает награды из rewards.yml
    public void reload() {
        plugin.reloadRewardsConfig();
        loadRewards();
        welcomeBackManager.reload();
    }
}