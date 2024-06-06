package com.mongodb.getmongodata;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class GetMongoDataParser {
    
    private static Logger logger = LoggerFactory.getLogger(GetMongoDataParser.class);
    
    private final static UpdateOptions UPDATE_OPTIONS = new UpdateOptions().upsert(true);

    SimpleDateFormat indexStatsFormatter = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
    SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    SimpleDateFormat mongoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    // protected final static

    private HtmlPrinter printer;

    private String currentLine = null;
    private String currentSection = null;
    // private String next = null;
    private File[] files;
    private File currentFile;

    private File outputFile;
    private File outputDir;

    BufferedReader in;
    private Gson gson;

    private Cluster currentCluster;
    private Date reportDate;
    
    private List<Cluster> clusters = new ArrayList<Cluster>();
    
    private String dbName;
    private Map<String, Database> databases = new HashMap<>();
    private Map<String, Map<String, Object>> shardKeys = new HashMap<>();
    private Database db;
    private Collection coll;
    private Namespace ns;
    CollectionStats collStats;

    private String hostType = null;

    Pattern collectionListDbNamePattern = Pattern.compile("^.*'(.*)'");
    
    Pattern uuidPattern = Pattern.compile("UUID\\(\"(.*?)\"\\)");
    
    private MongoClient mongoClient;
    private MongoDatabase reportDb;
    
    List<Pattern> nsExcludePatterns = new ArrayList<>();
    private boolean skipSamples;
    private String statsFieldPrefix;

    public GetMongoDataParser(File[] f, String output, String connectionUri, 
    		String[] nsExcludeFilters, boolean skipSamples, String prefix) throws FileNotFoundException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/spring.xml");
        this.files = f;
        this.skipSamples = skipSamples;
        this.statsFieldPrefix = prefix;
        if (output != null) {
            outputFile = new File(output);
        }
        gson = new GsonBuilder().create();
        printer = (HtmlPrinter) applicationContext.getBean("htmlPrinter");
        
        if (connectionUri != null) {
        	ConnectionString connectionString = new ConnectionString(connectionUri);
    		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
    		MongoClient mongoClient = MongoClients.create(mongoClientSettings);
    		
    		String dbName = connectionString.getDatabase();
    		reportDb = mongoClient.getDatabase(dbName);
        }
        
        if (nsExcludeFilters != null) {
        	for (String filter : nsExcludeFilters) {
        		nsExcludePatterns.add(Pattern.compile(filter));
        	}
        }
        
    }

    private void readSection() {
        try {
            if (currentLine.contains("List of databases")) {
                currentSection = "listOfDatabases";
                listOfDatabases();
            } else if (currentLine.contains("List of collections")) {
                currentSection = "listOfCollections";
                listOfCollections();
            } else if (currentLine.contains("Database stats")) {
                currentSection = "databaseStats";
                databaseStats();
            } else if (currentLine.contains("Collection stats")) {
                currentSection = "collectionStats";
                collectionStats();
            } else if (currentLine.toLowerCase().contains("index stats")) {
                currentSection = "indexStats";
                indexStats();
            } else if (currentLine.contains("Indexes")) {
                currentSection = "indexes";
                indexes();
            } else if (currentLine.contains("Server status")) {
                currentSection = "serverStatus";
                serverStatus();
            } else if (currentLine.contains("Host info")) {
                currentSection = "hostInfo";
                hostInfo();
            } else if (currentLine.contains("isMaster")) {
                currentSection = "isMaster";
                isMaster();
            } else if (currentLine.contains("Connected to")) {
                currentSection = "connectedTo";
                hostType = StringUtils.substringAfter(currentLine, "Connected to ");
                printer.addToModel("hostType", hostType);
            } else if (currentLine.contains("Sample document")) {
                currentSection = "sampleDocument";
                sampleDocument();
            } else if (currentLine.contains("Sharded databases")) {
                currentSection = "shardedDatabases";
                shardedDatabases();
            } else if (currentLine.contains("Sharded collections")) {
                currentSection = "shardedCollections";
                shardedCollections();
            } else if (currentLine.contains("Shell hostname")) {
            	currentSection = "shellHostname";
            	shellHostname();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void shardedDatabases() throws ParseException, IOException {
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
    }
    
    private void shardedCollections() throws ParseException, IOException {

    	Object obj = parseJson();
        if (obj == null) {
            return;
        }
        List<Map<String, Object>> collsList = (List<Map<String, Object>>)obj;
        for (Map<String, Object> coll : collsList) {
        	String ns = (String)coll.get("_id");
        	
        	Boolean dropped = (Boolean)coll.get("dropped");
        	if (Boolean.TRUE.equals(dropped)) {
        		continue;
        	}
        	Map<String, Object> key = (Map<String, Object>)coll.get("key");
        	shardKeys.put(ns, key);
        }
     }
    
    private void shellHostname() throws IOException {
    	currentLine = in.readLine();
    	if (currentLine != null) {
    		
    	}
    	
    }

    @SuppressWarnings("rawtypes")
    private void serverStatus() throws ParseException, IOException, java.text.ParseException {
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
        Map jsonObject = (Map) obj;
        String localTimeString = (String) jsonObject.get("localTime");
        if (localTimeString == null) {
            // probably no permissions to run serverStatus
            printer.addToModel("reportName", "XXX");
            
            currentCluster = new Cluster();
            currentCluster.setName("cluster");
            clusters.add(currentCluster);
            printer.addToModel("version", "unkonwn");
            return;
        }
        reportDate = mongoFormatter.parse(localTimeString);
        String reportDateStr = outputFormatter.format(reportDate);
        printer.addToModel("reportDate", reportDateStr);

        currentCluster = new Cluster();
        
        clusters.add(currentCluster);
        
        Map repl = (Map) jsonObject.get("repl");
        if (repl != null) {
            String setName = (String) repl.get("setName");
            printer.addToModel("reportName", setName);
            
            currentCluster.setName(setName);
            
        }

        String version = (String) jsonObject.get("version");
        printer.addToModel("version", version);

    }
    
    @SuppressWarnings("rawtypes")
    private void hostInfo() throws ParseException, IOException, java.text.ParseException {
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
        Map jsonObject = (Map) obj;
        Map system = (Map) jsonObject.get("system");
        if (system != null) {
        	Long memSizeMB = (Long)system.get("memSizeMB");
            currentCluster.setMemorySize(memSizeMB/1024);
        }
    }

    @SuppressWarnings("rawtypes")
    private void isMaster() throws ParseException, IOException, java.text.ParseException {
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
        Map jsonObject = (Map) obj;
        String hostname = (String) jsonObject.get("me");
        printer.addToModel("hostname", hostname);
    }
    
    @SuppressWarnings("rawtypes")
    private void sampleDocument() throws ParseException, IOException, java.text.ParseException {
        String sampleDoc = readJson(false, true);
        if (skipSamples) {
        	return;
        }
        if (sampleDoc != null && collStats != null && collStats.getNs() != null) {
            String fname = collStats.getNs() + ".js";
            File sampleOut = new File(outputDir, fname);
            BufferedWriter out = new BufferedWriter(new FileWriter(sampleOut));
            out.write(sampleDoc);
            out.close();
        }
        
    }

    private void listOfCollections() throws ParseException, IOException {
        String databaseName = null;

        Matcher m = collectionListDbNamePattern.matcher(currentLine);
        if (m.find()) {
            databaseName = m.group(1);
            // System.out.println("Found value: " + m.group() );
        } else {
            System.out.println("NO MATCH");
        }

        //System.out.println("*****" + databaseName);
        Object obj = parseJson();
        List jsonArray = (List) obj;

        db = currentCluster.getDatabase(databaseName);

        if (jsonArray != null) {
        	Iterator iterator = jsonArray.iterator();
            while (iterator.hasNext()) {
                String collectionName = (String) iterator.next();
                String nsStr = databaseName + "." + collectionName;
                boolean exclude = false;
                for (Pattern p : nsExcludePatterns) {
                	Matcher nsMatcher = p.matcher(nsStr);
                    if (nsMatcher.find()) {
                    	System.out.println("Excluding " + nsStr);
                    	exclude = true;
                    }
                }
                if (exclude) {
                	continue;
                }
                
                Collection collection = new Collection(new Namespace(databaseName, collectionName));
                collection.setName(collectionName);
                Map<String, Object> x = shardKeys.get(nsStr);
                db.addCollection(collection);
            }
        } else {
        	logger.warn("Collections list is empty for db: " + databaseName);
        }
    }

    private static BaseDatabaseStats parseDbStats(Map jsonObject) {
        BaseDatabaseStats stats = new BaseDatabaseStats();
        stats.setAvgObjSize((Double) jsonObject.get("avjObjSize"));
        stats.setDataSize((Double) jsonObject.get("dataSize")/1024.0);
        Double storageSize = (Double) jsonObject.get("storageSize")/1024.0;
        stats.setStorageSize(storageSize);
        Double nindexes = (Double) jsonObject.get("indexes");
        stats.setIndexes(nindexes.intValue());
        Double indexSize = (Double) jsonObject.get("indexSize")/1024.0;
        stats.setIndexSize(indexSize);
        Double collCount = (Double) jsonObject.get("objects");
        stats.setObjects(collCount.longValue());
        Double collections = (Double) jsonObject.get("collections");
        stats.setCollections(collections.longValue());

        return stats;
    }

    @SuppressWarnings("rawtypes")
    private void databaseStats() throws ParseException, IOException {

        DatabaseStats dbStats = parseGson(DatabaseStats.class);
        if (dbStats == null) { 
        	return;
        }
        Map<String, Map> rawShardedStats = dbStats.getRaw();
        if (rawShardedStats != null) {
            Double maxColls = 0.0;
            Set<String> keys = rawShardedStats.keySet();
            for (String key : keys) {
                String shardName = StringUtils.substringBefore(key, "/");
                Map m = rawShardedStats.get(key);
                BaseDatabaseStats shardStats = parseDbStats(m);
                dbStats.addShardStats(shardName, shardStats);
                String dbName = (String) m.get("db");
                dbStats.setDb(dbName);
                Double colls = (Double) m.get("collections");
                if (colls != null && colls > maxColls) {
                    maxColls = colls;
                }
            }
            dbStats.setCollections(maxColls.longValue());

        }
        Database db = currentCluster.getDatabase(dbStats.getDb());
        db.setDbStats(dbStats);
    }
    
    private CollectionStats parseCollectionStats(Map jsonObject, boolean topLevel) {
    	CollectionStats collStats = new CollectionStats();
    	String nsStr = (String)jsonObject.get("ns");
    	if (nsStr.equals("core.a_t_s_note")) {
    		System.out.println();
    	}
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
        collStats.setSize(((Long) jsonObject.get("size")).doubleValue());
        Long storageSize = (Long) jsonObject.get("storageSize");
        collStats.setStorageSize(storageSize.doubleValue());
        
        Long nindexes = (Long) jsonObject.get("nindexes");
        collStats.setNindexes(nindexes.doubleValue());
        Long collCount = (Long) jsonObject.get("count");
        collStats.setCount(collCount.doubleValue());
        
        Boolean sharded = (Boolean) jsonObject.get("sharded");
        if (sharded != null) {
        	collStats.setSharded(sharded);
        }
        

        Number collAvgObjSize = (Number) jsonObject.get("avgObjSize");
        if (collAvgObjSize != null) {
            collStats.setAvgObjSize(collAvgObjSize.doubleValue());
        }

        Long indexSize = (Long) jsonObject.get("totalIndexSize");
        if (indexSize != null) {
            collStats.setTotalIndexSize(indexSize.doubleValue());
        }

        Map<String, Long> indexSizes = (Map<String, Long>) jsonObject.get("indexSizes");
        collStats.setIndexSizes(indexSizes);
        
        Map<String,Map> wiredTiger = (Map<String,Map>)jsonObject.get("wiredTiger");
        if (wiredTiger != null ) { // TODO fix for not sharded && !topLevel
        	
        	Map bm = wiredTiger.get("block-manager");
        	Long reuse = (Long)bm.get("file bytes available for reuse");
        	
        	Map<String,Object> cache = (Map<String,Object>)wiredTiger.get("cache");
        	Long inCache = (Long) cache.get("bytes currently in the cache");
        	
        	collStats.setFileMbAvailable(reuse.doubleValue());
        	
        	collStats.setBytesInCache(inCache.doubleValue());
        	Long read = (Long) cache.get("bytes read into cache");
        	collStats.setBytesReadIntoCache(read.doubleValue());
        	Long written = (Long) cache.get("bytes written from cache");
        	collStats.setBytesWrittenFromCache(written.doubleValue());
        	 
        }
        
        
        
        
        	Map<String,Map<String, Map>> indexDetails = (Map<String,Map<String, Map>>)jsonObject.get("indexDetails");
        	
        	if (indexDetails != null) {
        		for (Map.Entry<String, Map<String, Map>> entry : indexDetails.entrySet()) {
            		String ixName = entry.getKey();
            		Map<String, Map> details = entry.getValue();
            		Index ix = coll.getIndexByInternalName(ixName);
            		//Index ix = coll.getIndexes().get(ixName);
            		if (ix == null) {
                		ix = new Index(collStats.getNs(), ixName);
                        coll.addIndex(ix);
                	}
            		Map detailsEntries = details.get("cache");
            		ix.incrementBytesInCache((Long)detailsEntries.get("bytes currently in the cache"));
            		
            	}
        	}
        	
    	return collStats;
    }

    @SuppressWarnings("rawtypes")
    private void collectionStats() throws ParseException, IOException {
        Object obj = parseJson();
        if (obj == null) {
        	return;
        }
        Map jsonObject = (Map) obj;

        // CollectionStats collStats = parseGson(CollectionStats.class);
        collStats = parseCollectionStats(jsonObject, true);
        
        if (coll == null) {
        	// ignore, could be filtered
            //System.out.println("***** Collection is null for " + collStats.getCollectionName());
        	return;
        } else {
        	coll.setCollectionStats(collStats);
        }
        
        
        
        Map<String,Map> shardStatsJson = (Map<String,Map>)jsonObject.get("shards");
        if (shardStatsJson != null) {
        	for (Map.Entry<String, Map> entry : shardStatsJson.entrySet()) {
            	BaseCollectionStats shardStats = parseCollectionStats(entry.getValue(), false);
            	if (shardStats != null) {
            		
            		collStats.addShardStats(entry.getKey(), shardStats);
            		
            	}
            }
        }
        
        
        if (collStats.isSharded()) {
        	printer.addToModel("sharded", true);
            db.setShardingEnabled(true);
        } else {
        	db.setPrimaryShard((String) jsonObject.get("primary"));
        }

        Map<String, BaseCollectionStats> shards = collStats.getShards();
        if (shards != null) {
            int count = 0;
            double total = 0.0;
            for (BaseCollectionStats shardStats : shards.values()) {
                Double avgObjSize = shardStats.getAvgObjSize();
                if (avgObjSize != null) {
                    total += avgObjSize;
                }

                count++;
            }
            Double avg = total / count;
            //System.out.println(avg);
            // workaround for SERVER-19533
            collStats.setAvgObjSize(avg);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void indexes() throws ParseException, IOException {
    	if (coll == null) {
    		return;
    	}
    	if (coll.getCollectionStats() == null) {
    		System.out.println();
    	}
        List indexesArray = (List) parseJson();

        if (indexesArray == null) {
            return;
        }
        for (Iterator<Map> i = indexesArray.iterator(); i.hasNext();) {
            Map indexJson = i.next();

            Boolean hidden = (Boolean)indexJson.get("hidden");
            Long expire = (Long)indexJson.get("expireAfterSeconds");
            
            String name = (String)indexJson.get("name");
            //String nsStr = (String)indexJson.get("ns");
            Namespace ns = new Namespace(db.getName(), coll.getName());
            
            Map<String, Object> key = (Map) indexJson.get("key");
            
            //Set entries = key.entrySet();
            StringBuilder sb = new StringBuilder();
            for (Iterator<String> i2 = key.keySet().iterator(); i2.hasNext();) {
            	String k = i2.next();
            	Object value = key.get(k);
            	if (value instanceof String && value.equals("hashed")) {
            		sb.append(k);
            		sb.append(":hashed");
            	} else {
            		sb.append(k);
            	}
            	if (i2.hasNext()) {
            		sb.append(", ");
            	}
            	
            }
            
    
            String internalName = (String)indexJson.get("name");
            Index ix = coll.getIndexByInternalName(internalName);
            if (ix == null) {
            	ix = new Index(ns, key, sb.toString());
            } else {
            	ix.setKey(key);
            	ix.setName(sb.toString());
            }
            
            if (Boolean.TRUE.equals(hidden)) {
            	ix.setHidden(true);
            }
            
            ix.setExpireAfterSeconds(expire);
            ix.internalName = name;
            coll.addIndex(ix);

            String xName = (String) indexJson.get("name");
            Map<String, Long> ixSizes = coll.getCollectionStats().getIndexSizes();
            Long size = ixSizes.get(xName);
            if (size != null) {
                ix.size = size.doubleValue();
            }
            
        }
    }

    private void indexStats() throws ParseException, IOException, java.text.ParseException {
    	if (coll == null) {
    		return;
    	}
    	
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
        Map jsonObject = (Map) obj;
        
        Map cursor = (Map)jsonObject.get("cursor");
        
        List statsList = null;
        
        if (cursor != null) {
            statsList = (List)cursor.get("firstBatch");
        } else {
            return;
        }
        
        Iterator<Map> statsListIterator = statsList.iterator();
        while (statsListIterator.hasNext()) {
            Map statsJson = statsListIterator.next();
            
            Map<String, Object> indexKeyJson = (Map<String, Object>)statsJson.get("key");
            String ixName = StringUtils.join(indexKeyJson.keySet(), ", ");
            Index ix = coll.getIndexes().get(ixName);
            if (ix == null) {
                System.out.println(ixName + " not found - inconsistent indexes?");
                continue;
            } else {
                //System.out.println("xx: " + coll.getName() + " " + ixName);
            }
            
            List ixStatsList = (List)statsJson.get("stats");
            Long totalAccesses = 0L;
            
            Iterator<Map> ixStatsListIterator = ixStatsList.iterator();
            while (ixStatsListIterator.hasNext()) {
                Map ixStatsJson = ixStatsListIterator.next();
                
                Long accesses = (Long)ixStatsJson.get("accesses");
                totalAccesses += accesses;
                String statsDate = (String)ixStatsJson.get("since");
                Date date = indexStatsFormatter.parse(statsDate);
                ix.addAccessDate(outputFormatter.format(date));
            }
            
            
            ix.accessOps += totalAccesses;
            
            if (coll.getName().equals("a_t_s_note") && ixName.equals("_id")) {
            	System.out.println("xx: " + coll.getName() + " " + ixName);
            }
        }
        
        /*
        IndexStatsResult indexStatsResult = parseGson(IndexStatsResult.class);
        List<IndexStats> indexStatsList = indexStatsResult.getResult();
        if (indexStatsList == null) {
            return;
        }
        for (IndexStats indexStats : indexStatsList) {
            String ixName = StringUtils.join(indexStats.key.keySet(), ", ");
            Index ix = coll.getIndexes().get(ixName);
            if (ix == null) {
                System.out.println(ixName + " not found");
            }

            Long totalAccesses = 0L;
            for (IndexStats.Stats stats : indexStats.stats) {
                totalAccesses += stats.accesses;
                Date date = indexStatsFormatter.parse(stats.since);
                ix.addAccessDate(outputFormatter.format(date));
            }
            ix.accessOps = totalAccesses;
            // coll.addIndex(ix);
        }
        */
    }

    private void listOfDatabases() throws ParseException, IOException {
        Object obj = parseJson();
        if (obj == null) {
            return;
        }
        Map jsonObject = (Map) obj;
        // Object x = jsonObject.get("databases");
        List databasesJson = (List) jsonObject.get("databases");
        this.currentCluster = new Cluster();
        //currentCluster.setDatabases(currentDatabases);

        Iterator<Map> iterator = databasesJson.iterator();
        while (iterator.hasNext()) {
            Map dbJson = iterator.next();
            
            // JSONObject dbJson = iterator.next();
            // System.out.println(reportDb.toJSONString());
            String dbName = (String) dbJson.get("name");
            
            Database db = databases.get(dbName);
            
            if (db == null) {
            	db = new Database(dbName);
            	databases.put(dbName, db);
            }
            currentCluster.addDatabase(db);
            
            Object sizeObj = dbJson.get("sizeOnDisk");
            if (sizeObj == null) {
            	continue;
            }
            
            db.setSizeOnDisk((Long)sizeObj);
        }
        // System.out.println(jsonObject.toJSONString());
    }

    private Object parseJson() throws ParseException, IOException {
        JSONParser parser = new JSONParser();
        ContainerFactory orderedKeyFactory = new ContainerFactory() {
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }

        };

        String json = readJson();
        if (json == null) {
        	return null;
        }
        // System.out.println("json: " + json);

        Object result = null;
        try {
            result = parser.parse(json, orderedKeyFactory);
        } catch (ParseException pe) {
        	// ignore these
            if (! json.startsWith("Error running function")) {
            	logger.warn(currentSection + " parse error " + pe.getMessage());
            }
            
        } catch (Exception pe) {
        	logger.warn(currentSection + " parse error " + pe.getMessage());
        }
        return result;

    }

    public <T> T parseGson(String json, Class<T> classOfT) throws IOException {
        return gson.fromJson(json, classOfT);
    }

    public <T> T parseGson(Class<T> classOfT) throws IOException {
        String json = readJson();
        return parseGson(json, classOfT);
    }
    
    private String readJson() throws IOException {
        return readJson(true, false);
    }
    
    private void normalizeJson() {
        if (currentLine.contains("NumberLong(")) {
            currentLine = currentLine.replaceAll("NumberLong\\(.*?([0-9]+).*?\\)", "$1");
        }
        if (currentLine.contains("NumberDecimal(")) {
            currentLine = currentLine.replaceAll("NumberDecimal\\(.*?([0-9]+).*?\\)", "$1");
        }
        if (currentLine.contains("ISODate(")) {
            currentLine = currentLine.replaceAll("ISODate\\((\".*\")\\)", "$1");
        }
        if (currentLine.contains("Timestamp(")) {
            currentLine = currentLine.replaceAll("Timestamp\\((.*),.*\\)", "$1");
        }

        if (currentLine.contains("ObjectId(")) {
            currentLine = currentLine.replaceAll("ObjectId\\((\".*\")\\)", "$1");
        }
        if (currentLine.contains("BinData(")) {
            currentLine = currentLine.replaceAll("BinData\\([0-9]+,(\".*\")\\)", "$1");
        }
        if (currentLine.contains("UUID(")) {
            currentLine = currentLine.replaceAll("UUID\\(\"(.*?)\"\\)", "\"$1\"");
        }
    }

    private String readJson(boolean normalize, boolean preserveLinefeeds) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        int lineNum = 0;
        while ((currentLine = in.readLine()) != null) {
            
            if (lineNum == 0 && currentLine.trim().equals("null")) {
                return null;
            }

            if (normalize) {
                normalizeJson();
            }

            if (currentLine.length() == 0) {
                continue;
            }
            // System.out.println("-- " + currentLine);
            if (currentLine.startsWith("}") || currentLine.startsWith("]")) {
                sb.append(currentLine);
                break;
            }
            if (currentLine.startsWith("{") && currentLine.endsWith("}")) {
                sb.append(currentLine);
                break;
            }
            if (currentLine.startsWith("[") && currentLine.endsWith("]")) {
                sb.append(currentLine);
                break;
            }

            sb.append(currentLine);
            sb.append("\n");

        }
        return sb.toString();
    }

    private void parseSingle(File file) throws IOException {
        in = new BufferedReader(new FileReader(file));

        while ((currentLine = in.readLine()) != null) {

            if (currentLine.length() == 0) {
                continue;
            }
            if (currentLine.startsWith("**")) {
                //System.out.println(currentLine);
                readSection();
            }

        }
        in.close();
        currentCluster.calculateStats();
        
        for (Entry<String, Map<String, Object>> e : shardKeys.entrySet()) {
        	String nsStr = e.getKey();
        	Namespace ns = new Namespace(nsStr);
        	Database db = databases.get(ns.getDatabaseName());
        	Collection coll = db.getCollection(ns.getCollectionName());
        	if (coll == null) {
        		System.out.println("Collection " + nsStr + " not found");
        		continue;
        	}
        	coll.setShardKey(e.getValue());
        	
        	for (Index ix : coll.getIndexes().values()) {
        		if (ix.getKey().equals(e.getValue())) {
        			ix.setShardKey(true);
        		}
        	}
        }
        
    }

    public void parseCombineMulti() throws IOException {
        for (File file : files) {
            logger.debug("------ parse " + file);
            parseSingle(file);
        }
        printer.addToModel("clusters", clusters);
        printer.print(outputFile);
        printer.clearModel();
    }
    
    public void parseNoCombine() throws IOException {
        logger.debug("parseNoCombine()");
        for (File file : files) {
            this.currentFile = file;
            
            
            File output = null;
            if (outputFile != null) {
                output = outputFile;
                outputDir = outputFile.getParentFile();
            } else {
                String baseName = FilenameUtils.getBaseName(file.getName());
                if (baseName != null && baseName.length() > 0) {
                    outputDir = file.getParentFile();
                    output = new File(outputDir, baseName + ".html");
                } else {
                    outputDir = new File("/tmp/");
                    output = new File(outputDir, "getmongodata.html");
                }
            }
            parseSingle(file);
            
            printer.addToModel("clusters", clusters);
            printer.addToModel("databases", currentCluster);
            
            printer.print(output);
            printer.clearModel();
        }
    }
    
    private void saveToExcel() throws  IOException {
    	File currDir = new File(".");
    	String path = currDir.getAbsolutePath();
    	String fileLocation = path.substring(0, path.length() - 1) + "oplog.xlsx";
    	
    	File file = new File(fileLocation);
    	Workbook workbook = null;
    	if (file.exists()) {
    		FileInputStream fis = new FileInputStream(file);
    		workbook = new XSSFWorkbook(fis);
    	} else {
    		workbook = new XSSFWorkbook();
    	}
    	
    	Sheet sheet = workbook.createSheet("collStats");

    	Row header = sheet.createRow(0);

    	CellStyle headerStyle = workbook.createCellStyle();
    	headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
    	headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    	XSSFFont font = ((XSSFWorkbook) workbook).createFont();
    	font.setFontName("Arial");
    	font.setFontHeightInPoints((short) 16);
    	font.setBold(true);
    	headerStyle.setFont(font);

    	int c = 0;
    	Cell headerCell = header.createCell(c++);
    	headerCell.setCellValue("Namespace");
    	headerCell.setCellStyle(headerStyle);

    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("op");
    	headerCell.setCellStyle(headerStyle);
    	
    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("count");
    	headerCell.setCellStyle(headerStyle);
    	
    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("min");
    	headerCell.setCellStyle(headerStyle);
    	
    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("max");
    	headerCell.setCellStyle(headerStyle);
    	
    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("avg");
    	headerCell.setCellStyle(headerStyle);
    	
    	headerCell = header.createCell(c++);
    	headerCell.setCellValue("total");
    	headerCell.setCellStyle(headerStyle);
    	
    	int rowNum = 1;
    	
    	java.util.Collection<Database> dbs = currentCluster.getDatabases();
    	for (Database db : dbs) {
    		for (Collection coll : db.getCollections()) {
    			BaseCollectionStats cs = coll.getCollectionStats();
    			if (cs != null) {
    				Row row = sheet.createRow(rowNum++);
    	    		Cell cell = row.createCell(0);
    	    		cell.setCellValue(cs.getNs().getDatabaseName());

    	    		cell = row.createCell(1);
    	    		cell.setCellValue(cs.getNs().getCollectionName());
    			} else {
    				logger.debug("Ignoring collection {}, no stats", coll);
    			}
    			
    		}
    	}
    	
    	FileOutputStream outputStream = new FileOutputStream(file);
    	workbook.write(outputStream);
    	workbook.close();
    	outputStream.close();
    }
    
    private String getPrefixedName(String name) {
    	if (statsFieldPrefix != null) {
    		return statsFieldPrefix + name;
    	} else {
    		return name;
    	}
    }
    
    private void saveIndexStats(MongoCollection<Document> statsColl, Index ix) {
    	
    	Namespace ns = ix.getNamespace();
    	String nsStr = ns.getNamespace();
    	String id = nsStr + "-" + ix.internalName;
    	Document key = new Document("_id", id);
    	
    	List<Bson> updates = new ArrayList<>();
    	//updates.add(set("db", ns.getDatabaseName()));
		//updates.add(set("collection", ns.getCollectionName()));
    	updates.add(set("ns", nsStr));
    	updates.add(set("hidden", ix.isHidden()));
    	updates.add(set("shardKey", ix.isShardKey()));
    	updates.add(set("redundant", ix.isRedundant()));
    	updates.add(set("accesses", ix.accessOps));
    	updates.add(set("size", ix.size));
    	updates.add(set("name", ix.internalName));
    	
    	Bson update = combine(updates);
		statsColl.updateOne(key, update, UPDATE_OPTIONS);
    }
    
    private void saveStatsToDb(MongoCollection<Document> statsColl, BaseCollectionStats cs, String shardName) {
    	if (cs != null) {
    		List<Bson> updates = new ArrayList<>();
    		updates.add(set("db", cs.getNs().getDatabaseName()));
    		updates.add(set("collection", cs.getNs().getCollectionName()));
    		updates.add(set(getPrefixedName("count"), cs.getCount()));
    		updates.add(set(getPrefixedName("avgObjSize"), cs.getAvgObjSize()));
    		updates.add(set(getPrefixedName("dataSizeGB"), cs.getSize()/1024));
    		updates.add(set(getPrefixedName("storageSizeGB"), cs.getStorageSize()/1024));
    		updates.add(set(getPrefixedName("indexSizeGB"), cs.getTotalIndexSize()/1024));
    		updates.add(set(getPrefixedName("totalStorageSizeGB"), (cs.getStorageSize() + cs.getTotalIndexSize())/1024));
    		updates.add(set("sharded", cs.isSharded()));
		
			
			Document key = null;
			if (shardName != null) {
				key = new Document("_id", shardName + ":" + cs.getNs().toString());
				updates.add(set("shardName", shardName));
			} else {
				key = new Document("_id", cs.getNs().toString());
			}
			
			Bson update = combine(updates);
			statsColl.updateOne(key, update, UPDATE_OPTIONS);
    	}
    	
    }
    
    public static List<String> findPrefixes(List<String> strings) {
        List<String> prefixes = new ArrayList<>();

        for (String str1 : strings) {
            for (String str2 : strings) {
                if (!str1.equals(str2) && str2.startsWith(str1)) {
                    prefixes.add(str1);
                    break;
                }
            }
        }

        return prefixes;
    }
    
    private void saveReportToDb() {
    	///String reportDateStr = ymdFormat.format(reportDate);
    	MongoCollection<Document> statsCollection = this.reportDb.getCollection("collStats");
    	MongoCollection<Document> shardStatsCollection = this.reportDb.getCollection("shardStats");
    	MongoCollection<Document> indexStatsCollection = this.reportDb.getCollection("indexStats");
    	java.util.Collection<Database> dbs = currentCluster.getDatabases();
    	for (Database db : dbs) {
    		for (Collection coll : db.getCollections()) {
    			
    			Map<String, Index> indexesMap = coll.getIndexes();
    			java.util.Collection<Index> indexes = indexesMap.values();
    			
    			if (indexes.size() > 1) {
    				
    				List<String> keys = new ArrayList<>(indexes.size());
    				
    				for (Index i : indexes) {
    					if (i.getKey().size() > 1) {
    						keys.add(i.getName());
    					}
        			}
    				
    				
					List<String> prefixes = findPrefixes(keys);
					if (prefixes.size() > 0) { 
						for (String prefixed : prefixes) {
							Index redundantIx = indexesMap.get(prefixed);
							if (redundantIx != null) {
								logger.debug("redundant index: {}, size: {}", redundantIx.toString(), String.valueOf(redundantIx.size));
								redundantIx.setRedundant(true);
							} else {
								logger.warn("did not find prefixed index in index set: [{}]", prefixed);
							}
						}
					}
    				
    				for (Index i : indexes) {
    					saveIndexStats(indexStatsCollection, i);
        			}
    				
    			}
    			
    			
    			
    			CollectionStats cs = coll.getCollectionStats();
    			if (cs != null) {
    				saveStatsToDb(statsCollection, cs, null);
        			
        			if (cs.getShards() != null) {
        				for (Map.Entry<String, BaseCollectionStats> entry : cs.getShards().entrySet()) {
            				String shardName = entry.getKey();
            				BaseCollectionStats s = entry.getValue();
            				saveStatsToDb(shardStatsCollection, s, shardName);
            				//System.out.println(shardName);
            			}
        			}
        			
    			} else {
    				logger.debug("Ignoring collection {}, no stats", coll);
    			}
    			
    		}
    	}
    	
    }

    public void parse() throws IOException {

        if (files.length > 0 && outputFile != null) {
            parseCombineMulti();
        } else {
            parseNoCombine();
        }
        if (reportDb != null) {
        	saveReportToDb();
        }
    }

}
