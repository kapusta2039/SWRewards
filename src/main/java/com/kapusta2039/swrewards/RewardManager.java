package com.kapusta2039.swrewards;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RewardManager {
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(\\d+)([smhd])");
    private static final long TICK_INTERVAL = 1200L; // 60 секунд
    private static final long DEFAULT_PERIOD = 3600_000L; // 1 час

    private final SWRewards plugin;
    private final List<Reward> rewards = new ArrayList<>();
    private final Map<UUID, Map<String, Long>> playerTimestamps = new HashMap<>();
    private final File dataFile;
    private Object task;

    public RewardManager(SWRewards plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadRewards();
        loadData();
    }

    private void loadRewards() {
        rewards.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("rewards");

        if (section == null) {
            plugin.getLogger().warning("В config.yml не найден раздел 'rewards'!");
            return;
        }

        for (String key : section.getKeys(false)) {
            String path = key + ".";
            String periodStr = section.getString(path + "period", "1h");
            String message = section.getString(path + "message", "&aВы получили награду!");
            List<String> commands = section.getStringList(path + "commands");
            long periodMillis = parsePeriod(key, periodStr);

            rewards.add(new Reward(key, periodMillis, message, commands));
            plugin.getLogger().info("Загружена награда: '" + key + "' (период: " + periodStr + ")");
        }

        if (rewards.isEmpty()) {
            plugin.getLogger().warning("Не загружено ни одной награды из config.yml!");
        }
    }

    private long parsePeriod(String rewardId, String period) {
        Matcher matcher = PERIOD_PATTERN.matcher(period.trim().toLowerCase(Locale.ROOT));

        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            if (amount <= 0) {
                plugin.getLogger().warning("Награда '" + rewardId + "' имеет неположительное значение периода. Используется 1ч.");
                return DEFAULT_PERIOD;
            }
            return switch (matcher.group(2)) {
                case "s" -> amount * 1_000L;
                case "m" -> amount * 60_000L;
                case "h" -> amount * 3_600_000L;
                case "d" -> amount * 86_400_000L;
                default -> DEFAULT_PERIOD;
            };
        }

        plugin.getLogger().warning("Награда '" + rewardId + "' имеет неверный формат периода: '" + period + "'. Используется 1ч.");
        return DEFAULT_PERIOD;
    }

    private void loadData() {
        if (!dataFile.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        for (String uuidStr : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = data.getConfigurationSection(uuidStr);
                Map<String, Long> timestamps = new HashMap<>();
                if (playerSection != null) {
                    for (String rewardId : playerSection.getKeys(false)) {
                        timestamps.put(rewardId, playerSection.getLong(rewardId));
                    }
                }
                playerTimestamps.put(uuid, timestamps);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Пропущен неверный UUID в data.yml: '" + uuidStr + "'");
            }
        }
        plugin.getLogger().info("Загружены данные для " + playerTimestamps.size() + " игроков.");
    }

    public void saveData() {
        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Long>> entry : playerTimestamps.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Long> rewardEntry : entry.getValue().entrySet()) {
                data.set(uuidStr + "." + rewardEntry.getKey(), rewardEntry.getValue());
            }
        }
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось сохранить данные игроков", e);
        }
    }

    public void startTracking() {
        if (Bukkit.getGlobalRegionScheduler() != null) {
            task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> {
                checkAllPlayers();
            }, 1L, TICK_INTERVAL);
        } else {
            plugin.getLogger().warning("GlobalRegionScheduler недоступен, используется BukkitRunnable (может не работать на Folia)");
            task = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    checkAllPlayers();
                }
            }.runTaskTimer(plugin, 1L, TICK_INTERVAL);
        }
    }

    public void stopTracking() {
        if (task == null) return;

        if (Bukkit.getGlobalRegionScheduler() != null && task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask) {
            ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) task).cancel();
        } else if (task instanceof org.bukkit.scheduler.BukkitTask) {
            ((org.bukkit.scheduler.BukkitTask) task).cancel();
        }
        task = null;
    }

    private void checkAllPlayers() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Map<String, Long> timestamps = playerTimestamps.computeIfAbsent(uuid, k -> new HashMap<>());

            List<Reward> toGrant = new ArrayList<>();
            for (Reward reward : rewards) {
                long lastClaim = timestamps.getOrDefault(reward.getId(), 0L);
                if (now - lastClaim >= reward.getPeriodMillis()) {
                    toGrant.add(reward);
                }
            }

            if (!toGrant.isEmpty()) {
                runInPlayerRegion(player, () -> {
                    for (Reward reward : toGrant) {
                        grantReward(player, reward);
                    }
                });

                for (Reward reward : toGrant) {
                    timestamps.put(reward.getId(), now);
                }
                changed = true;
            }
        }

        if (changed) {
            saveData();
        }
    }

    private void runInPlayerRegion(Player player, Runnable action) {
        try {
            if (player.getScheduler() != null) {
                player.getScheduler().run(plugin, scheduledTask -> action.run(), null);
                return;
            }
        } catch (NoSuchMethodError | Exception ignored) {
        }
        action.run();
    }

    private void grantReward(Player player, Reward reward) {
        player.sendMessage(reward.getColoredMessage());

        if (Bukkit.getGlobalRegionScheduler() != null) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> {
                executeCommands(reward, player.getName());
            });
        } else {
            executeCommands(reward, player.getName());
        }
    }

    private void executeCommands(Reward reward, String playerName) {
        for (String command : reward.getCommands()) {
            String cmd = command.replace("%player_name%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    public List<Reward> getRewards() {
        return List.copyOf(rewards);
    }

    public void reload() {
        plugin.reloadConfig();
        loadRewards();
    }
}