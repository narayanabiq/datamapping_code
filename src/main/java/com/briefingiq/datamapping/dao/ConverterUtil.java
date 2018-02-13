package com.briefingiq.datamapping.dao;

import java.sql.Date;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ConverterUtil {
	
	final static Logger logger = Logger.getLogger(ConverterUtil.class);

	public static Date toDate(Object object) {

		Date returnDate = null;
		if(object==null) return null;
				logger.debug(" data type "+ object.getClass().getName());
		switch (object.getClass().getName()) {
		case "java.sql.Date":
			returnDate =  (Date) object;
			break;

		case "java.util.Date":
			returnDate = new Date(((java.util.Date)object).getTime());
			break;
		case "java.sql.Timestamp":
			returnDate = new Date(((java.sql.Timestamp)object).getTime());
			break;
		default:
			returnDate = null;
			break;
		}

		return returnDate;
	}
	
	public static Timestamp toTimestamp(Object object) {
		
		Timestamp value =null;
		if(object==null) return null;
		
		switch (object.getClass().getName()) {
		case "java.sql.Timestamp":
			value = (java.sql.Timestamp)object;
			break;
		default:
			value = null;
			break;
		}
		
		
		
		return value;		
	}
	
		
	public static Long toNumber(Object object) {
		Long value = null;
		if(object==null) return null;
		try{
		switch (object.getClass().getName()) {
		case "java.lang.String":
				value = Long.parseLong(object.toString());
			break;
		case "java.math.BigDecimal":
				value = ((java.math.BigDecimal)object).longValue();
			break;
		default:
			break;
				
		}
		}catch (Exception e) {
			value = null;
		}
		return value;
	}

	public static Object toBoolean(Object object) {
		Integer value = null;
		if(object==null) return null;
		switch (object.getClass().getName()) {
		
			case "java.lang.String":
					if(object.toString().equalsIgnoreCase("Y") || object.toString().equalsIgnoreCase("YES"))
							value =1;
					else
						value = 0;
						
						
				break;
			default:
				value = 0;
				break;
		}
			
		return value;
	}
	
	public static String  getTimestampFormat(java.util.Date timestamp,String format) throws ParseException{
		return  new SimpleDateFormat(format).format(timestamp);
	}
	
	public static void main(String[] args) {
		Timestamp ts=new Timestamp(System.currentTimeMillis());
		
		System.out.println(ts);
		
	}

}