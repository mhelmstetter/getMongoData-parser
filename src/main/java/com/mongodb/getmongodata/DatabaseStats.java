package com.mongodb.getmongodata;

import java.util.Map;
import java.util.TreeMap;

public class DatabaseStats extends BaseDatabaseStats {

    
    private Map<String, Map> raw;
    
    private Map<String, BaseDatabaseStats> shardStats = new TreeMap<String, BaseDatabaseStats>();
    
    public DatabaseStats() {
        super();
    }


    public Map<String, Map> getRaw() {
        return raw;
    }
    
    public void addShardStats(String shardName, BaseDatabaseStats stats) {
        shardStats.put(shardName, stats);
    }


    public Map<String, BaseDatabaseStats> getShardStats() {
        return shardStats;
    }
    
    

}
