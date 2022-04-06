package com.mongodb.getmongodata;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class Cluster {
    
    private String name;
    private Long memorySize;
    
    private BaseDatabaseStats stats = new BaseDatabaseStats();
    
    private Map<String, Database> databases = new TreeMap<String, Database>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<Database> getDatabases() {
        return databases.values();
        
    }

    public void addDatabase(Database database) {
        databases.put(database.getName(), database);
    }

    public Database getDatabase(String databaseName) {
    	if (databaseName != null) {
    		return databases.get(databaseName);
    	} else { 
    		return null;
    	}
        
    }
    
    public BaseDatabaseStats getStats() {
        return this.stats;
    }
    
    public void calculateStats() {
        for (Database database : databases.values()) {
            if (! database.getName().equals("local")) {
            	DatabaseStats s = database.getDbStats();
            	if (s != null) {
            		stats.addStats(s);
            	} else {
            		System.out.println("stats is null for " + database.getName());
            	}
                
            }
        }
    }

    public void setMemorySize(Long d) {
        this.memorySize = d;
    }
    
    public Long getMemorySize() {
        return memorySize;
    }

}
