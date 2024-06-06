package com.mongodb.getmongodata;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Index {
    
    public Index(Namespace namespace, Map<String, Object> key, String name) {
    	this.namespace = namespace;
    	this.key = key;
        this.name = name; 
    }
    
    public Index(Namespace namespace, String internalName) {
    	this.namespace = namespace;
    	this.internalName = internalName;
    }
    
    
    private Namespace namespace;
    public String internalName;
    public String name;
    public Map<String, Object> key;
    public String longName;
    
    public Double size;
    public Long accessOps = 0L;
    public Long expireAfterSeconds;
    private boolean redundant;
    private boolean hidden;
    private boolean shardKey;
    
    private Double bytesInCache = 0.0;
    
    public Set<String> accessDates = new TreeSet<String>();
    
    public String getName() {
        return name;
    }
    
    public Long getAccessOps() {
        return accessOps;
    }
    
    public Double getSize() {
        return size;
    }
    
    public void addAccessDate(String date) {
        accessDates.add(date);
    }
    
    public Set<String> getAccessDates() {
        return accessDates;
    }
    
    public String getAccessDatesStr() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = accessDates.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

	public Long getExpireAfterSeconds() {
		return expireAfterSeconds;
	}

	public void setExpireAfterSeconds(Long expireAfterSeconds) {
		this.expireAfterSeconds = expireAfterSeconds;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setKey(Map<String, Object> key) {
		this.key = key;
	}
	
	public void incrementBytesInCache(Long bytesInCache) {
		this.bytesInCache += bytesInCache;
	}

	public Double getBytesInCache() {
		return bytesInCache;
	}

	public Map<String, Object> getKey() {
		return key;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Index [ns=");
		builder.append(namespace.toString());
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	public boolean isRedundant() {
		return redundant;
	}

	public void setRedundant(boolean redundant) {
		this.redundant = redundant;
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isShardKey() {
		return shardKey;
	}

	public void setShardKey(boolean shardKey) {
		this.shardKey = shardKey;
	}
    


}
