package com.mongodb.getmongodata;

import java.util.HashMap;
import java.util.Map;

public class CollectionStats extends BaseCollectionStats {
    
//    "ns" : "xx.yy",
//    "count" : 57,
//    "size" : 0,
//    "avgObjSize" : 3693,
//    "storageSize" : 0,
//    "capped" : false,
//    "nindexes" : 2,
//    "totalIndexSize" : 0,
//    "indexSizes" : {
//            "_id_" : 0,
//            "xxx" : 0
//    },
    
    //private String ns;
	

    
    private Map<String, BaseCollectionStats> shards;
    
    public CollectionStats() {
		super();
        
    }
    
    public Map<String, BaseCollectionStats> getShards() {
        return shards;
    }
    
    public void addShardStats(String shardName, BaseCollectionStats stats) {
    	if (shards == null) {
    		shards = new HashMap<>();
    	}
    	
    	this.incrementBytesInCache(stats.getBytesInCache());
    	this.incrementBytesReadIntoCache(stats.getBytesReadIntoCache());
    	this.incrementBytesWrittenFromCache(stats.getBytesWrittenFromCache());
    	
    	shards.put(shardName, stats);
    }

}
