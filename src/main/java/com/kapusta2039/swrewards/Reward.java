package com.kapusta2039.swrewards;

import java.util.List;
import java.util.Objects;

public final class Reward {

    private final String id;
    private final long periodMillis;
    private final String rawMessage;
    private final List<String> commands;

    public Reward(String id, long periodMillis, String rawMessage, List<String> commands) {
        this.id = Objects.requireNonNull(id, "id не должен быть пустым");
        this.periodMillis = periodMillis;
        this.rawMessage = Objects.requireNonNull(rawMessage, "сообщение не должно быть пустым");
        this.commands = Objects.requireNonNull(commands, "команды в плагине обязательны");
    }

    public String getId() {
        return id;
    }

    public long getPeriodMillis() {
        return periodMillis;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public String getColoredMessage() {
        return rawMessage.replace('&', '§');
    }

    public List<String> getCommands() {
        return List.copyOf(commands);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reward reward)) return false;
        return id.equals(reward.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Reward{id='" + id + "', periodMillis=" + periodMillis + "}";
    }
}
