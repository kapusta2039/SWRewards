package com.kapusta2039.swrewards.manager;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.model.PlayerData;
import com.kapusta2039.swrewards.util.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Определяет, когда игрок возвращается после долгого отсутствия, и выдаёт награду
public final class WelcomeBackManager implements Listener {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("(\\d+)([smhd])");

    private final SWRewards plugin;
    private boolean enabled;
    private long checkPeriodMillis;
    private String rewardMode;   // "ALL" или "RANDOM"
    private String rawMessage;
    private List<String> commands;
    private final Random random = new Random();

    public WelcomeBackManager(SWRewards plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        ConfigurationSection section = plugin.getRewardsConfig().getConfigurationSection("welcome-back");
        if (section == null) {
            this.enabled = false;
            return;
        }

        this.enabled = section.getBoolean("enabled", true);
        this.checkPeriodMillis = parsePeriod(section.getString("check-period", "7d"));
        this.rewardMode = section.getString("reward-mode", "ALL");
        this.rawMessage = section.getString("message", "&aWelcome back!");
        this.commands = section.getStringList("commands");
    }

    private long parsePeriod(String period) {
        Matcher m = PERIOD_PATTERN.matcher(period.trim().toLowerCase());
        if (m.matches()) {
            long amt = Long.parseLong(m.group(1));
            return switch (m.group(2)) {
                case "s" -> amt * 1_000L;
                case "m" -> amt * 60_000L;
                case "h" -> amt * 3_600_000L;
                case "d" -> amt * 86_400_000L;
                default -> 604_800_000L; // 7d
            };
        }
        return 604_800_000L;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.isNewPlayer()) return;

        if (data.isWelcomeBackGranted()) return;

        long awayTime = System.currentTimeMillis() - data.getLastLeave();
        if (awayTime >= checkPeriodMillis) {
            grantWelcomeBack(player, data);
        }
    }

    private void grantWelcomeBack(Player player, PlayerData data) {
        if (commands.isEmpty()) return;

        // Folia-safe: dispatchCommand только с главного региона
        Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
            if ("RANDOM".equalsIgnoreCase(rewardMode)) {
                String cmd = commands.get(random.nextInt(commands.size()))
                        .replace("%player_name%", player.getName())
                        .replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else {
                for (String cmd : commands) {
                    cmd = cmd.replace("%player_name%", player.getName())
                             .replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }

            String broadcast = HexUtils.colorize(
                    rawMessage.replace("%player_name%", player.getName()));
            Bukkit.broadcast(net.kyori.adventure.text.Component.text(broadcast));
        });

        data.setWelcomeBackGranted(true);
        data.setLastWelcomeBackTime(System.currentTimeMillis());
    }

    public boolean isEnabled()             { return enabled; }
    public long getCheckPeriodMillis()     { return checkPeriodMillis; }
}
