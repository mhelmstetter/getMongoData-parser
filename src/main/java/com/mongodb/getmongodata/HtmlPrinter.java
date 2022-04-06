package com.mongodb.getmongodata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.mongodb.util.VelocityServiceImpl;

public class HtmlPrinter {
    
    private VelocityServiceImpl velocity;
    Map<String, Object> model = new HashMap<String, Object>();
    private String template;
    
    public HtmlPrinter() {
    }
    
    public void addToModel(String key, Object value) {
        model.put(key, value);
    }
    
    public void clearModel() {
        model.clear();
    }
    
    public void print(File outputFile) throws IOException {
        String result = velocity.mergeTemplateIntoString(template, model);
        //velocity.mergeTemplateIntoString(templateLocation, model);
        
        //FileUtils.forceMkdir(xmlFile.getParentFile());
        System.out.println("Writing to " + outputFile.getPath());
        BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
        out.write(result);
        out.close();
    }
    
    public void setVelocityService(VelocityServiceImpl velocityService) {
        this.velocity = velocityService;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
