package com.mongodb.getmongodata;

public class BaseDatabaseStats {
    
    private String db;
    private Long collections = 0L;
    private Long objects = 0L;
    private Double avgObjSize = 0.0;
    private Double dataSize = 0.0;
    private Double storageSize = 0.0;
    private Integer numExtents = 0;
    private Integer indexes = 0;
    private Double indexSize = 0.0;
    
    public BaseDatabaseStats() {
        
    }
    
    /**
     *  Add the provided stats to this stats
     */
    public void addStats(BaseDatabaseStats stats) {
        //System.out.println("-- " + stats.getDb());
        collections += stats.collections;
        objects += stats.objects;
        dataSize += stats.dataSize;
        storageSize += stats.storageSize;
        numExtents += stats.numExtents;
        indexes += stats.indexes;
        indexSize += stats.indexSize;
        avgObjSize = (avgObjSize + stats.avgObjSize) / 2;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public long getCollections() {
        return collections;
    }

    public void setCollections(Long collections) {
        this.collections = collections;
    }

    public Long getObjects() {
        return objects;
    }

    public void setObjects(long objects) {
        this.objects = objects;
    }

    public Double getAvgObjSize() {
        return avgObjSize;
    }

    public void setAvgObjSize(Double avgObjSize) {
        this.avgObjSize = avgObjSize;
    }

    public Double getDataSize() {
        return dataSize;
    }
    
    public Double getDataSizeGB() {
        return dataSize/1024;
    }

    public void setDataSize(Double dataSize) {
        this.dataSize = dataSize;
    }

    public Double getStorageSize() {
        return storageSize;
    }
    
    public Double getStorageSizeGB() {
        return storageSize/1024;
    }

    public void setStorageSize(Double storageSize) {
        this.storageSize = storageSize;
    }

    public Integer getNumExtents() {
        return numExtents;
    }

    public void setNumExtents(Integer numExtents) {
        this.numExtents = numExtents;
    }

    public Integer getIndexes() {
        return indexes;
    }

    public void setIndexes(Integer indexes) {
        this.indexes = indexes;
    }

    public Double getIndexSize() {
        return indexSize;
    }

    public void setIndexSize(Double indexSize) {
        this.indexSize = indexSize;
    }

    public void setObjects(Long objects) {
        this.objects = objects;
    }

}
