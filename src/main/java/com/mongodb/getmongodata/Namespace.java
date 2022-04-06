package com.mongodb.getmongodata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Namespace {

    private String databaseName;
    private String collectionName;

    private final static Pattern namespacePattern = Pattern.compile("^(.*?)\\.(.*)$");

    public Namespace(String ns) {
        Matcher m = namespacePattern.matcher(ns);
        if (m.find()) {
            databaseName = m.group(1);
            collectionName = m.group(2);
        }
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String toString() {
        return databaseName + "." + collectionName;
    }

}
