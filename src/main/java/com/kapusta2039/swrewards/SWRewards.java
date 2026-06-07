package com.kapusta2039.swrewards;

import com.kapusta2039.swrewards.command.ReloadCommand;
import com.kapusta2039.swrewards.database.DatabaseManager;
import com.kapusta2039.swrewards.manager.*;
import com.kapusta2039.swrewards.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;

public final class SWRewards extends JavaPlugin implements Listener {

    private DatabaseManager database;
    private RewardManager rewardManager;
    private AFKManager afkManager;
    private MessageManager messageManager;
    private WelcomeBackManager welcomeBackManager;
    private PlaceholderManager placeholderManager;
    private File rewardsFile;
    private FileConfiguration rewardsConfig;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Сохранение настроек (только если нет)
        saveDefaultConfig();
        safeSaveResource("rewards.yml");
        safeSaveResource("messages/messages_ru.yml");
        safeSaveResource("messages/messages_en.yml");

        // Загрузка сообщений
        String lang = getConfig().getString("options.lang", "ru");
        this.messageManager = new MessageManager(this);
        messageManager.load(lang);

        // Инициализация базы данных
        String storageMethod = getConfig().getString("options.storage-method", "H2");
        this.database = new DatabaseManager(this, storageMethod);
        database.init();

        // Создание менеджеров
        this.afkManager = new AFKManager(this);
        this.welcomeBackManager = new WelcomeBackManager(this);
        this.rewardManager = new RewardManager(this, database, afkManager, welcomeBackManager);
        this.placeholderManager = new PlaceholderManager(this);

        // Загрузка наград
        welcomeBackManager.reload();
        rewardManager.loadRewards();

        // Регистрация команд
        ReloadCommand reloadCmd = new ReloadCommand(this);
        getCommand("swrewards").setExecutor(reloadCmd);
        getCommand("swrewards").setTabCompleter(reloadCmd);

        // Регистрация событий
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(afkManager, this);
        getServer().getPluginManager().registerEvents(welcomeBackManager, this);

        // Регистрация PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new PlaceholderAPIHook(this, placeholderManager).register();
                getLogger().info("PlaceholderAPI подключён: %swrewards_*%");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Не удалось зарегистрировать PlaceholderAPI", e);
            }
        } else {
            getLogger().info("PlaceholderAPI не найден — плейсхолдеры отключены.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            rewardManager.loadPlayer(player.getUniqueId());
        }

        // Отслеживание наград
        rewardManager.start();

        getLogger().info("SWRewards v" + getPluginMeta().getVersion() + " загружен.");
    }

    @Override
    public void onDisable() {
        if (afkManager != null) afkManager.clearBossBars();
        if (rewardManager != null) {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = rewardManager.getCached(player.getUniqueId());
                if (data != null) {
                    data.setLastLeave(now);
                }
            }
            rewardManager.saveAll();
        }
        if (database != null) {
            database.shutdown();
        }
        getLogger().info("SWRewards выключен.");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = rewardManager.loadPlayer(uuid);
        data.setLastActivity(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = rewardManager.getCached(uuid);
        if (data != null) {
            data.setLastLeave(System.currentTimeMillis());
        }
        rewardManager.unloadPlayer(uuid);
    }

    public void reloadPlugin() {
        reloadConfig();
        String lang = getConfig().getString("options.lang", "ru");
        messageManager.load(lang);
        if (afkManager != null) afkManager.reload();
        rewardManager.reload();
        getLogger().info("SWRewards перезагружен.");
    }

    public FileConfiguration getRewardsConfig() {
        if (rewardsConfig == null) {
            reloadRewardsConfig();
        }
        return rewardsConfig;
    }

    public void reloadRewardsConfig() {
        if (rewardsFile == null) {
            rewardsFile = new File(getDataFolder(), "rewards.yml");
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public RewardManager getRewardManager()          { return rewardManager; }
    public MessageManager getMessageManager()        { return messageManager; }
    public AFKManager getAfkManager()                { return afkManager; }
    public WelcomeBackManager getWelcomeBackManager() { return welcomeBackManager; }
    public PlayerData getPlayerData(UUID uuid)       { return rewardManager != null ? rewardManager.getCached(uuid) : null; }

    // saveResource без варнинга, если файл уже есть
    private void safeSaveResource(String path) {
        if (!new File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }
}
