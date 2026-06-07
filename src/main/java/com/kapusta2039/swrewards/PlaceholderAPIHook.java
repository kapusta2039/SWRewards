package com.kapusta2039.swrewards;

import com.kapusta2039.swrewards.manager.PlaceholderManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final SWRewards plugin;
    private final PlaceholderManager manager;

    public PlaceholderAPIHook(SWRewards plugin, PlaceholderManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "swrewards";
    }

    @Override
    public @NotNull String getAuthor() {
        return "kapusta2039";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return manager.onPlaceholderRequest(player, params);
    }
}
