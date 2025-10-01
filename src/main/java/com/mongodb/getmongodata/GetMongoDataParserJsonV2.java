package com.mongodb.getmongodata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Parser for the new consolidated JSON format where databaseStats is at the top level
 */
public class GetMongoDataParserJsonV2 {

    protected final static ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "/spring.xml");

    private File file;
    private Gson gson;

    private Cluster databases = new Cluster();

    public GetMongoDataParserJsonV2(File f) throws FileNotFoundException {
        this.file = f;
        gson = new GsonBuilder().create();
    }

    @SuppressWarnings("unchecked")
    public void parse() throws IOException, ParseException {
        System.out.println("Using GetMongoDataParserJsonV2 for file: " + file.getName());

        JSONParser parser = new JSONParser();
        ContainerFactory orderedKeyFactory = new ContainerFactory() {
            public List creatArrayContainer() {
                return new LinkedList();
            }

            public Map createObjectContainer() {
                return new LinkedHashMap();
            }
        };

        System.out.println("Parsing JSON file...");
        FileReader reader = new FileReader(file);
        Map rootMap = (Map) parser.parse(reader, orderedKeyFactory);
        reader.close();

        System.out.println("Root map keys: " + rootMap.keySet());

        // Extract database stats
        List<Map> databaseStatsList = (List<Map>) rootMap.get("databaseStats");
        if (databaseStatsList != null) {
            System.out.println("Found " + databaseStatsList.size() + " databases");
            for (Map dbStatsMap : databaseStatsList) {
                processDatabaseStats(dbStatsMap);
            }
        } else {
            System.out.println("WARNING: No databaseStats found in JSON");
        }

        // Calculate cluster stats
        databases.calculateStats();

        // Generate HTML output
        System.out.println("Generating HTML output...");
        System.out.println("Total databases processed: " + databases.getDatabases().size());
        System.out.println("Cluster stats - Data Size: " + databases.getStats().getDataSizeGB() + " GB");

        // Debug: Print first database details
        if (databases.getDatabases().size() > 0) {
            Database firstDb = databases.getDatabases().iterator().next();
            System.out.println("First database: " + firstDb.getName());
            if (firstDb.getDbStats() != null) {
                System.out.println("  Collections: " + firstDb.getDbStats().getCollections());
                System.out.println("  Data Size: " + firstDb.getDbStats().getDataSizeGB() + " GB");
            } else {
                System.out.println("  WARNING: Database stats is NULL!");
            }
        }

        HtmlPrinter printer = (HtmlPrinter)applicationContext.getBean("htmlPrinter");
        printer.addToModel("databases", databases);
        File outputFile = null;
        String baseName = FilenameUtils.getBaseName(file.getName());
        if (baseName != null && baseName.length() > 0) {
            outputFile = new File(file.getParentFile(), baseName + ".html");
        } else {
            outputFile = new File("/tmp/", "getmongodata.html");
        }
        printer.print(outputFile);
        System.out.println("HTML output written to: " + outputFile.getAbsolutePath());
    }

    @SuppressWarnings("unchecked")
    private void processDatabaseStats(Map dbStatsMap) {
        String dbName = (String) dbStatsMap.get("db");
        System.out.println("Processing database: " + dbName);
        Database database = new Database(dbName);

        Long sizeOnDisk = getLongValue(dbStatsMap.get("storageSize"));
        database.setSizeOnDisk(sizeOnDisk);

        // Create DatabaseStats object
        DatabaseStats dbStats = new DatabaseStats();
        dbStats.setDb(dbName);
        dbStats.setCollections(getLongValue(dbStatsMap.get("collections")));
        dbStats.setDataSize(getDoubleValue(dbStatsMap.get("dataSize")));
        dbStats.setStorageSize(getDoubleValue(dbStatsMap.get("storageSize")));
        dbStats.setIndexes(getIntValue(dbStatsMap.get("indexes")));
        dbStats.setIndexSize(getDoubleValue(dbStatsMap.get("indexSize")));
        dbStats.setAvgObjSize(getDoubleValue(dbStatsMap.get("avgObjSize")));

        database.setDbStats(dbStats);
        databases.addDatabase(database);

        // Process collection stats
        List<Map> collectionStatsList = (List<Map>) dbStatsMap.get("collectionstats");
        if (collectionStatsList != null) {
            System.out.println("  Found " + collectionStatsList.size() + " collections");
            for (Map collStatsMap : collectionStatsList) {
                processCollectionStats(database, collStatsMap);
            }
        } else {
            System.out.println("  No collections found for database: " + dbName);
        }
    }

    @SuppressWarnings("unchecked")
    private void processCollectionStats(Database database, Map collStatsMap) {
        String collectionName = (String) collStatsMap.get("name");
        String type = (String) collStatsMap.get("type");
        Collection collection = new Collection(new Namespace(database.getName(), collectionName));
        collection.setName(collectionName);
        collection.setType(type != null ? type : "regular");

        // Create CollectionStats object
        CollectionStats collStats = new CollectionStats();
        collStats.setNs(new Namespace(database.getName(), collectionName));
        collStats.setCount(getDoubleValue(collStatsMap.get("count")));
        collStats.setSize(getDoubleValue(collStatsMap.get("dataSize")));
        collStats.setAvgObjSize(getDoubleValue(collStatsMap.get("avgObjSize")));
        collStats.setStorageSize(getDoubleValue(collStatsMap.get("storageSize")));
        collStats.setNindexes(getDoubleValue(collStatsMap.get("nindexes")));
        collStats.setTotalIndexSize(getDoubleValue(collStatsMap.get("totalIndexSize")));
        collStats.setCapped(getBooleanValue(collStatsMap.get("capped")));

        // Parse index sizes
        Map indexesMap = (Map) collStatsMap.get("indexes");
        if (indexesMap != null) {
            Map<String, Long> indexSizes = (Map<String, Long>) indexesMap.get("indexSizes");
            if (indexSizes != null) {
                collStats.setIndexSizes(new LinkedHashMap<>());
                for (Object key : indexSizes.keySet()) {
                    String indexName = (String) key;
                    Long size = getLongValue(indexSizes.get(indexName));
                    collStats.getIndexSizes().put(indexName, size);
                }
            }

            // Process index stats
            List<Map> indexStatsList = (List<Map>) indexesMap.get("stats");
            if (indexStatsList != null) {
                for (Map indexStatsMap : indexStatsList) {
                    processIndexStats(collection, indexStatsMap);
                }
            }
        }

        collection.setCollectionStats(collStats);
        database.addCollection(collection);
    }

    @SuppressWarnings("unchecked")
    private void processIndexStats(Collection collection, Map indexStatsMap) {
        String name = (String) indexStatsMap.get("name");
        Map key = (Map) indexStatsMap.get("key");

        if (key == null) {
            return;
        }

        String sKey = String.join(", ", key.keySet());
        Index ix = new Index(collection.getNamespace(), key, sKey);

        // Get size from collection stats
        if (collection.getCollectionStats() != null &&
            collection.getCollectionStats().getIndexSizes() != null) {
            Long size = collection.getCollectionStats().getIndexSizes().get(name);
            if (size != null) {
                ix.size = size.doubleValue();
            }
        }

        // Get access stats
        Map accessesMap = (Map) indexStatsMap.get("accesses");
        if (accessesMap != null) {
            Object opsObj = accessesMap.get("ops");
            if (opsObj != null) {
                if (opsObj instanceof Map) {
                    Map opsMap = (Map) opsObj;
                    Object numberLong = opsMap.get("$numberLong");
                    if (numberLong != null) {
                        ix.accessOps = Long.parseLong(numberLong.toString());
                    }
                } else {
                    ix.accessOps = getLongValue(opsObj);
                }
            }
        }

        collection.addIndex(ix);
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Double) {
            return ((Double) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    private Integer getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private Double getDoubleValue(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private Boolean getBooleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    public static void main(String[] args) throws IOException, ParseException {
        File f = new File(args[0]);
        GetMongoDataParserJsonV2 parser = new GetMongoDataParserJsonV2(f);
        parser.parse();
    }
}
