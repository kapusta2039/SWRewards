package com.kapusta2039.swrewards.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private long lastActivity;
    private long totalPlayTime;     
    private long firstJoin;          
    private long lastLeave;            
    private boolean newPlayer;        
    private boolean afk;           
    private long afkSince;          

    private final Map<String, RewardProgress> rewardProgress = new HashMap<>();

    private boolean welcomeBackGranted;
    private long lastWelcomeBackTime;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.firstJoin = System.currentTimeMillis();
        this.lastActivity = System.currentTimeMillis();
        this.newPlayer = true;
    }

    public UUID getUuid()                           { return uuid; }
    public long getLastActivity()                   { return lastActivity; }
    public void setLastActivity(long lastActivity)   { this.lastActivity = lastActivity; }
    public long getTotalPlayTime()                  { return totalPlayTime; }
    public void setTotalPlayTime(long totalPlayTime) { this.totalPlayTime = totalPlayTime; }
    public long getFirstJoin()                      { return firstJoin; }
    public void setFirstJoin(long firstJoin)         { this.firstJoin = firstJoin; }
    public long getLastLeave()                      { return lastLeave; }
    public void setLastLeave(long lastLeave)         { this.lastLeave = lastLeave; }
    public boolean isNewPlayer()                    { return newPlayer; }
    public void setNewPlayer(boolean newPlayer)      { this.newPlayer = newPlayer; }
    public boolean isAfk()                          { return afk; }
    public void setAfk(boolean afk)                 { this.afk = afk; }
    public long getAfkSince()                       { return afkSince; }
    public void setAfkSince(long afkSince)           { this.afkSince = afkSince; }

    public RewardProgress getProgress(String rewardId) {
        return rewardProgress.computeIfAbsent(rewardId, k -> new RewardProgress());
    }

    public Map<String, RewardProgress> getAllProgress() {
        return rewardProgress;
    }

    public boolean isWelcomeBackGranted()              { return welcomeBackGranted; }
    public void setWelcomeBackGranted(boolean val)     { this.welcomeBackGranted = val; }
    public long getLastWelcomeBackTime()               { return lastWelcomeBackTime; }
    public void setLastWelcomeBackTime(long val)       { this.lastWelcomeBackTime = val; }

    public static final class RewardProgress {
        private long lastClaim;
        private long accumulatedTime; 
        private boolean granted;    
        private int todayClaims;    
        private long lastClaimDate;

        public long getLastClaim()                  { return lastClaim; }
        public void setLastClaim(long lastClaim)    { this.lastClaim = lastClaim; }
        public long getAccumulatedTime()            { return accumulatedTime; }
        public void setAccumulatedTime(long val)    { this.accumulatedTime = val; }
        public void addAccumulatedTime(long val)    { this.accumulatedTime += val; }
        public boolean isGranted()                  { return granted; }
        public void setGranted(boolean granted)     { this.granted = granted; }
        public int getTodayClaims()                 { return todayClaims; }
        public void setTodayClaims(int val)         { this.todayClaims = val; }
        public long getLastClaimDate()              { return lastClaimDate; }
        public void setLastClaimDate(long val)      { this.lastClaimDate = val; }

        public void incrementTodayClaims() {
            long now = System.currentTimeMillis();
            if (!isSameDay(lastClaimDate, now)) {
                todayClaims = 0;
            }
            todayClaims++;
            lastClaimDate = now;
        }

        private static boolean isSameDay(long time1, long time2) {
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal1.setTimeInMillis(time1);
            cal2.setTimeInMillis(time2);
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
                && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
        }
    }
}
