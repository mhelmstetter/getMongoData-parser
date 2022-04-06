package com.mongodb.getmongodata;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class IndexStats {
    
    public static class Stats {
        public Long accesses;
        public String host;
        public String since;
    }
    
    public Map<String, String> key;
    public List<Stats> stats;
    
//    "waitedMS" : NumberLong(0),
//    "result" : [
//            {
//                    "stats" : [
//                            {
//                                    "accesses" : NumberLong(0),
//                                    "host" : "mngdb-ebf-prd1-001:27008",
//                                    "since" : "Tue, 17 Oct 2017 21:59:19 GMT"
//                            }
//                    ],
//                    "key" : {
//                            "role" : 1,
//                            "db" : 1
//                    }
//            },
//            {
//                    "stats" : [
//                            {
//                                    "accesses" : NumberLong(0),
//                                    "host" : "mngdb-ebf-prd1-001:27008",
//                                    "since" : "Tue, 17 Oct 2017 21:59:19 GMT"
//                            }
//                    ],
//                    "key" : {
//                            "_id" : 1
//                    }
//            }
//    ],
//    "ok" : 1
    
   

}
