package com.kapusta2039.swrewards.command;

import com.kapusta2039.swrewards.SWRewards;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class ReloadCommand implements CommandExecutor, TabCompleter {

    private final SWRewards plugin;

    public ReloadCommand(SWRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Проверка прав
        if (!sender.hasPermission("swrewards.reload")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reload.no_permission"));
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("§cUsage: /" + label + " reload");
            return true;
        }

        // Безопасная перезагрузка
        try {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessageManager().getMessage("command.reload.success"));
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while reloading: " + e.getMessage());
            plugin.getLogger().severe("Ошибка перезагрузки: " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("swrewards.reload")) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}
