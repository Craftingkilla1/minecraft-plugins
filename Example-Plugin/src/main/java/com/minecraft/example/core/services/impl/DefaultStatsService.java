// ./Example-Plugin/src/main/java/com/minecraft/example/core/services/impl/DefaultStatsService.java
package com.minecraft.example.core.services.impl;

import com.minecraft.core.utils.LogUtil;
import com.minecraft.example.config.ConfigManager;
import com.minecraft.example.core.services.StatsService;
import com.minecraft.example.sql.dao.PlayerDAO;
import com.minecraft.example.sql.dao.PlayerStatsDAO;
import com.minecraft.example.sql.models.Player;
import com.minecraft.example.sql.models.PlayerStats;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

// Use fully qualified name for Bukkit Player to avoid conflict
// import org.bukkit.entity.Player;

/**
 * Default implementation of the StatsService interface.
 */
public class DefaultStatsService implements StatsService {
    
    private final PlayerDAO playerDAO;
    private final PlayerStatsDAO statsDAO;
    private final ConfigManager configManager;
    
    public DefaultStatsService(PlayerDAO playerDAO, PlayerStatsDAO statsDAO, ConfigManager configManager) {
        this.playerDAO = playerDAO;
        this.statsDAO = statsDAO;
        this.configManager = configManager;
    }

    // Use org.bukkit.entity.Player with fully qualified name when needed
    @Override
    public void incrementStat(org.bukkit.entity.Player player, String statName) {
        incrementStat(player, statName, 1);
    }
    
    @Override
    public void incrementStat(org.bukkit.entity.Player player, String statName, int amount) {
        UUID uuid = player.getUniqueId();
        
        // Find player record
        playerDAO.findByUuid(uuid).thenAccept(optionalPlayer -> {
            if (optionalPlayer.isPresent()) {
                Player dbPlayer = optionalPlayer.get();
                
                // Find or create stat
                statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                    .thenCompose(optionalStat -> {
                        if (optionalStat.isPresent()) {
                            PlayerStats stat = optionalStat.get();
                            stat.setValue(stat.getValue() + amount);
                            return statsDAO.update(stat);
                        } else {
                            PlayerStats newStat = new PlayerStats();
                            newStat.setPlayerId(dbPlayer.getId());
                            newStat.setName(statName);
                            newStat.setValue(amount);
                            return statsDAO.save(newStat);
                        }
                    })
                    .exceptionally(e -> {
                        LogUtil.severe("Error incrementing stat: " + e.getMessage());
                        return null;
                    });
            }
        });
    }
    
    @Override
    public void setStat(org.bukkit.entity.Player player, String statName, int value) {
        UUID uuid = player.getUniqueId();
        
        playerDAO.findByUuid(uuid).thenAccept(optionalPlayer -> {
            if (optionalPlayer.isPresent()) {
                Player dbPlayer = optionalPlayer.get();
                
                statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                    .thenCompose(optionalStat -> {
                        if (optionalStat.isPresent()) {
                            PlayerStats stat = optionalStat.get();
                            stat.setValue(value);
                            return statsDAO.update(stat);
                        } else {
                            PlayerStats newStat = new PlayerStats();
                            newStat.setPlayerId(dbPlayer.getId());
                            newStat.setName(statName);
                            newStat.setValue(value);
                            return statsDAO.save(newStat);
                        }
                    })
                    .exceptionally(e -> {
                        LogUtil.severe("Error setting stat: " + e.getMessage());
                        return null;
                    });
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setStatAsync(org.bukkit.entity.Player player, String statName, int value) {
        UUID uuid = player.getUniqueId();
        
        return playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    
                    return statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                        .thenCompose(optionalStat -> {
                            if (optionalStat.isPresent()) {
                                PlayerStats stat = optionalStat.get();
                                stat.setValue(value);
                                return statsDAO.update(stat);
                            } else {
                                PlayerStats newStat = new PlayerStats();
                                newStat.setPlayerId(dbPlayer.getId());
                                newStat.setName(statName);
                                newStat.setValue(value);
                                return statsDAO.save(newStat);
                            }
                        });
                }
                return CompletableFuture.completedFuture(null);
            })
            .thenApply(result -> null);
    }
    
    @Override
    public int getStat(org.bukkit.entity.Player player, String statName) {
        UUID uuid = player.getUniqueId();
        
        Optional<Player> optionalPlayer = playerDAO.findByUuid(uuid).join();
        if (optionalPlayer.isPresent()) {
            Player dbPlayer = optionalPlayer.get();
            
            Optional<PlayerStats> optionalStat = statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName).join();
            return optionalStat.map(PlayerStats::getValue).orElse(0);
        }
        
        return 0;
    }
    
    @Override
    public CompletableFuture<Integer> getStatAsync(org.bukkit.entity.Player player, String statName) {
        UUID uuid = player.getUniqueId();
        
        return playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    
                    return statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                        .thenApply(optionalStat -> 
                            optionalStat.map(PlayerStats::getValue).orElse(0)
                        );
                }
                return CompletableFuture.completedFuture(0);
            });
    }
    
    @Override
    public Map<String, Integer> getAllStats(org.bukkit.entity.Player player) {
        UUID uuid = player.getUniqueId();
        return getAllStats(uuid);
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getAllStatsAsync(org.bukkit.entity.Player player) {
        UUID uuid = player.getUniqueId();
        return getAllStatsAsync(uuid);
    }
    
    @Override
    public Map<String, Integer> getAllStats(UUID uuid) {
        Map<String, Integer> result = new HashMap<>();
        
        Optional<Player> optionalPlayer = playerDAO.findByUuid(uuid).join();
        if (optionalPlayer.isPresent()) {
            Player dbPlayer = optionalPlayer.get();
            
            List<PlayerStats> stats = statsDAO.findAllByPlayerId(dbPlayer.getId()).join();
            for (PlayerStats stat : stats) {
                result.put(stat.getName(), stat.getValue());
            }
        }
        
        return result;
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getAllStatsAsync(UUID uuid) {
        return playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    
                    return statsDAO.findAllByPlayerId(dbPlayer.getId())
                        .thenApply(stats -> {
                            Map<String, Integer> result = new HashMap<>();
                            for (PlayerStats stat : stats) {
                                result.put(stat.getName(), stat.getValue());
                            }
                            return result;
                        });
                }
                return CompletableFuture.completedFuture(new HashMap<>());
            });
    }
    
    @Override
    public boolean resetStat(org.bukkit.entity.Player player, String statName) {
        UUID uuid = player.getUniqueId();
        
        Optional<Player> optionalPlayer = playerDAO.findByUuid(uuid).join();
        if (optionalPlayer.isPresent()) {
            Player dbPlayer = optionalPlayer.get();
            
            return statsDAO.deleteByPlayerIdAndName(dbPlayer.getId(), statName).join();
        }
        
        return false;
    }
    
    @Override
    public boolean resetAllStats(org.bukkit.entity.Player player) {
        UUID uuid = player.getUniqueId();
        
        Optional<Player> optionalPlayer = playerDAO.findByUuid(uuid).join();
        if (optionalPlayer.isPresent()) {
            Player dbPlayer = optionalPlayer.get();
            
            return statsDAO.deleteAllByPlayerId(dbPlayer.getId()).join();
        }
        
        return false;
    }
    
    @Override
    public void initializePlayer(org.bukkit.entity.Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    // Update player name if changed
                    Player dbPlayer = optionalPlayer.get();
                    if (!dbPlayer.getName().equals(name)) {
                        dbPlayer.setName(name);
                        return playerDAO.update(dbPlayer);
                    }
                    return CompletableFuture.completedFuture(dbPlayer);
                } else {
                    // Create new player
                    Player newPlayer = new Player();
                    newPlayer.setUuid(uuid);
                    newPlayer.setName(name);
                    newPlayer.setFirstJoin(LocalDateTime.now());
                    newPlayer.setLastSeen(LocalDateTime.now());
                    return playerDAO.save(newPlayer);
                }
            })
            .exceptionally(e -> {
                LogUtil.severe("Error initializing player: " + e.getMessage());
                return null;
            });
    }
    
    @Override
    public void updatePlayerActivity(org.bukkit.entity.Player player) {
        UUID uuid = player.getUniqueId();
        
        playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    dbPlayer.setLastSeen(LocalDateTime.now());
                    return playerDAO.update(dbPlayer);
                }
                return CompletableFuture.completedFuture(null);
            })
            .exceptionally(e -> {
                LogUtil.severe("Error updating player activity: " + e.getMessage());
                return null;
            });
    }
    
    @Override
    public Map<String, Integer> getTopPlayers(String statName, int limit) {
        Map<String, Integer> result = new LinkedHashMap<>();
        
        List<PlayerStats> topStats = statsDAO.findTopByStatName(statName, limit).join();
        
        for (PlayerStats stat : topStats) {
            // Use findById instead of findByPlayerId
            Optional<Player> optionalPlayer = playerDAO.findById(stat.getPlayerId()).join();
            if (optionalPlayer.isPresent()) {
                result.put(optionalPlayer.get().getName(), stat.getValue());
            }
        }
        
        return result;
    }
    
    @Override
    public CompletableFuture<Map<String, Integer>> getTopPlayersAsync(String statName, int limit) {
        return statsDAO.findTopByStatName(statName, limit)
            .thenCompose(topStats -> {
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                Map<String, Integer> result = new LinkedHashMap<>();
                
                for (PlayerStats stat : topStats) {
                    CompletableFuture<Void> future = playerDAO.findById(stat.getPlayerId())
                        .thenAccept(optionalPlayer -> {
                            if (optionalPlayer.isPresent()) {
                                synchronized (result) {
                                    result.put(optionalPlayer.get().getName(), stat.getValue());
                                }
                            }
                        });
                    futures.add(future);
                }
                
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> result);
            });
    }
    
    @Override
    public int getPlayerRank(org.bukkit.entity.Player player, String statName) {
        UUID uuid = player.getUniqueId();
        
        Optional<Player> optionalPlayer = playerDAO.findByUuid(uuid).join();
        if (optionalPlayer.isPresent()) {
            Player dbPlayer = optionalPlayer.get();
            
            Optional<PlayerStats> optionalStat = statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName).join();
            if (optionalStat.isPresent()) {
                PlayerStats stat = optionalStat.get();
                
                return statsDAO.getStatRanking(statName, stat.getValue()).join();
            }
        }
        
        return -1;
    }
    
    @Override
    public void updateStats(org.bukkit.entity.Player player, Map<String, Integer> stats) {
        UUID uuid = player.getUniqueId();
        
        playerDAO.findByUuid(uuid)
            .thenAccept(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                        String statName = entry.getKey();
                        int value = entry.getValue();
                        
                        CompletableFuture<Void> future = statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                            .thenCompose(optionalStat -> {
                                if (optionalStat.isPresent()) {
                                    PlayerStats stat = optionalStat.get();
                                    stat.setValue(value);
                                    return statsDAO.update(stat);
                                } else {
                                    PlayerStats newStat = new PlayerStats();
                                    newStat.setPlayerId(dbPlayer.getId());
                                    newStat.setName(statName);
                                    newStat.setValue(value);
                                    return statsDAO.save(newStat);
                                }
                            })
                            .thenApply(result -> null);
                        
                        futures.add(future);
                    }
                    
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .exceptionally(e -> {
                            LogUtil.severe("Error updating stats: " + e.getMessage());
                            return null;
                        });
                }
            });
    }
    
    @Override
    public CompletableFuture<Void> updateStatsAsync(org.bukkit.entity.Player player, Map<String, Integer> stats) {
        UUID uuid = player.getUniqueId();
        
        return playerDAO.findByUuid(uuid)
            .thenCompose(optionalPlayer -> {
                if (optionalPlayer.isPresent()) {
                    Player dbPlayer = optionalPlayer.get();
                    
                    List<CompletableFuture<Void>> futures = new ArrayList<>();
                    
                    for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                        String statName = entry.getKey();
                        int value = entry.getValue();
                        
                        CompletableFuture<Void> future = statsDAO.findByPlayerIdAndName(dbPlayer.getId(), statName)
                            .thenCompose(optionalStat -> {
                                if (optionalStat.isPresent()) {
                                    PlayerStats stat = optionalStat.get();
                                    stat.setValue(value);
                                    return statsDAO.update(stat);
                                } else {
                                    PlayerStats newStat = new PlayerStats();
                                    newStat.setPlayerId(dbPlayer.getId());
                                    newStat.setName(statName);
                                    newStat.setValue(value);
                                    return statsDAO.save(newStat);
                                }
                            })
                            .thenApply(result -> null);
                        
                        futures.add(future);
                    }
                    
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                }
                
                return CompletableFuture.completedFuture(null);
            });
    }
}