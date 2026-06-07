package com.kapusta2039.swrewards.manager;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.model.PlayerData;
import com.kapusta2039.swrewards.model.Reward;
import com.kapusta2039.swrewards.model.PlayerData.RewardProgress;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public final class PlaceholderManager {

    private final SWRewards plugin;

    public PlaceholderManager(SWRewards plugin) {
        this.plugin = plugin;
    }

    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) return "";

        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return "";

        // %swrewards_afk%
        if (params.equalsIgnoreCase("afk")) {
            return String.valueOf(data.isAfk());
        }

        // %swrewards_next_reward%
        if (params.equalsIgnoreCase("next_reward")) {
            long shortest = Long.MAX_VALUE;
            for (Reward reward : plugin.getRewardManager().getRewards()) {
                RewardProgress rp = data.getProgress(reward.getId());
                if (!reward.isRepetitive() && rp.isGranted()) continue;
                long remaining = reward.getPeriodMillis() - rp.getAccumulatedTime();
                if (remaining < shortest) shortest = remaining;
            }
            return shortest > 0 && shortest < Long.MAX_VALUE
                    ? formatTime(shortest)
                    : plugin.getMessageManager().getRaw("reward.available_now");
        }

        // %swrewards_next_reward_<id>%
        if (params.startsWith("next_reward_")) {
            String rewardId = params.substring("next_reward_".length());
            Reward reward = plugin.getRewardManager().getReward(rewardId);
            if (reward == null) {
                plugin.getLogger().warning("Неизвестная награда в плейсхолдере: " + rewardId);
                return "&cUnknown reward";
            }

            RewardProgress rp = data.getProgress(rewardId);
            if (!reward.isRepetitive() && rp.isGranted()) {
                return plugin.getMessageManager().getRaw("reward.one_time_claimed");
            }

            long remaining = reward.getPeriodMillis() - rp.getAccumulatedTime();
            return remaining > 0 ? formatTime(remaining)
                    : plugin.getMessageManager().getRaw("reward.available_now");
        }

        return "";
    }

    private String formatTime(long millis) {
        long days    = TimeUnit.MILLISECONDS.toDays(millis);
        long hours   = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
