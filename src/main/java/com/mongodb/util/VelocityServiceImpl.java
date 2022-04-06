package com.mongodb.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.DisplayTool;
import org.apache.velocity.tools.generic.ListTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;

public class VelocityServiceImpl {

	private VelocityEngine velocityEngine;
	private MessageSource messageSource;
	private DateTool dateTool;
	private NumberTool numberTool;
	private Formatter formatter;
	private DateTimeFormatter dateTimeFormatter;
	private ListTool listTool;
	private MathTool mathTool;
	private DisplayTool displayTool;
	
	private Map<String, Object> tools;
	
	public VelocityServiceImpl() {
		dateTool = new DateTool();
		numberTool = new NumberTool();
		formatter = new Formatter();
		listTool = new ListTool();
		mathTool = new MathTool();
		displayTool = new DisplayTool();
	}

	public String mergeTemplateIntoString(String templateLocation, Map model)
			throws VelocityException {
		StringWriter result = new StringWriter();
		mergeTemplate(velocityEngine, templateLocation, model, result);
		return result.toString();
	}

	private void mergeTemplate(VelocityEngine velocityEngine,
			String templateLocation, Map model, StringWriter writer)
			throws VelocityException {
		try {
			VelocityContext context = new VelocityContext(model);
			context.put("dateTool", dateTool);
			context.put("listTool", listTool);
			context.put("number", numberTool);
			context.put("math", mathTool);
			context.put("display", displayTool);
			context.put("formatter", formatter);
			context.put("messageSource", messageSource);
			context.put("dateTimeFormatter", dateTimeFormatter);
			for (String key : tools.keySet()) {
				context.put(key, tools.get(key));
			}
			velocityEngine.mergeTemplate(templateLocation, context,
					writer);
		} catch (VelocityException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new VelocityException(ex.toString());
		}

	}

	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}
	
	public class Formatter {
		public String format(String format, ArrayList args) {
			return String.format(format, args.toArray());
		}
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
		this.dateTimeFormatter = dateTimeFormatter;
	}

	public void setTools(Map<String, Object> tools) {
		this.tools = tools;
	}	
}
