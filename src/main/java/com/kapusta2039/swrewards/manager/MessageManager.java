package com.kapusta2039.swrewards.manager;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.util.HexUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class MessageManager {

    private final SWRewards plugin;
    private final Map<String, String> cache = new HashMap<>();

    public MessageManager(SWRewards plugin) {
        this.plugin = plugin;
    }

    // Загружает сообщения с заданным языком
    public void load(String lang) {
        cache.clear();

        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        saveDefaultLanguage("messages_ru.yml");
        saveDefaultLanguage("messages_en.yml");

        File langFile = new File(messagesDir, "messages_" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Файл языка не найден: " + langFile.getName()
                    + ". Загружаю 'en'.");
            langFile = new File(messagesDir, "messages_en.yml");
            if (!langFile.exists()) {
                plugin.getLogger().severe("Нет файлов языков!");
                return;
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        flatten(config);
        plugin.getLogger().info("Загружены сообщения (" + lang + ")");
    }

    private void saveDefaultLanguage(String fileName) {
        File target = new File(plugin.getDataFolder(), "messages/" + fileName);
        if (!target.exists()) {
            try {
                plugin.saveResource("messages/" + fileName, false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not save default: " + fileName, e);
            }
        }
    }

    private void flatten(ConfigurationSection config) {
        for (Map.Entry<String, Object> entry : config.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                cache.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }

    public String getMessage(String key) {
        return HexUtils.colorize(cache.getOrDefault(key, "&cMissing message: " + key));
    }

    public String getRaw(String key) {
        return cache.getOrDefault(key, "Missing message: " + key);
    }
}
