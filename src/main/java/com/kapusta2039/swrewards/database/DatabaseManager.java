package com.kapusta2039.swrewards.database;

import com.kapusta2039.swrewards.SWRewards;
import com.kapusta2039.swrewards.model.PlayerData;
import com.kapusta2039.swrewards.model.PlayerData.RewardProgress;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

//Управляет подключениями к базе данных
public final class DatabaseManager {

    private static final String TABLE_PLAYERS   = "swrewards_players";
    private static final String TABLE_REWARDS   = "swrewards_rewards";

    private final SWRewards plugin;
    private final String storageMethod;
    private HikariDataSource dataSource;

    public DatabaseManager(SWRewards plugin, String storageMethod) {
        this.plugin = plugin;
        this.storageMethod = storageMethod.toLowerCase();
    }

    // Инициализирует пул подключений к базе данных и создает таблицы.
    public void init() {
        try {
            switch (storageMethod) {
                case "mysql"  -> initMySQL();
                case "sqlite" -> initSQLite();
                default       -> initH2();
            }
            createTables();
            plugin.getLogger().info("БД инициализирована: " + storageMethod);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось инициализировать БД", e);
        }
    }

    private void initH2() {
        HikariConfig config = new HikariConfig();
        File dbFile = new File(plugin.getDataFolder(), "data/swrewards");
        config.setJdbcUrl("jdbc:h2:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
    }

    private void initSQLite() {
        HikariConfig config = new HikariConfig();
        File dbFile = new File(plugin.getDataFolder(), "data/swrewards.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
    }

    private void initMySQL() {
        String host     = plugin.getConfig().getString("options.data.host", "localhost");
        int port        = plugin.getConfig().getInt("options.data.port", 3306);
        String database = plugin.getConfig().getString("options.data.database", "swrewards");
        String user     = plugin.getConfig().getString("options.data.username", "root");
        String password = plugin.getConfig().getString("options.data.password", "");
        int poolSize    = plugin.getConfig().getInt("options.data.pool-size", 10);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(2);
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);
    }

    private void createTables() {
        String playersSQL;
        String rewardsSQL;

        if (storageMethod.equals("mysql")) {
            playersSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYERS + " ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "last_activity BIGINT NOT NULL DEFAULT 0, "
                    + "total_play_time BIGINT NOT NULL DEFAULT 0, "
                    + "first_join BIGINT NOT NULL DEFAULT 0, "
                    + "last_leave BIGINT NOT NULL DEFAULT 0, "
                    + "is_new_player TINYINT NOT NULL DEFAULT 1, "
                    + "welcome_back_granted TINYINT NOT NULL DEFAULT 0, "
                    + "last_welcome_back_time BIGINT NOT NULL DEFAULT 0)";

            rewardsSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_REWARDS + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "reward_id VARCHAR(64) NOT NULL, "
                    + "last_claim BIGINT NOT NULL DEFAULT 0, "
                    + "accumulated_time BIGINT NOT NULL DEFAULT 0, "
                    + "granted TINYINT NOT NULL DEFAULT 0, "
                    + "today_claims INT NOT NULL DEFAULT 0, "
                    + "last_claim_date BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, reward_id))";
        } else {
            // H2 / SQLite
            playersSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_PLAYERS + " ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "last_activity BIGINT NOT NULL DEFAULT 0, "
                    + "total_play_time BIGINT NOT NULL DEFAULT 0, "
                    + "first_join BIGINT NOT NULL DEFAULT 0, "
                    + "last_leave BIGINT NOT NULL DEFAULT 0, "
                    + "is_new_player BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "welcome_back_granted BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "last_welcome_back_time BIGINT NOT NULL DEFAULT 0)";

            rewardsSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_REWARDS + " ("
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "reward_id VARCHAR(64) NOT NULL, "
                    + "last_claim BIGINT NOT NULL DEFAULT 0, "
                    + "accumulated_time BIGINT NOT NULL DEFAULT 0, "
                    + "granted BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "today_claims INT NOT NULL DEFAULT 0, "
                    + "last_claim_date BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (uuid, reward_id))";
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(playersSQL);
            stmt.execute(rewardsSQL);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось создать таблицы", e);
        }
    }

    // Подключение и создание таблиц
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public PlayerData loadPlayer(UUID uuid) {
        PlayerData data = new PlayerData(uuid);

        String sql = "SELECT * FROM " + TABLE_PLAYERS + " WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                data.setLastActivity(rs.getLong("last_activity"));
                data.setTotalPlayTime(rs.getLong("total_play_time"));
                data.setFirstJoin(rs.getLong("first_join"));
                data.setLastLeave(rs.getLong("last_leave"));
                data.setNewPlayer(rs.getBoolean("is_new_player"));
                data.setWelcomeBackGranted(rs.getBoolean("welcome_back_granted"));
                data.setLastWelcomeBackTime(rs.getLong("last_welcome_back_time"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось загрузить игрока: " + uuid, e);
        }

        // Прогресс в вознаграждении за загрузку
        String rSql = "SELECT * FROM " + TABLE_REWARDS + " WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(rSql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                RewardProgress rp = data.getProgress(rs.getString("reward_id"));
                rp.setLastClaim(rs.getLong("last_claim"));
                rp.setAccumulatedTime(rs.getLong("accumulated_time"));
                rp.setGranted(rs.getBoolean("granted"));
                rp.setTodayClaims(rs.getInt("today_claims"));
                rp.setLastClaimDate(rs.getLong("last_claim_date"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось загрузить прогресс для: " + uuid, e);
        }

        return data;
    }

    public void savePlayer(PlayerData data) {
        UUID uuid = data.getUuid();

        String sql = "MERGE INTO " + TABLE_PLAYERS + " (uuid, last_activity, total_play_time, "
                + "first_join, last_leave, is_new_player, welcome_back_granted, last_welcome_back_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        if (storageMethod.equals("sqlite")) {
            sql = "INSERT OR REPLACE INTO " + TABLE_PLAYERS + " "
                    + "(uuid, last_activity, total_play_time, first_join, last_leave, "
                    + "is_new_player, welcome_back_granted, last_welcome_back_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        } else if (storageMethod.equals("mysql")) {
            sql = "INSERT INTO " + TABLE_PLAYERS + " "
                    + "(uuid, last_activity, total_play_time, first_join, last_leave, "
                    + "is_new_player, welcome_back_granted, last_welcome_back_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE last_activity=VALUES(last_activity), "
                    + "total_play_time=VALUES(total_play_time), "
                    + "last_leave=VALUES(last_leave), "
                    + "welcome_back_granted=VALUES(welcome_back_granted), "
                    + "last_welcome_back_time=VALUES(last_welcome_back_time)";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, data.getLastActivity());
            ps.setLong(3, data.getTotalPlayTime());
            ps.setLong(4, data.getFirstJoin());
            ps.setLong(5, data.getLastLeave());
            ps.setBoolean(6, data.isNewPlayer());
            ps.setBoolean(7, data.isWelcomeBackGranted());
            ps.setLong(8, data.getLastWelcomeBackTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Не удалось сохранить игрока: " + uuid, e);
        }

        // Сохранение прогресса в вознаграждении
        saveRewardProgress(uuid, data.getAllProgress());
    }

    private void saveRewardProgress(UUID uuid, Map<String, RewardProgress> progress) {
        for (Map.Entry<String, RewardProgress> entry : progress.entrySet()) {
            RewardProgress rp = entry.getValue();
            String rewardId = entry.getKey();

            String sql;
            if (storageMethod.equals("sqlite")) {
                sql = "INSERT OR REPLACE INTO " + TABLE_REWARDS + " "
                        + "(uuid, reward_id, last_claim, accumulated_time, granted, today_claims, last_claim_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else if (storageMethod.equals("mysql")) {
                sql = "INSERT INTO " + TABLE_REWARDS + " "
                        + "(uuid, reward_id, last_claim, accumulated_time, granted, today_claims, last_claim_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE last_claim=VALUES(last_claim), "
                        + "accumulated_time=VALUES(accumulated_time), "
                        + "granted=VALUES(granted), "
                        + "today_claims=VALUES(today_claims), "
                        + "last_claim_date=VALUES(last_claim_date)";
            } else {
                sql = "MERGE INTO " + TABLE_REWARDS + " "
                        + "(uuid, reward_id, last_claim, accumulated_time, granted, today_claims, last_claim_date) "
                        + "KEY (uuid, reward_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            }

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, rewardId);
                ps.setLong(3, rp.getLastClaim());
                ps.setLong(4, rp.getAccumulatedTime());
                ps.setBoolean(5, rp.isGranted());
                ps.setInt(6, rp.getTodayClaims());
                ps.setLong(7, rp.getLastClaimDate());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Не удалось сохранить прогресс: "
                        + uuid + "/" + rewardId, e);
            }
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
