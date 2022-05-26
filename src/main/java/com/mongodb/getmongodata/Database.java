package com.mongodb.getmongodata;

import java.util.Map;
import java.util.TreeMap;

public class Database {

    private String name;
    private boolean shardingEnabled;
    private String primaryShard;
    private long sizeOnDisk;
    private Map<String, Collection> collections = new TreeMap<String, Collection>();
    private DatabaseStats dbStats;
    
    private String replicaSet;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSizeOnDisk() {
        return sizeOnDisk;
    }

    public void setSizeOnDisk(long sizeOnDisk) {
        this.sizeOnDisk = sizeOnDisk;
    }

    public void addCollection(Collection collection) {
    	String name = collection.getName();
    	if (name.startsWith("evt")) {
    		//System.out.println();
    	}
        collections.put(collection.getName(), collection);
    }
    
    public Double getTotalIndexBytesInCache() {
    	Double total = 0.0;
    	for (Collection c : collections.values()) {
    		
    		for (Index ix : c.getIndexes().values()) {
    			if (ix.getBytesInCache() != null) {
    				total += ix.getBytesInCache()/BaseCollectionStats.ONE_GB;
    			}
    		}
    	}
    	return total;
    }
    
    public Double getTotalDataBytesInCache() {
    	Double total = 0.0;
    	for (Collection c : collections.values()) {
    		CollectionStats cs = c.getCollectionStats();
    		if (cs != null) {
    			total += cs.getBytesInCacheGb();
    		}
    		
    	}
    	return total;
    }
    
    public java.util.Collection<Collection> getCollections() {
        return collections.values();
        
    }
    
    public Collection getCollection(String name) {
        return collections.get(name);
    }

    public DatabaseStats getDbStats() {
        return dbStats;
    }

    public void setDbStats(DatabaseStats dbStats) {
        this.dbStats = dbStats;
    }

    public String getPrimaryShard() {
        return primaryShard;
    }

    public void setPrimaryShard(String primaryShard) {
        this.primaryShard = primaryShard;
    }

    public boolean isShardingEnabled() {
        return shardingEnabled;
    }

    public void setShardingEnabled(boolean shardingEnabled) {
        this.shardingEnabled = shardingEnabled;
    }

    public String getReplicaSet() {
        return replicaSet;
    }

    public void setReplicaSet(String replicaSet) {
        this.replicaSet = replicaSet;
    }

}
