package com.mongodb.getmongodata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class Collection {

    private String name;
    private CollectionStats collectionStats;
    
    private Map<String, Index> indexes = new LinkedHashMap<>();
    
    private Map<String, Index> internalNameMap = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CollectionStats getCollectionStats() {
        return collectionStats;
    }
    
    public Map<String, Index> getIndexes() {
        return indexes;
    }
    
    public Index getIndexByInternalName(String name) {
        return internalNameMap.get(name);
    }
    
    public void setCollectionStats(CollectionStats collectionStats) {
        this.collectionStats = collectionStats;
    }
    
    public void addIndex(Index index) {
    	if (index.key != null) {
    		String ixName = StringUtils.join(index.key.keySet(), ", ");
            indexes.put(ixName, index);
    	}
        internalNameMap.put(index.internalName, index);
    }

}
