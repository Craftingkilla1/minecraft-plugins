// ./Example-Plugin/src/main/java/com/minecraft/example/database/migration/AddIndexesMigration.java
package com.minecraft.example.database.migration;

import com.minecraft.sqlbridge.api.migration.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration to add indexes for performance optimization.
 */
public class AddIndexesMigration implements Migration {
    
    @Override
    public int getVersion() {
        return 2;
    }
    
    @Override
    public String getDescription() {
        return "Add indexes for improved query performance";
    }
    
    @Override
    public void migrate(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Indexes for stats table
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_stat_name ON stats (stat_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_player_stat ON stats (player_id, stat_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_value ON stats (stat_value DESC)");
            
            // Indexes for players table
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON players (name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_last_seen ON players (last_seen DESC)");
            
            // Indexes for achievements
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_achievements_category ON achievements (category)");
            
            // Indexes for player_achievements
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_achievements_player ON player_achievements (player_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_achievements_earned ON player_achievements (earned_date DESC)");
            
            // Indexes for leaderboards
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboards_category ON leaderboards (category)");
            
            // Indexes for leaderboard_entries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_leaderboard ON leaderboard_entries (leaderboard_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_rank ON leaderboard_entries (leaderboard_id, rank)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_leaderboard_entries_score ON leaderboard_entries (score DESC)");
        }
    }
    
    @Override
    public void rollback(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Drop indexes for stats table
            stmt.execute("DROP INDEX IF EXISTS idx_stats_stat_name");
            stmt.execute("DROP INDEX IF EXISTS idx_stats_player_stat");
            stmt.execute("DROP INDEX IF EXISTS idx_stats_value");
            
            // Drop indexes for players table
            stmt.execute("DROP INDEX IF EXISTS idx_players_name");
            stmt.execute("DROP INDEX IF EXISTS idx_players_last_seen");
            
            // Drop indexes for achievements
            stmt.execute("DROP INDEX IF EXISTS idx_achievements_category");
            
            // Drop indexes for player_achievements
            stmt.execute("DROP INDEX IF EXISTS idx_player_achievements_player");
            stmt.execute("DROP INDEX IF EXISTS idx_player_achievements_earned");
            
            // Drop indexes for leaderboards
            stmt.execute("DROP INDEX IF EXISTS idx_leaderboards_category");
            
            // Drop indexes for leaderboard_entries
            stmt.execute("DROP INDEX IF EXISTS idx_leaderboard_entries_leaderboard");
            stmt.execute("DROP INDEX IF EXISTS idx_leaderboard_entries_rank");
            stmt.execute("DROP INDEX IF EXISTS idx_leaderboard_entries_score");
        }
    }
}