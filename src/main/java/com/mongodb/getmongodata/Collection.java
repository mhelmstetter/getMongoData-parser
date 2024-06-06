package com.mongodb.getmongodata;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class Collection {
	
	public Collection(Namespace ns) {
		this.namespace = ns;
	}
	
	
	private Namespace namespace;
    private String name;
    private Map<String, Object> shardKey;
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
    		
    		if (! indexes.containsKey(ixName)) {
    			indexes.put(ixName, index);
    		}
            
            if (index.getNamespace().toString().equals("core.a_t_s_note") && ixName.equals("_id")) {
            	System.out.println();
            }
    	}
    	if (! internalNameMap.containsKey(index.internalName)) {
    		internalNameMap.put(index.internalName, index);
    	}
        
    }

	public Namespace getNamespace() {
		return namespace;
	}

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}

	public Map<String, Object> getShardKey() {
		return shardKey;
	}

	public void setShardKey(Map<String, Object> shardKey) {
		this.shardKey = shardKey;
	}

}
