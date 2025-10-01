package com.mongodb.wellnesschecker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.getmongodata.Cluster;
import com.mongodb.getmongodata.Collection;
import com.mongodb.getmongodata.CollectionStats;
import com.mongodb.getmongodata.Database;
import com.mongodb.getmongodata.GetMongoDataParser;
import com.mongodb.getmongodata.Index;
import com.mongodb.getmongodata.Namespace;

public class MwcOutputSplitter {

	private static Logger logger = LoggerFactory.getLogger(MwcOutputSplitter.class);

	private Cluster currentCluster;
	private Map<String, Database> databases = new HashMap<>();
	private CollectionStats collStats;
	private Collection coll;
	private Namespace ns;

	private final ObjectMapper objectMapper = new ObjectMapper();

	String section;
	String subsection;
	Map<String, Object> stanza;

	public MwcOutputSplitter() {
		this.currentCluster = new Cluster();
	}

	public void splitMwcOutput(String inputFile) throws IOException {
		File file = new File(inputFile);
		List<Map<String, Object>> jsonData = objectMapper.readValue(file,
				new TypeReference<List<Map<String, Object>>>() {
				});

		String prevNs = null;
		for (Map<String, Object> st : jsonData) {
			this.stanza = st;
			section = (String) stanza.get("section");
			subsection = (String) stanza.get("subsection");

			String ns = determineNamespace(stanza, prevNs);
			prevNs = ns;

			processSubsection(ns);
		}
	}

	private String determineNamespace(Map<String, Object> stanza, String prevNs) {
		switch (subsection) {
		case "collection_stats_(mb)":
			return extractStringFromJson(stanza, "output", "ns");
		case "database_stats_(mb)":
			return extractStringFromJson(stanza, "output", "db");
		case "index_stats":
			if (stanza.containsKey("output") && stanza.get("output") instanceof Map) {
				return extractStringFromJson(stanza, "output", "cursor", "ns");
			}
			return null;
		case "indexes":
			String nsdb = extractStringFromJson(stanza, "commandParameters", "db");
			String nscoll = extractStringFromJson(stanza, "commandParameters", "collection");
			return nsdb != null && nscoll != null ? nsdb + "." + nscoll : null;
		case "shard_distribution":
			return prevNs;
		default:
			return null;
		}
	}

	private String extractStringFromJson(Map<String, Object> data, String... keys) {
		Map<String, Object> current = data;
		for (int i = 0; i < keys.length - 1; i++) {
			Object value = current.get(keys[i]);
			if (!(value instanceof Map))
				return null;
			current = (Map<String, Object>) value;
		}
		Object finalValue = current.get(keys[keys.length - 1]);
		return finalValue instanceof String ? (String) finalValue : null;
	}

	private void processSubsection(String ns) {
		String nss = ns == null ? "" : "-" + ns;
		String filename = subsection + nss + ".json";

		if (subsection.equals("list_of_databases")) {
			listOfDatabases();
		} else if (subsection.equals("collection_stats_(mb)")) {
			collectionStats();
		}

		// Replace file writing with processing logic
		System.out.println("Processing: " + filename);
		// System.out.println(stanza);
	}

	@SuppressWarnings("unchecked")
	private void listOfDatabases() {
		Map<String, Object> obj = (Map) stanza.get("output");

		List<Map<String, Object>> obj2 = (List<Map<String, Object>>) obj.get("databases");

		for (Map<String, Object> dbMap : obj2) {
			String dbName = (String) dbMap.get("name");
			Database db = databases.get(dbName);

			if (db == null) {
				db = new Database(dbName);
				databases.put(dbName, db);
			}
			currentCluster.addDatabase(db);
			Map<String, Object> sizeObj = (Map<String, Object>) dbMap.get("sizeOnDisk");
			if (sizeObj == null) {
				continue;
			}

			if (sizeObj.containsKey("$numberLong")) {
				db.setSizeOnDisk(Long.parseLong((String) sizeObj.get("$numberLong")));
			} else {
				logger.warn("sizeObj, unexpected type: {}", sizeObj);
			}

		}
	}

	private void collectionStats() {
		Map<String, Object> obj = (Map) stanza.get("output");
		collStats = parseCollectionStats(obj);
	}
	
	private double toDouble(Object obj) {
	    if (obj instanceof Number) {
	        return ((Number) obj).doubleValue();
	    } else if (obj instanceof Map) {
	    	Map map = (Map)obj;
	    	if (map.containsKey("$numberLong")) {
	    		return toDouble(map.get("$numberLong"));
	    	} else {
	    		logger.warn("**** toDouble WTF");
	    		return 0.0;
	    	}
	    } else {
	        logger.warn("Unexpected type: " + (obj == null ? "null" : obj.getClass().getName()));
	        return 0.0;
	    }
	}

	private CollectionStats parseCollectionStats(Map jsonObject) {
		CollectionStats collStats = new CollectionStats();
		String nsStr = (String) jsonObject.get("ns");
		if (nsStr != null) {
			this.ns = new Namespace(nsStr);
			collStats.setNs(ns);

			String dbName = collStats.getDatabaseName();
			Database db = currentCluster.getDatabase(dbName);
			if (db == null) {
				logger.warn("database {} not found, missing from list of databases?", dbName);
				return null;
			}
			coll = db.getCollection(collStats.getCollectionName());
		}

		Object testGoodStats = jsonObject.get("storageSize");
		if (testGoodStats == null) {
			return null;
		}

		collStats.setAvgObjSize((Double) jsonObject.get("avjObjSize"));
		Object sizeObj = jsonObject.get("size");
		collStats.setSize(toDouble(sizeObj));
		
		Double storageSize = (Double) jsonObject.get("storageSize");
		collStats.setStorageSize(storageSize);

		Integer nindexes = (Integer) jsonObject.get("nindexes");
		collStats.setNindexes(nindexes.doubleValue());
		Object collCountObj = jsonObject.get("count");
		collStats.setCount(toDouble(collCountObj));
		

		Boolean sharded = (Boolean) jsonObject.get("sharded");
		if (sharded != null) {
			collStats.setSharded(sharded);
		}

		Number collAvgObjSize = (Number) jsonObject.get("avgObjSize");
		if (collAvgObjSize != null) {
			collStats.setAvgObjSize(collAvgObjSize.doubleValue());
		}

		Double indexSize = (Double) jsonObject.get("totalIndexSize");
		if (indexSize != null) {
			collStats.setTotalIndexSize(indexSize);
		}

		Map<String, Long> indexSizes = (Map<String, Long>) jsonObject.get("indexSizes");
		collStats.setIndexSizes(indexSizes);

		Map<String, Map> wiredTiger = (Map<String, Map>) jsonObject.get("wiredTiger");
		if (wiredTiger != null) { // TODO fix for not sharded && !topLevel

			Map bm = wiredTiger.get("block-manager");
			Object reuse = bm.get("file bytes available for reuse");

			Map<String, Object> cache = (Map<String, Object>) wiredTiger.get("cache");
			 Object inCache = cache.get("bytes currently in the cache");
			 //Integer inCache = (Integer)
			 
			 if (inCache instanceof Integer) {
				collStats.setBytesInCache(((Integer)inCache).doubleValue());
			 }

			collStats.setFileMbAvailable(toDouble(reuse));

			
			Object readObj = cache.get("bytes read into cache");
			collStats.setBytesReadIntoCache(toDouble(readObj));
			
			Object written = cache.get("bytes written from cache");
			collStats.setBytesWrittenFromCache(toDouble(written));

		}

		Map<String, Map<String, Map>> indexDetails = (Map<String, Map<String, Map>>) jsonObject.get("indexDetails");

		if (indexDetails != null) {
			for (Map.Entry<String, Map<String, Map>> entry : indexDetails.entrySet()) {
				String ixName = entry.getKey();
				Map<String, Map> details = entry.getValue();
				Index ix = coll.getIndexByInternalName(ixName);
				// Index ix = coll.getIndexes().get(ixName);
				if (ix == null) {
					ix = new Index(collStats.getNs(), ixName);
					coll.addIndex(ix);
				}
				Map detailsEntries = details.get("cache");
				ix.incrementBytesInCache((Long) detailsEntries.get("bytes currently in the cache"));

			}
		}

		System.out.println();
		return collStats;
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage: java MwcOutputSplitter <inputFile>");
			System.exit(1);
		}

		MwcOutputSplitter splitter = new MwcOutputSplitter();
		try {
			splitter.splitMwcOutput(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
