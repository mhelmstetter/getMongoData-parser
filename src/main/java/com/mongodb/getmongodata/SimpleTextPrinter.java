package com.mongodb.getmongodata;

import java.io.OutputStream;

public class SimpleTextPrinter {
    
    private Cluster cluster;
    private OutputStream os;
    
    public SimpleTextPrinter(Cluster cluster) {
        this.cluster = cluster;
        this.os = System.out;
    }
    
    public void print() {
        for (Database d : cluster.getDatabases()) {
            System.out.println(d.getName());
            for (Collection c : d.getCollections()) {
                System.out.println("    " + c.getName());
            }
        }
    }

}
