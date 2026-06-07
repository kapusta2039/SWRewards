package com.kapusta2039.swrewards.manager;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.model.PlayerData;
import com.kapusta2039.swrewards.util.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Отслеживает движение и проверяет время простоя
public final class AFKManager implements Listener {

    private final SWRewards plugin;
    private long afkThreshold;  // ms
    private boolean useActionBar;
    private boolean useBossBar;
    private boolean useChatMessage;

    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Boolean> afkNotifyCooldown = new HashMap<>();

    public AFKManager(SWRewards plugin) {
        this.plugin = plugin;
        this.afkThreshold = parsePeriod(plugin.getConfig().getString("options.time_to_afk", "5m"));
        this.useActionBar = plugin.getConfig().getBoolean("options.afk.afk_actionbar", true);
        this.useBossBar   = plugin.getConfig().getBoolean("options.afk.afk_bossbar", true);
        this.useChatMessage = plugin.getConfig().getBoolean("options.afk.afk_message", true);
    }

    private long parsePeriod(String period) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)([smhd])")
                .matcher(period.trim().toLowerCase());
        if (m.matches()) {
            long amt = Long.parseLong(m.group(1));
            return switch (m.group(2)) {
                case "s" -> amt * 1_000L;
                case "m" -> amt * 60_000L;
                case "h" -> amt * 3_600_000L;
                case "d" -> amt * 86_400_000L;
                default -> 300_000L;
            };
        }
        return 300_000L; // 5 min
    }

    public long getAfkThreshold() {
        return afkThreshold;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setLastActivity(System.currentTimeMillis());

            // Восстановление из AFK
            if (data.isAfk()) {
                data.setAfk(false);
                data.setAfkSince(0);
                removeBossBar(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Сбросить AFK при входе
        UUID uuid = event.getPlayer().getUniqueId();
        afkNotifyCooldown.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        removeBossBar(event.getPlayer());
        afkNotifyCooldown.remove(uuid);
    }

    // Вызывается каждый тик из RewardManager
    public boolean checkAfk(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        long idleTime = now - data.getLastActivity();

        if (idleTime >= afkThreshold) {
            if (!data.isAfk()) {
                data.setAfk(true);
                data.setAfkSince(now);
                notifyAfk(player);
            }
            return true;
        }

        if (data.isAfk()) {
            data.setAfk(false);
            data.setAfkSince(0);
            removeBossBar(player);
        }
        return false;
    }

    private void notifyAfk(Player player) {
        if (useActionBar) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    com.kapusta2039.swrewards.util.HexUtils.colorize(
                            plugin.getMessageManager().getRaw("afk.actionbar"))));
        }
        if (useBossBar && !bossBars.containsKey(player.getUniqueId())) {
            BossBar bar = Bukkit.createBossBar(
                    HexUtils.colorize(plugin.getMessageManager().getRaw("afk.bossbar")),
                    BarColor.WHITE, BarStyle.SOLID);
            bar.addPlayer(player);
            bossBars.put(player.getUniqueId(), bar);
        }
        if (useChatMessage) {
            player.sendMessage(plugin.getMessageManager().getMessage("afk.chat"));
        }
    }

    private void removeBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    // Очистка всех BossBar при выключении
    public void clearBossBars() {
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
    }

    // Перезагрузка настроек из config.yml
    public void reload() {
        this.afkThreshold = parsePeriod(plugin.getConfig().getString("options.time_to_afk", "5m"));
        this.useActionBar = plugin.getConfig().getBoolean("options.afk.afk_actionbar", true);
        this.useBossBar   = plugin.getConfig().getBoolean("options.afk.afk_bossbar", true);
        this.useChatMessage = plugin.getConfig().getBoolean("options.afk.afk_message", true);
    }
}
