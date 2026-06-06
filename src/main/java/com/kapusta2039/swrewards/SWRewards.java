package com.kapusta2039.swrewards;

import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.plugin.configuration.PluginMeta;

public final class SWRewards extends JavaPlugin {

    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginMeta meta = this.getPluginMeta();
        getLogger().info("SWRewards v" + meta.getVersion() + " включен!");

        rewardManager = new RewardManager(this);
        rewardManager.startTracking();
    }

    @Override
    public void onDisable() {
        if (rewardManager != null) {
            rewardManager.saveData();
            rewardManager.stopTracking();
        }
        getLogger().info("SWRewards отключен!");
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }
}
