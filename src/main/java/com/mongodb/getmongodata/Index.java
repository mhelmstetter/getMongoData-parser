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
    
    public Index() {
    }
    
    
    public String name;
    public Map<String, Object> key;
    public String longName;
    public String ns;
    public Double size;
    public Long accessOps;
    public Long expireAfterSeconds;
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
    


}
