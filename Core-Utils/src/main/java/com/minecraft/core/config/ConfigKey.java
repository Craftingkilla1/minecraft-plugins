package com.minecraft.core.config;

/**
 * Enum of all configuration keys with their default values and types
 */
public enum ConfigKey {
    // General settings
    BUNGEE_MODE("bungee-mode", false, Boolean.class),
    SERVER_NAME("server-name", "", String.class),
    MAIN_SERVER("main-server", "start", String.class),
    VOID_SERVER("void-server", "void", String.class),
    USE_CENTRALIZED_CONFIG("use-centralized-config", false, Boolean.class),
    DEBUG_MODE("debug-mode", false, Boolean.class),
    
    // Invite settings
    BASE_INVITE_POINTS("base-invite-points", 10.0, Double.class),
    POINTS_MULTIPLIER("points-multiplier", 0.9, Double.class),
    INVITE_CODE_LENGTH("invite-code-length", 6, Integer.class),
    INVITE_CODE_VALIDITY_HOURS("invite-code-validity-hours", 24, Integer.class),
    INVITE_LIMIT_PER_PLAYER("invite-limit-per-player", 2, Integer.class),
    MAX_INVITE_CODES_PER_PLAYER("max-invite-codes-per-player", 2, Integer.class),
    INVITE_TIMER_SECONDS("invite-timer-seconds", 7200, Integer.class),
    HIDE_ADMIN_INVITERS("hide-admin-inviters", true, Boolean.class),
    
    // Storage settings
    STORAGE_TYPE("storage.type", "file", String.class),
    MYSQL_HOST("storage.mysql.host", "localhost", String.class),
    MYSQL_PORT("storage.mysql.port", 3306, Integer.class),
    MYSQL_DATABASE("storage.mysql.database", "minecraft_invites", String.class),
    MYSQL_USERNAME("storage.mysql.username", "root", String.class),
    MYSQL_PASSWORD("storage.mysql.password", "password", String.class),
    MYSQL_TABLE_PREFIX("storage.mysql.table-prefix", "inv_", String.class),
    MYSQL_CONNECTION_POOL_SIZE("storage.mysql.connection-pool-size", 10, Integer.class),
    MYSQL_USE_SSL("storage.mysql.use-ssl", false, Boolean.class),
    
    // Cache settings
    CACHE_ENABLED("cache.enabled", true, Boolean.class),
    CACHE_MAX_SIZE("cache.max-size", 1000, Integer.class),
    CACHE_EXPIRATION_MINUTES("cache.expiration-minutes", 60, Integer.class),
    CACHE_SAVE_INTERVAL("cache.save-interval", 300, Integer.class),
    
    // Message settings
    MSG_WELCOME("messages.welcome", "&aWelcome to the server!", String.class),
    MSG_CODE_PROMPT("messages.code-prompt", "&eYou need an invite code to join this server. Please enter it in chat.", String.class),
    MSG_CODE_VALID("messages.code-valid", "&aInvite code accepted! Welcome to the server!", String.class),
    MSG_CODE_INVALID("messages.code-invalid", "&cInvalid invite code. Please try again or ask for a new code.", String.class),
    MSG_CODE_EXPIRED("messages.code-expired", "&cThis invite code has expired.", String.class),
    MSG_CODE_ALREADY_USED("messages.code-already-used", "&cThis invite code has already been used.", String.class),
    MSG_CODE_OWN("messages.code-own", "&cYou cannot use your own invite code.", String.class),
    MSG_CODE_GENERATED("messages.code-generated", "&aYour invite code is: &6{code}", String.class),
    MSG_CODE_SHARE_PROMPT("messages.code-share-prompt", "&aShare this code with your friends!", String.class),
    MSG_PLAYER_INVITED("messages.player-invited", "&a{player} has joined the server using your invite code! You earned {points} points!", String.class),
    MSG_PLAYER_JOINED("messages.player-joined", "&a{player} has joined the server!", String.class),
    MSG_INVITE_LIMIT_REACHED("messages.invite-limit-reached", "&cYou have reached your invite limit!", String.class),
    MSG_TIMER_STARTED("messages.timer-started", "&eYou have &6{time}&e to invite &6{limit}&e players.", String.class),
    MSG_TIMER_EXPIRED("messages.timer-expired", "&cYour time to invite players has expired!", String.class),
    MSG_TIMER_UPDATED("messages.timer-updated", "&eThe invite timer duration has been updated. Your timer now has &6{time}&e remaining.", String.class),
    
    // Advanced settings
    CLEANUP_INTERVAL("advanced.cleanup-interval", 60, Integer.class),
    CONFIG_SYNC_INTERVAL("advanced.config-sync-interval", 30, Integer.class),
    VERIFY_PLAYERS_PERIODICALLY("advanced.verify-players-periodically", true, Boolean.class),
    VERIFY_PLAYERS_INTERVAL("advanced.verify-players-interval", 10, Integer.class),
    MAX_INVITE_TREE_DEPTH("advanced.max-invite-tree-depth", 10, Integer.class),
    TRACK_DETAILED_STATS("advanced.track-detailed-stats", true, Boolean.class);
    
    private final String path;
    private final Object defaultValue;
    private final Class<?> type;
    
    /**
     * Create a new config key
     * 
     * @param path The path in the config file
     * @param defaultValue The default value
     * @param type The type of the value
     */
    private ConfigKey(String path, Object defaultValue, Class<?> type) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.type = type;
    }
    
    /**
     * Get the path in the config file
     * 
     * @return The path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Get the default value
     * 
     * @return The default value
     */
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Get the type of the value
     * 
     * @return The type
     */
    public Class<?> getType() {
        return type;
    }
    
    /**
     * Get a config key by its path
     * 
     * @param path The path in the config file
     * @return The config key, or null if not found
     */
    public static ConfigKey getByPath(String path) {
        for (ConfigKey key : values()) {
            if (key.getPath().equals(path)) {
                return key;
            }
        }
        return null;
    }
}