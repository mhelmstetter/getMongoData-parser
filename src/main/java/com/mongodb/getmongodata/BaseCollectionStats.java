package com.mongodb.getmongodata;

import java.util.Map;

public class BaseCollectionStats {
	
	public final static Double ONE_GB = 1024.0 * 1024 * 1024;
    Double count;
    Double size = 0.0;
    Double avgObjSize = 0.0;
    boolean sharded;

	private Double storageSize;
	private boolean capped;
	private Double nindexes;
	private Double totalIndexSize;
	private Map<String, Long> indexSizes;
	private Namespace namespace;
	
	
	
	private Double bytesInCache = 0.0;
	private Double bytesReadIntoCache = 0.0;
	private Double bytesWrittenFromCache = 0.0;
	
	private Double fileMbAvailable = 0.0;
	

	public BaseCollectionStats() {
		super();
	}

	public String getDatabaseName() {
		if (namespace == null) {
			return null;
		}
		return namespace.getDatabaseName();
	}

	public String getCollectionName() {
		if (namespace == null) {
			return null;
		}
		return namespace.getCollectionName();
	}

	public void setNs(Namespace ns) {
		this.namespace = ns;
	}

	public Namespace getNs() {
		return this.namespace;
	}

	public String getCompression() {
		Double result = size / storageSize;

		if (result.isNaN() || result.isInfinite()) {
			return "-";
		}
		return String.format("%,.2f", result);
	}

	public Double getCount() {
		return count;
	}

	public void setCount(Double count) {
		this.count = count;
	}

	public Double getAvgObjSize() {
		return avgObjSize;
	}

	public void setAvgObjSize(Double avgObjSize) {
		this.avgObjSize = avgObjSize;
	}

	public Double getStorageSize() {
		return storageSize;
	}

	public void setStorageSize(Double storageSize) {
		this.storageSize = storageSize;
	}

	public boolean isCapped() {
		return capped;
	}

	public void setCapped(boolean capped) {
		this.capped = capped;
	}

	public Double getNindexes() {
		return nindexes;
	}

	public void setNindexes(Double nindexes) {
		this.nindexes = nindexes;
	}

	public Double getTotalIndexSize() {
		return totalIndexSize;
	}

	public void setTotalIndexSize(Double totalIndexSize) {
		this.totalIndexSize = totalIndexSize;
	}

	public Double getSize() {
		return size;
	}

	public void setSize(Double size) {
		this.size = size;
	}

	public Double getBytesInCache() {
		return bytesInCache;
	}

	public Map<String, Long> getIndexSizes() {
		return indexSizes;
	}

	public void setIndexSizes(Map<String, Long> indexSizes) {
		this.indexSizes = indexSizes;
	}

	public boolean isSharded() {
		return sharded;
	}

	public void setSharded(boolean sharded) {
		this.sharded = sharded;
	}

	public Double getDataSizeGB() {
		return size / 1024;
	}

	public Double getStorageSizeGB() {
		return storageSize / 1024;
	}

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}
	
	public void incrementBytesInCache(Double bytesInCache) {
		this.bytesInCache += bytesInCache;
	}

	public void setBytesInCache(Double bytesInCache) {
		this.bytesInCache = bytesInCache;
	}
	
	public void incrementBytesReadIntoCache(Double bytesReadIntoCache) {
		this.bytesReadIntoCache += bytesReadIntoCache;
	}

	public void setBytesReadIntoCache(Double bytesReadIntoCache) {
		this.bytesReadIntoCache = bytesReadIntoCache;
	}
	
	public void incrementBytesWrittenFromCache(Double bytesWrittenFromCache) {
		this.bytesWrittenFromCache += bytesWrittenFromCache;
	}

	public void setBytesWrittenFromCache(Double bytesWrittenFromCache) {
		this.bytesWrittenFromCache = bytesWrittenFromCache;
	}
	
	public Double getBytesWrittenFromCacheGb() {
		return bytesWrittenFromCache / ONE_GB;
	}
	
	public Double getBytesReadIntoCacheGb() {
		return bytesReadIntoCache / ONE_GB;
	}
	
	public Double getBytesInCacheGb() {
		return bytesInCache / ONE_GB;
	}

	public Double getBytesReadIntoCache() {
		return bytesReadIntoCache;
	}

	public Double getBytesWrittenFromCache() {
		return bytesWrittenFromCache;
	}

	public Double getFileMbAvailable() {
		return fileMbAvailable;
	}

	public void setFileMbAvailable(Double fileMbAvailable) {
		this.fileMbAvailable = fileMbAvailable;
	}

}