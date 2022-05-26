package com.mongodb.getmongodata;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Index {
    
    public Index(Map<String, Object> key, String name) {
        this.key = key;
        this.name = name; 
    }
    
    public Index(String internalName) {
    	this.internalName = internalName;
    }
    
    
    public String internalName;
    public String name;
    public Map<String, Object> key;
    public String longName;
    public String ns;
    public Double size;
    public Long accessOps;
    public Long expireAfterSeconds;
    
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
    


}
