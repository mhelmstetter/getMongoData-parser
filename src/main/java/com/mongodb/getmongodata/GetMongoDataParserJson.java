package com.mongodb.getmongodata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GetMongoDataParserJson {
    
    protected final static ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "getmongodata/spring.xml");
    
    private String currentLine = null;
    //private String next = null;
    private File file;
    BufferedReader in;
    private Gson gson;
    
    private Cluster databases = new Cluster();
    private Collection coll;
    
    Pattern collectionListDbNamePattern = Pattern.compile("^.*'(.*)'");
    
    public GetMongoDataParserJson(File f) throws FileNotFoundException {
        this.file = f;
        in = new BufferedReader(new FileReader(file));
        gson = new GsonBuilder().create();
    }

    private void readSection() {
        try {
            if (currentLine.contains("List of databases")) {
                listOfDatabases();
            } else if (currentLine.contains("List of collections")) {
                listOfCollections();
            } else if (currentLine.contains("Database stats")) {
                databaseStats();
            } else if (currentLine.contains("Collection stats")) {
                collectionStats();
            } else if (currentLine.toLowerCase().contains("index stats")) {
                indexStats();
            } else if (currentLine.contains("Indexes")) {
                indexes();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void listOfCollections() throws ParseException, IOException {
        String databaseName = null;
        
        Matcher m = collectionListDbNamePattern.matcher(currentLine);
        if (m.find( )) {
            databaseName = m.group(1);
            //System.out.println("Found value: " + m.group() );
         } else {
            System.out.println("NO MATCH");
         }
        
        System.out.println("*****"+databaseName); 
        Object obj = parseJson();
        List jsonArray = (List) obj;
        
        Database database = databases.getDatabase(databaseName);
        
        Iterator iterator = jsonArray.iterator();
        while (iterator.hasNext()) {
            String collectionName = (String)iterator.next();
            Collection collection = new Collection();
            collection.setName(collectionName);
            database.addCollection(collection);
        }
    }
    
    private void databaseStats() throws ParseException, IOException {
        DatabaseStats dbStats = parseGson(DatabaseStats.class);
        Database db = databases.getDatabase(dbStats.getDb());
        db.setDbStats(dbStats);
    }
    
    private void collectionStats() throws ParseException, IOException {
        CollectionStats collStats = parseGson(CollectionStats.class);
        Database db = databases.getDatabase(collStats.getDatabaseName());
        coll = db.getCollection(collStats.getCollectionName());
        if (coll == null) {
            System.out.println("***** Collection is null for " + collStats.getCollectionName());
        }
        coll.setCollectionStats(collStats);
    }
    
    private void indexes() throws ParseException, IOException {
        List indexesArray = (List)parseJson();
        
        if (indexesArray == null) {
            return;
        }
        for (Iterator<Map> i = indexesArray.iterator(); i.hasNext();) {
            Map indexJson = i.next();
            
            Map key = (Map)indexJson.get("key");
            //System.out.println("+++++" + key);
            Set entries = key.entrySet();
            String sKey = StringUtils.join(key.keySet(), ", ");
            //System.out.println("+++++" + ixName);
            
            String name = (String)indexJson.get("name");
            Index ix = new Index(key, sKey);
            
            coll.addIndex(ix);
            
            String xName = (String)indexJson.get("name");
            Map<String, Long> ixSizes = coll.getCollectionStats().getIndexSizes();
            Long size = ixSizes.get(xName);
            ix.size = size.doubleValue();
        }
    }
    
    private void indexStats() throws ParseException, IOException {
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
            IndexStats.Stats stats = indexStats.stats.get(0);
            ix.accessOps = stats.accesses;
            //coll.addIndex(ix);
        }
    }
    
    private void listOfDatabases() throws ParseException, IOException {
        Object obj = parseJson();
        Map jsonObject = (Map) obj;
        List databasesJson = (List)jsonObject.get("databases");
        listOfDatabases(databasesJson);
    }
    
    private void listOfDatabases(List databasesJson) throws ParseException, IOException {
        Iterator<Map> iterator = databasesJson.iterator();
        while (iterator.hasNext()) {
            Map dbJson = iterator.next();
            //JSONObject dbJson = iterator.next();
            //System.out.println(db.toJSONString());
            Database db = new Database();
            db.setName((String)dbJson.get("name"));
            db.setSizeOnDisk((Long)dbJson.get("sizeOnDisk"));
            databases.addDatabase(db);
        }
        //System.out.println(jsonObject.toJSONString());
    }
    
    private Object parseJson() throws ParseException, IOException {
        JSONParser parser = new JSONParser();
        ContainerFactory orderedKeyFactory = new ContainerFactory()
        {
            public List creatArrayContainer() {
              return new LinkedList();
            }

            public Map createObjectContainer() {
              return new LinkedHashMap();
            }

        };
        
        String json = readJson(in);
        //System.out.println("json: " + json);
        
        Object result = null;
        try {
            result = parser.parse(json, orderedKeyFactory);
        } catch (ParseException pe) {
            parser.getPosition();
        }
        return result;
        
    }
    
    public <T> T parseGson(Class<T> classOfT) throws IOException {
        String json = readJson(in);
        //System.out.println("gson " + json);
        return gson.fromJson(json, classOfT);
        
    }
    
    private String readJson(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        while ((currentLine = in.readLine()) != null) {
            
            if (currentLine.contains("NumberLong(")) {
                currentLine = currentLine.replaceAll("NumberLong\\(.*?([0-9]+).*?\\)", "$1");
            }
            if (currentLine.contains("ISODate(")) {
                currentLine = currentLine.replaceAll("ISODate\\((\".*\")\\)", "$1");
            }
            
            if (currentLine.length() == 0) {
                continue;
            }
            //System.out.println("-- " + currentLine);
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
            
        }
        return sb.toString();
    }
    

    
    @SuppressWarnings("unused")
    private void readFile() throws IOException, ParseException {
        
        List<Map> result = (List<Map>)parseJson();
        
        for (Map jsonMap : result) {
            String section = (String)jsonMap.get("section");
            
            Object output = jsonMap.get("output");
            if (output instanceof LinkedHashMap) {
                LinkedHashMap e = (LinkedHashMap)output;
                Map.Entry y = (Map.Entry)e.entrySet().iterator().next();
                if (y.getKey().equals("databases")) {
                    List dbList = (List)y.getValue();
                    listOfDatabases(dbList);
                } else if (y.getKey().equals("db")) {
                    Object z = y.getValue();
                    System.out.println(z);
                } else {
                    //System.out.println("** " + y.getKey());
                }
                //System.out.println(section);
            }
            
        }
        
        
//        while ((currentLine = in.readLine()) != null) {
//            
//
//            if (currentLine.length() == 0) {
//                continue;
//            }
//            if (currentLine.startsWith("**")) {
//                System.out.println(currentLine);
//                readSection();
//            }
//            
//        }
//        in.close();
        
        //SimpleTextPrinter printer = new SimpleTextPrinter(databases);
        //printer.print();
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
    }

    public static void main(String[] args) throws IOException, ParseException {
        File f = new File(args[0]);
        GetMongoDataParserJson parser = new GetMongoDataParserJson(f);
        parser.readFile();
        
    }

}
