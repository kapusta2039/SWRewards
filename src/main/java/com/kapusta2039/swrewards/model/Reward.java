package com.kapusta2039.swrewards.model;

import com.kapusta2039.swrewards.util.HexUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Reward {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("(\\d+)([smhd])");

    private final String id;
    private final long periodMillis;
    private final String rawMessage;
    private final List<String> commands;
    private final boolean detectAfk;
    private final boolean repetitive;
    private final String timeRange;
    private final String permission;
    private final int dailyLimit;

    private Reward(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.periodMillis = builder.periodMillis;
        this.rawMessage = Objects.requireNonNull(builder.rawMessage, "message");
        this.commands = Collections.unmodifiableList(
                Objects.requireNonNull(builder.commands, "commands"));
        this.detectAfk = builder.detectAfk;
        this.repetitive = builder.repetitive;
        this.timeRange = builder.timeRange;
        this.permission = builder.permission;
        this.dailyLimit = builder.dailyLimit;
    }

    // Создает вознаграждение из раздела конфигурации YAML
    public static Reward fromConfig(String id, ConfigurationSection section) {
        String periodStr = section.getString("period", "1h");
        boolean detectAfk = section.getBoolean("detect_afk", true);
        boolean repetitive = section.getBoolean("repetitive", true);
        String message = section.getString("message", "&aYou received a reward!");
        List<String> commands = section.getStringList("commands");
        String timeRange = section.getString("time_range");
        String permission = section.getString("permission");
        int dailyLimit = section.getInt("daily_limit", 0);

        return new Builder(id)
                .periodMillis(parsePeriod(id, periodStr))
                .rawMessage(message)
                .commands(commands)
                .detectAfk(detectAfk)
                .repetitive(repetitive)
                .timeRange(timeRange != null && !timeRange.isEmpty() ? timeRange : null)
                .permission(permission != null && !permission.isEmpty() ? permission : null)
                .dailyLimit(dailyLimit)
                .build();
    }

    // Парсит период из строки
    private static long parsePeriod(String rewardId, String period) {
        Matcher matcher = PERIOD_PATTERN.matcher(period.trim().toLowerCase());
        if (matcher.matches()) {
            long amount = Long.parseLong(matcher.group(1));
            if (amount <= 0) return 3_600_000L;
            return switch (matcher.group(2)) {
                case "s" -> amount * 1_000L;
                case "m" -> amount * 60_000L;
                case "h" -> amount * 3_600_000L;
                case "d" -> amount * 86_400_000L;
                default -> 3_600_000L;
            };
        }
        return 3_600_000L;
    }

    public String getId()              { return id; }
    public long getPeriodMillis()      { return periodMillis; }
    public String getRawMessage()      { return rawMessage; }
    public String getColoredMessage()  { return HexUtils.colorize(rawMessage); }
    public List<String> getCommands()  { return commands; }
    public boolean isDetectAfk()       { return detectAfk; }
    public boolean isRepetitive()      { return repetitive; }
    public boolean hasTimeRange()      { return timeRange != null; }
    public String getTimeRange()       { return timeRange; }
    public boolean hasPermission()     { return permission != null; }
    public String getPermission()      { return permission; }
    public boolean hasDailyLimit()     { return dailyLimit > 0; }
    public int getDailyLimit()         { return dailyLimit; }

    public static final class Builder {
        private final String id;
        private long periodMillis;
        private String rawMessage;
        private List<String> commands;
        private boolean detectAfk = true;
        private boolean repetitive = true;
        private String timeRange;
        private String permission;
        private int dailyLimit;

        public Builder(String id) { this.id = id; }

        public Builder periodMillis(long val)       { this.periodMillis = val; return this; }
        public Builder rawMessage(String val)        { this.rawMessage = val; return this; }
        public Builder commands(List<String> val)    { this.commands = val; return this; }
        public Builder detectAfk(boolean val)        { this.detectAfk = val; return this; }
        public Builder repetitive(boolean val)       { this.repetitive = val; return this; }
        public Builder timeRange(String val)         { this.timeRange = val; return this; }
        public Builder permission(String val)        { this.permission = val; return this; }
        public Builder dailyLimit(int val)           { this.dailyLimit = val; return this; }
        public Reward build()                        { return new Reward(this); }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reward reward)) return false;
        return id.equals(reward.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Reward{id='" + id + "', period=" + periodMillis + "ms}";
    }
}
