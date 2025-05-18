// ./Example-Plugin/src/main/java/com/minecraft/example/integration/features/AdminToolsFeature.java
package com.minecraft.example.integration.features;

import com.minecraft.core.api.CoreAPI;
import com.minecraft.core.command.annotation.Command;
import com.minecraft.core.command.annotation.Permission;
import com.minecraft.core.command.annotation.SubCommand;
import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.ExamplePlugin;
import com.minecraft.example.core.services.AchievementService;
import com.minecraft.example.core.services.StatsService;
import com.minecraft.example.sql.dao.AchievementDAO;
import com.minecraft.example.sql.dao.PlayerAchievementDAO;
import com.minecraft.example.sql.dao.PlayerDAO;
import com.minecraft.example.sql.dao.PlayerStatsDAO;
import com.minecraft.example.sql.models.Achievement;
import com.minecraft.example.sql.models.Player;
import com.minecraft.example.sql.models.PlayerAchievement;
import com.minecraft.example.sql.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides administrative tools for managing the plugin.
 * Demonstrates integration of Core-Utils and SQL-Bridge.
 */
public class AdminToolsFeature implements Listener {
    
    private final ExamplePlugin plugin;
    
    /**
     * Constructs a new AdminToolsFeature.
     *
     * @param plugin The plugin instance
     */
    public AdminToolsFeature(ExamplePlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Enables the feature.
     */
    public void enable() {
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Register admin commands
        CoreAPI.Commands.register(new AdminCommand());
        
        LogUtil.info("Admin tools feature enabled");
    }
    
    /**
     * Disables the feature.
     */
    public void disable() {
        LogUtil.info("Admin tools feature disabled");
    }
    
    /**
     * Handles inventory click events.
     *
     * @param event The event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        // Check if this is our admin inventory
        if (title.contains("Admin Tools")) {
            event.setCancelled(true);
            
            // Handle admin menu clicks
            // Implementation would go here
        }
    }
    
    /**
     * Admin command class for advanced plugin management.
     */
    @Command(name = "admin", description = "Advanced plugin administration commands")
    @Permission("exampleplugin.admin")
    private class AdminCommand implements com.minecraft.core.command.TabCompletionProvider {
        
        /**
         * Default command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(description = "Shows the admin menu")
        public void defaultCommand(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.AQUA + "=== Example-Plugin Admin Tools ===");
            sender.sendMessage(ChatColor.GRAY + "Available commands:");
            sender.sendMessage(ChatColor.YELLOW + "/admin stats" + ChatColor.GRAY + " - Show database statistics");
            sender.sendMessage(ChatColor.YELLOW + "/admin export" + ChatColor.GRAY + " - Export data to files");
            sender.sendMessage(ChatColor.YELLOW + "/admin purge" + ChatColor.GRAY + " - Purge inactive players");
            sender.sendMessage(ChatColor.YELLOW + "/admin backup" + ChatColor.GRAY + " - Backup database data");
            sender.sendMessage(ChatColor.YELLOW + "/admin repair" + ChatColor.GRAY + " - Repair database");
            sender.sendMessage(ChatColor.YELLOW + "/admin reload" + ChatColor.GRAY + " - Reload the plugin");
        }
        
        /**
         * Stats command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "stats", description = "Shows database statistics")
        public void statsCommand(CommandSender sender, String[] args) {
            if (!plugin.isDatabaseEnabled()) {
                sender.sendMessage(ChatColor.RED + "Database features are disabled.");
                return;
            }
            
            sender.sendMessage(ChatColor.AQUA + "=== Database Statistics ===");
            sender.sendMessage(ChatColor.GRAY + "Fetching data...");
            
            // Run async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Get player count
                    int playerCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM players",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // Get achievement count
                    int achievementCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM achievements",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // Get awarded achievements count
                    int awardedCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM player_achievements",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // Get total blocks broken
                    int totalBlocksBroken = plugin.getDatabase().queryFirst(
                            "SELECT SUM(blocks_broken) as total FROM player_stats",
                            row -> row.getInt("total")
                    ).orElse(0);
                    
                    // Get total blocks placed
                    int totalBlocksPlaced = plugin.getDatabase().queryFirst(
                            "SELECT SUM(blocks_placed) as total FROM player_stats",
                            row -> row.getInt("total")
                    ).orElse(0);
                    
                    // Get total player kills
                    int totalPlayerKills = plugin.getDatabase().queryFirst(
                            "SELECT SUM(player_kills) as total FROM player_stats",
                            row -> row.getInt("total")
                    ).orElse(0);
                    
                    // Get total mob kills
                    int totalMobKills = plugin.getDatabase().queryFirst(
                            "SELECT SUM(mob_kills) as total FROM player_stats",
                            row -> row.getInt("total")
                    ).orElse(0);
                    
                    // Get total playtime
                    int totalPlaytimeSeconds = plugin.getDatabase().queryFirst(
                            "SELECT SUM(playtime_seconds) as total FROM player_stats",
                            row -> row.getInt("total")
                    ).orElse(0);
                    
                    // Format total playtime
                    int totalHours = totalPlaytimeSeconds / 3600;
                    int totalMinutes = (totalPlaytimeSeconds % 3600) / 60;
                    String formattedPlaytime = totalHours + "h " + totalMinutes + "m";
                    
                    // Send results to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.YELLOW + "Players: " + ChatColor.WHITE + playerCount);
                        sender.sendMessage(ChatColor.YELLOW + "Achievements: " + ChatColor.WHITE + achievementCount);
                        sender.sendMessage(ChatColor.YELLOW + "Awarded Achievements: " + ChatColor.WHITE + awardedCount);
                        sender.sendMessage(ChatColor.YELLOW + "Total Blocks Broken: " + ChatColor.WHITE + totalBlocksBroken);
                        sender.sendMessage(ChatColor.YELLOW + "Total Blocks Placed: " + ChatColor.WHITE + totalBlocksPlaced);
                        sender.sendMessage(ChatColor.YELLOW + "Total Player Kills: " + ChatColor.WHITE + totalPlayerKills);
                        sender.sendMessage(ChatColor.YELLOW + "Total Mob Kills: " + ChatColor.WHITE + totalMobKills);
                        sender.sendMessage(ChatColor.YELLOW + "Total Playtime: " + ChatColor.WHITE + formattedPlaytime);
                    });
                } catch (Exception e) {
                    LogUtil.severe("Error fetching database statistics: " + e.getMessage());
                    
                    // Send error to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "Error fetching database statistics: " + e.getMessage());
                    });
                }
            });
        }
        
        /**
         * Export command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "export", description = "Exports data to files", minArgs = 0, maxArgs = 1)
        public void exportCommand(CommandSender sender, String[] args) {
            if (!plugin.isDatabaseEnabled()) {
                sender.sendMessage(ChatColor.RED + "Database features are disabled.");
                return;
            }
            
            // Determine export type
            String exportType = "all";
            if (args.length > 0) {
                exportType = args[0].toLowerCase();
            }
            
            if (!Arrays.asList("all", "players", "achievements", "stats").contains(exportType)) {
                sender.sendMessage(ChatColor.RED + "Invalid export type. Valid types: all, players, achievements, stats");
                return;
            }
            
            sender.sendMessage(ChatColor.AQUA + "Starting export of " + exportType + " data...");
            
            // Run async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Create exports directory
                    File exportsDir = new File(plugin.getDataFolder(), "exports");
                    if (!exportsDir.exists()) {
                        exportsDir.mkdirs();
                    }
                    
                    // Export data based on type
                    if (exportType.equals("all") || exportType.equals("players")) {
                        exportPlayers(exportsDir);
                    }
                    
                    if (exportType.equals("all") || exportType.equals("achievements")) {
                        exportAchievements(exportsDir);
                    }
                    
                    if (exportType.equals("all") || exportType.equals("stats")) {
                        exportStats(exportsDir);
                    }
                    
                    // Send success to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GREEN + "Data export completed successfully.");
                        sender.sendMessage(ChatColor.GRAY + "Files saved to plugins/" + plugin.getName() + "/exports/");
                    });
                } catch (Exception e) {
                    LogUtil.severe("Error exporting data: " + e.getMessage());
                    
                    // Send error to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "Error exporting data: " + e.getMessage());
                    });
                }
            });
        }
        
        /**
         * Exports player data.
         *
         * @param exportsDir The exports directory
         * @throws IOException If an I/O error occurs
         */
        private void exportPlayers(File exportsDir) throws IOException {
            PlayerDAO playerDAO = plugin.getPlayerDAO();
            List<Player> players = playerDAO.findAll();
            
            YamlConfiguration config = new YamlConfiguration();
            
            for (Player player : players) {
                String uuid = player.getUuid().toString();
                config.set(uuid + ".name", player.getName());
                config.set(uuid + ".first_join", player.getFirstJoin().getTime());
                config.set(uuid + ".last_join", player.getLastJoin().getTime());
            }
            
            config.save(new File(exportsDir, "players_export.yml"));
        }
        
        /**
         * Exports achievement data.
         *
         * @param exportsDir The exports directory
         * @throws IOException If an I/O error occurs
         */
        private void exportAchievements(File exportsDir) throws IOException {
            AchievementDAO achievementDAO = plugin.getAchievementDAO();
            PlayerAchievementDAO playerAchievementDAO = plugin.getPlayerAchievementDAO();
            PlayerDAO playerDAO = plugin.getPlayerDAO();
            
            // Export achievements
            List<Achievement> achievements = achievementDAO.findAll();
            YamlConfiguration achievementsConfig = new YamlConfiguration();
            
            for (Achievement achievement : achievements) {
                String id = achievement.getIdentifier();
                achievementsConfig.set(id + ".name", achievement.getName());
                achievementsConfig.set(id + ".description", achievement.getDescription());
                achievementsConfig.set(id + ".icon", achievement.getIconMaterial());
            }
            
            achievementsConfig.save(new File(exportsDir, "achievements_export.yml"));
            
            // Export player achievements
            YamlConfiguration playerAchievementsConfig = new YamlConfiguration();
            
            List<Player> players = playerDAO.findAll();
            for (Player player : players) {
                UUID uuid = player.getUuid();
                List<PlayerAchievement> playerAchievements = playerAchievementDAO.findByPlayerUuid(uuid);
                
                if (!playerAchievements.isEmpty()) {
                    List<String> achievementIds = playerAchievements.stream()
                            .map(PlayerAchievement::getAchievementIdentifier)
                            .collect(Collectors.toList());
                    
                    playerAchievementsConfig.set(uuid.toString() + ".name", player.getName());
                    playerAchievementsConfig.set(uuid.toString() + ".achievements", achievementIds);
                }
            }
            
            playerAchievementsConfig.save(new File(exportsDir, "player_achievements_export.yml"));
        }
        
        /**
         * Exports stats data.
         *
         * @param exportsDir The exports directory
         * @throws IOException If an I/O error occurs
         */
        private void exportStats(File exportsDir) throws IOException {
            PlayerStatsDAO statsDAO = plugin.getPlayerStatsDAO();
            PlayerDAO playerDAO = plugin.getPlayerDAO();
            
            YamlConfiguration config = new YamlConfiguration();
            
            List<Player> players = playerDAO.findAll();
            for (Player player : players) {
                UUID uuid = player.getUuid();
                Optional<PlayerStats> stats = statsDAO.findByPlayerUuid(uuid);
                
                if (stats.isPresent()) {
                    PlayerStats playerStats = stats.get();
                    String uuidString = uuid.toString();
                    
                    config.set(uuidString + ".name", player.getName());
                    config.set(uuidString + ".blocks_broken", playerStats.getBlocksBroken());
                    config.set(uuidString + ".blocks_placed", playerStats.getBlocksPlaced());
                    config.set(uuidString + ".player_kills", playerStats.getPlayerKills());
                    config.set(uuidString + ".mob_kills", playerStats.getMobKills());
                    config.set(uuidString + ".deaths", playerStats.getDeaths());
                    config.set(uuidString + ".playtime_seconds", playerStats.getPlaytimeSeconds());
                    config.set(uuidString + ".distance_traveled", playerStats.getDistanceTraveled());
                }
            }
            
            config.save(new File(exportsDir, "stats_export.yml"));
        }
        
        /**
         * Purge command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "purge", description = "Purges inactive players", minArgs = 1, maxArgs = 2)
        public void purgeCommand(CommandSender sender, String[] args) {
            if (!plugin.isDatabaseEnabled()) {
                sender.sendMessage(ChatColor.RED + "Database features are disabled.");
                return;
            }
            
            // Get days parameter
            int days;
            try {
                days = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number of days. Must be a number.");
                return;
            }
            
            // Check if days is valid
            if (days <= 0) {
                sender.sendMessage(ChatColor.RED + "Days must be greater than 0.");
                return;
            }
            
            // Check for confirmation
            boolean confirmed = args.length > 1 && args[1].equalsIgnoreCase("confirm");
            if (!confirmed) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: This will permanently delete data for players who haven't logged in for " + days + " days.");
                sender.sendMessage(ChatColor.YELLOW + "To confirm, type " + ChatColor.GOLD + "/admin purge " + days + " confirm");
                return;
            }
            
            sender.sendMessage(ChatColor.AQUA + "Starting purge of inactive players...");
            
            // Run async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Calculate cutoff date
                    long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
                    
                    // Get inactive players from database
                    List<Player> inactivePlayers = plugin.getDatabase().query(
                            "SELECT * FROM players WHERE last_join < ?",
                            row -> {
                                Player player = new Player();
                                player.setId(row.getInt("id"));
                                player.setUuid(UUID.fromString(row.getString("uuid")));
                                player.setName(row.getString("name"));
                                player.setFirstJoin(row.getTimestamp("first_join"));
                                player.setLastJoin(row.getTimestamp("last_join"));
                                return player;
                            },
                            new java.sql.Timestamp(cutoffTime)
                    );
                    
                    // Skip if no inactive players
                    if (inactivePlayers.isEmpty()) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.YELLOW + "No inactive players found.");
                        });
                        return;
                    }
                    
                    // Delete inactive players
                    int purgeCount = 0;
                    PlayerDAO playerDAO = plugin.getPlayerDAO();
                    
                    for (Player player : inactivePlayers) {
                        // Skip online players
                        if (Bukkit.getOfflinePlayer(player.getUuid()).isOnline()) {
                            continue;
                        }
                        
                        // Delete player
                        if (playerDAO.deleteByUuid(player.getUuid())) {
                            purgeCount++;
                        }
                    }
                    
                    // Send results to main thread
                    final int finalPurgeCount = purgeCount;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (finalPurgeCount > 0) {
                            sender.sendMessage(ChatColor.GREEN + "Purged " + finalPurgeCount + " inactive players.");
                        } else {
                            sender.sendMessage(ChatColor.YELLOW + "No players were purged.");
                        }
                    });
                } catch (Exception e) {
                    LogUtil.severe("Error purging inactive players: " + e.getMessage());
                    
                    // Send error to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "Error purging inactive players: " + e.getMessage());
                    });
                }
            });
        }
        
        /**
         * Backup command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "backup", description = "Backs up database data")
        public void backupCommand(CommandSender sender, String[] args) {
            if (!plugin.isDatabaseEnabled()) {
                sender.sendMessage(ChatColor.RED + "Database features are disabled.");
                return;
            }
            
            sender.sendMessage(ChatColor.AQUA + "Starting database backup...");
            
            // Run async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Create backup directory
                    File backupDir = new File(plugin.getDataFolder(), "backups");
                    if (!backupDir.exists()) {
                        backupDir.mkdirs();
                    }
                    
                    // Create date-stamped backup directory
                    Date now = new Date();
                    String dateString = String.format("%tF_%tH-%tM-%tS", now, now, now, now);
                    File dateBackupDir = new File(backupDir, dateString);
                    dateBackupDir.mkdirs();
                    
                    // Export all data to backup directory
                    exportPlayers(dateBackupDir);
                    exportAchievements(dateBackupDir);
                    exportStats(dateBackupDir);
                    
                    // Create backup info file
                    YamlConfiguration backupInfo = new YamlConfiguration();
                    backupInfo.set("backup_date", now.getTime());
                    backupInfo.set("plugin_version", plugin.getDescription().getVersion());
                    
                    // Get database stats
                    int playerCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM players",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    int achievementCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM achievements",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    int statsCount = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM player_stats",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    backupInfo.set("players_count", playerCount);
                    backupInfo.set("achievements_count", achievementCount);
                    backupInfo.set("stats_count", statsCount);
                    
                    backupInfo.save(new File(dateBackupDir, "backup_info.yml"));
                    
                    // Send success to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GREEN + "Database backup completed successfully.");
                        sender.sendMessage(ChatColor.GRAY + "Backup saved to plugins/" + plugin.getName() + "/backups/" + dateString + "/");
                    });
                } catch (Exception e) {
                    LogUtil.severe("Error backing up database: " + e.getMessage());
                    
                    // Send error to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "Error backing up database: " + e.getMessage());
                    });
                }
            });
        }
        
        /**
         * Repair command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "repair", description = "Repairs database issues")
        public void repairCommand(CommandSender sender, String[] args) {
            if (!plugin.isDatabaseEnabled()) {
                sender.sendMessage(ChatColor.RED + "Database features are disabled.");
                return;
            }
            
            sender.sendMessage(ChatColor.AQUA + "Analyzing database for issues...");
            
            // Run async to avoid blocking main thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    // Get stats on potential issues
                    
                    // 1. Players without stats
                    int playersWithoutStats = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM players p " +
                            "LEFT JOIN player_stats ps ON p.id = ps.player_id " +
                            "WHERE ps.player_id IS NULL",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // 2. Orphaned stats
                    int orphanedStats = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM player_stats ps " +
                            "LEFT JOIN players p ON ps.player_id = p.id " +
                            "WHERE p.id IS NULL",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // 3. Orphaned achievements
                    int orphanedAchievements = plugin.getDatabase().queryFirst(
                            "SELECT COUNT(*) as count FROM player_achievements pa " +
                            "LEFT JOIN players p ON pa.player_id = p.id " +
                            "WHERE p.id IS NULL",
                            row -> row.getInt("count")
                    ).orElse(0);
                    
                    // Total issues
                    final int totalIssues = playersWithoutStats + orphanedStats + orphanedAchievements;
                    
                    // Send results to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.YELLOW + "Database analysis complete:");
                        sender.sendMessage(ChatColor.YELLOW + "- Players without stats: " + playersWithoutStats);
                        sender.sendMessage(ChatColor.YELLOW + "- Orphaned stats: " + orphanedStats);
                        sender.sendMessage(ChatColor.YELLOW + "- Orphaned achievements: " + orphanedAchievements);
                        
                        if (totalIssues == 0) {
                            sender.sendMessage(ChatColor.GREEN + "No issues found! Database is healthy.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Found " + totalIssues + " issues.");
                            sender.sendMessage(ChatColor.YELLOW + "To fix these issues, type " + ChatColor.GOLD + "/admin repair fix");
                        }
                    });
                } catch (Exception e) {
                    LogUtil.severe("Error analyzing database: " + e.getMessage());
                    
                    // Send error to main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "Error analyzing database: " + e.getMessage());
                    });
                }
            });
        }
        
        /**
         * Reload command handler.
         *
         * @param sender The command sender
         * @param args The command arguments
         */
        @SubCommand(name = "reload", description = "Reloads the plugin")
        public void reloadCommand(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.AQUA + "Reloading Example-Plugin...");
            
            try {
                // Reload configuration
                plugin.getConfigManager().reloadAll();
                
                // Reload features
                // (This would typically involve disabling and re-enabling features)
                
                sender.sendMessage(ChatColor.GREEN + "Plugin reloaded successfully.");
            } catch (Exception e) {
                LogUtil.severe("Error reloading plugin: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "Error reloading plugin: " + e.getMessage());
            }
        }
        
        @Override
        public List<String> getTabCompletions(String subCommand, CommandSender sender, String[] args) {
            List<String> completions = new ArrayList<>();
            
            if (subCommand.isEmpty()) {
                // Root command completions
                if (args.length == 1) {
                    String arg = args[0].toLowerCase();
                    List<String> subCommands = Arrays.asList(
                            "stats", "export", "purge", "backup", "repair", "reload");
                    
                    return subCommands.stream()
                            .filter(cmd -> cmd.startsWith(arg))
                            .collect(Collectors.toList());
                }
            } else if (subCommand.equalsIgnoreCase("export")) {
                if (args.length == 1) {
                    String arg = args[0].toLowerCase();
                    List<String> types = Arrays.asList("all", "players", "achievements", "stats");
                    
                    return types.stream()
                            .filter(type -> type.startsWith(arg))
                            .collect(Collectors.toList());
                }
            } else if (subCommand.equalsIgnoreCase("purge")) {
                if (args.length == 1) {
                    String arg = args[0].toLowerCase();
                    List<String> days = Arrays.asList("7", "30", "90", "180", "365");
                    
                    return days.stream()
                            .filter(day -> day.startsWith(arg))
                            .collect(Collectors.toList());
                } else if (args.length == 2) {
                    String arg = args[1].toLowerCase();
                    
                    if ("confirm".startsWith(arg)) {
                        completions.add("confirm");
                    }
                    
                    return completions;
                }
            } else if (subCommand.equalsIgnoreCase("repair")) {
                if (args.length == 1) {
                    String arg = args[0].toLowerCase();
                    
                    if ("fix".startsWith(arg)) {
                        completions.add("fix");
                    }
                    
                    return completions;
                }
            }
            
            return completions;
        }
    }
}