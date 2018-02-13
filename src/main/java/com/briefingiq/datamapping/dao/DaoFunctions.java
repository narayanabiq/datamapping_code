package com.briefingiq.datamapping.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Pattern;

import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.session.SessionProperties.Jdbc;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.briefingiq.datamapping.Constants;

import oracle.net.aso.n;


@Component
public class DaoFunctions {

	
	
	public String[] colorCodes = {"#2741c4", "#c452a6", "#c4b93d", "#ba68c8", "#020302", "#75c447", "#c97f5c", "#c4c2c2", "#7d4ac9", "#c46ec4", "#a2c45a", "#4730c9", "#c43e2f", "#4ec9a6", "#c43421", "#2f39c4"};
	
	@Autowired
	DBConnectionUtil dbConnectionUtil;
	
	@Autowired
	QueryUtil queryUtil;
	
	public SimpleDateFormat format = new SimpleDateFormat("dd-MM-YYYY");
	
	public String[] getNameDetail(String email) {
		String name = email.substring(0, email.indexOf("@"));
		return email.substring(0, email.indexOf("@")).split(Pattern.quote("."));
	}
	
	
	public static void main(String args[]) {
		DaoFunctions functions = new DaoFunctions();
		String[] names = functions.getNameDetail("nara.yana.appana@gmail.com");
		System.out.println("names "+names[0]+"  "+names[1]);
	}
	
	public Object createUser(Object userName,Integer role,Boolean isLocationUser,Long locationId) throws  Exception {
		
		Map<String,Object> user =new HashMap<>();
		Long userId = 0L;
		
		Map<String,Object> userWhereClause = new HashMap<String,Object>();
		userWhereClause.put("USER_NAME", userName);
		
		List<Map<String,Object>> users = queryUtil.getRecords(Constants.DEST_CONNECTION_NAME, "BI_USER", userWhereClause); 
		
		if(users.isEmpty()) {
			userWhereClause.put("ID","BI_USER_SEQ.nextval");
			userWhereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
			userWhereClause.put("CREATED_BY", "CVC");
			userWhereClause.put("CREATED_TS", new java.sql.Timestamp(System.currentTimeMillis()));
			userWhereClause.put("VERSION", 0);
			userWhereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
			userWhereClause.put("COLOR_CODE", colorCodes[new Random().nextInt(15)]);
			
			String[] names = this.getNameDetail(userName.toString());
			if(names.length>0) {
				userWhereClause.put("FIRST_NAME",WordUtils.capitalizeFully(names[0]));
				if(names.length==2) {
					userWhereClause.put("LAST_NAME", WordUtils.capitalizeFully(names[1]));
					userWhereClause.put("MIDDLE_NAME",null);
				}
				if(names.length==3) {
					userWhereClause.put("LAST_NAME", WordUtils.capitalizeFully(names[2]));
					userWhereClause.put("MIDDLE_NAME",WordUtils.capitalizeFully(names[1]));
				}
			}
			userId = queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, "BI_USER", userWhereClause);
		}else {
			userId =Long.parseLong(users.get(0).get("ID").toString());
		}
		
		Map<String,Object> userContactWhereClause = new HashMap<String,Object>();
		userContactWhereClause.put("VALUE", userName);
		userContactWhereClause.put("USER_ID", userId);
		userContactWhereClause.put("CONTACT_TYPE", "email");
		
		queryUtil.checkAndInsert("BI_USER_CONTACT", userContactWhereClause);
		
		Map<String,Object> userRoleWhereClause = new HashMap<String,Object>();
		userRoleWhereClause.put("USER_ID", userId);
		
	     List<Map<String,Object>> roles =	queryUtil.getRecords(Constants.DEST_CONNECTION_NAME,"BI_USER_ROLE", userRoleWhereClause);
	     if(roles.isEmpty()) {
	    	 userRoleWhereClause.put("ROLE_ID", role);
	    	 queryUtil.insertRecordWOPK(Constants.DEST_CONNECTION_NAME, "BI_USER_ROLE", userRoleWhereClause);
	     }else {
	    	 	if(roles.get(0).get("ROLE_ID").toString().equalsIgnoreCase("4")){
	    		 dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update("UPDATE BI_USER_ROLE SET ROLE_ID = "+role+ " WHERE USER_ID = "+userId);
	    	 }
	     }
	
		return userId;
	}
	
	
	public void insertLocationUser(Long userId,Long locationId,Integer roleId) throws Exception {
		Map<String,Object> whereClause = new HashMap<String,Object>();
		whereClause.put("USER_ID", userId);
		whereClause.put("ROLE_ID", roleId);
		whereClause.put("LOCATION_ID", locationId);
		List locationUser =	queryUtil.getRecords(Constants.DEST_CONNECTION_NAME, "BI_LOCATION_USER", whereClause);
		if(locationUser.isEmpty()) {
			whereClause.put("ID","BI_LOCATION_USER_SEQ.nextval");
			whereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
			whereClause.put("CREATED_BY", "CVC");
			whereClause.put("CREATED_TS", new java.sql.Timestamp(System.currentTimeMillis()));
			whereClause.put("VERSION", 0);
			whereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
			whereClause.put("IS_ACTIVE", 1);
			queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, "BI_LOCATION_USER", whereClause);
		}
	}
	
	public Object processCateringType(Object cateringType) throws Exception {
		Map<String,Object> whereClause = new HashMap<String,Object>();
		whereClause.put("CODE","CATERING_TYPE");
		
		List<Map<String,Object>> lookUpType = queryUtil.getRecords(Constants.DEST_CONNECTION_NAME, "BI_LOOKUP_TYPE", whereClause);
		
		Long lookUpTypeId =Long.parseLong(lookUpType.get(0).get("ID").toString());
		
		Map<String,Object> lookUpwhereClause = new HashMap<String,Object>();
		lookUpwhereClause.put("CODE",cateringType);
		lookUpwhereClause.put("LOOKUP_TYPE_ID",lookUpTypeId);
		
		List<Map<String,Object>> lookupValues = queryUtil.getRecords(Constants.DEST_CONNECTION_NAME, "BI_LOOKUP_VALUE", lookUpwhereClause);
		
		if(lookupValues.isEmpty()) {
			Map<String,Object> cvcLookUpwhereClause = new HashMap<String,Object>();
			cvcLookUpwhereClause.put("LOV_NAME", "CATERING_TYPE");
			cvcLookUpwhereClause.put("CODE", cateringType);
			List<Map<String,Object>> cvcLookupValues = queryUtil.getRecords(Constants.SRC_CONNECTION_NAME, "VW_CVC_LOOKUP_VALUES", cvcLookUpwhereClause);
			
			if(!cvcLookupValues.isEmpty()) {
				lookUpwhereClause.put("VALUE", cvcLookupValues.get(0).get("VALUE1"));
				lookUpwhereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
				lookUpwhereClause.put("CREATED_BY", "CVC");
				lookUpwhereClause.put("CREATED_TS", new java.sql.Timestamp(System.currentTimeMillis()));
				lookUpwhereClause.put("VERSION", 0);
				lookUpwhereClause.put("UNIQUE_ID", UUID.randomUUID().toString());
				lookUpwhereClause.put("IS_ACTIVE", 1);
				whereClause.put("ID","BI_LOOKUP_VALUE_SEQ.nextval");
				 queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, "BI_LOOKUP_VALUE", lookUpwhereClause);
				 return cvcLookupValues.get(0).get("VALUE1");
			}
			
		}else {
			return lookupValues.get(0).get("VALUE");			
		}
		
		
		return null;
	}
	
	public Object processBM(Object userName) throws Exception {
		return this.createUser(userName, 2,false,null);		
	}
	
	public Object processRequester(Object userName) throws Exception {
		return this.createUser(userName, 4,false,null);		
	}
	
	
	public void correctActvityDays(Long requestId) throws Exception {
		Map<String, Object> clause= new HashMap<>();
		clause.put("REQUEST_ID", requestId);
		Set<java.sql.Timestamp> daysSet = new HashSet<java.sql.Timestamp>();
		List<Map<String,Object>> days = queryUtil.getRecords(Constants.DEST_CONNECTION_NAME, "BI_REQUEST_ACTIVITY_DAY", clause);
		for(Map<String,Object> day : days) {
				if(daysSet.contains((java.sql.Timestamp)day.get("EVENT_DATE"))) {
					dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update("DELETE BI_REQUEST_ACTIVITY_DAY WHERE ID = "+day.get("ID"));
				}else {
					daysSet.add((java.sql.Timestamp)day.get("EVENT_DATE"));
				}
					
		}
	}
	
	
	public void updateCostCenter(Long requestId,Long sourceRequestId) {
		List<Map<String, Object>> costCenter =  dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME).query("SELECT DISTINCT REPLACE(REPLACE(cost_center,CHR(9),''),' ','') CC FROM VW_CVC_AGENDA_CATERING WHERE REQUEST_ID = "+sourceRequestId, new ColumnMapRowMapper());
		if(!costCenter.isEmpty()) {
			dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update("UPDATE BI_REQUEST SET COST_CENTER = '"+costCenter.get(0).get("CC").toString()+"', SCHEDULAR =  (SELECT ID FROM BI_USER WHERE USER_NAME = 'cvclimo_us@briefingiq.com' )  WHERE ID = "+requestId);
		}
	}
	
	@Autowired
	DataMapperUtil dataMapperUtil;
	
	public void populateMainRoom(Long requestId,Long sourceRequestId) throws Exception {
		Map<String,Object> activitiesClause = new HashMap<>();
		activitiesClause.put("REQUEST_ID", requestId);
		List<Map<String,Object>> activitydays =queryUtil.getRecords(Constants.DEST_CONNECTION_NAME,"BI_REQUEST_ACTIVITY_DAY", activitiesClause);
		
		for(Map<String, Object> activityDay : activitydays) {
			String mainRoomQuery = "SELECT * FROM CX_CVC.CVC_AGENDA WHERE ROOM_MAIN = 'Y' AND REQUEST_ID = "+sourceRequestId+" AND ENTRY_TYPE IN ('START_MARK','END_MARK') AND ENTRY_DATE = TO_DATE ( '"+format.format(new Date(((java.sql.Timestamp)activityDay.get("EVENT_DATE")).getTime()))+"' ,'DD-MM-YYYY')" ;
			System.out.println(" main room query "+mainRoomQuery);
			List<Map<String,Object>> mainRooms = dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME).queryForList(mainRoomQuery);
			if(!mainRooms.isEmpty()) {
				Map<String,Object> columns = new HashMap<>();
				columns.put("PROCESS_ID",104);
				columns.put("SRC_FK_COLUMN", "ID");
				columns.put("DEST_FK_TABLE", "BI_LOCATION");
				columns.put("DEST_FK_COLUMN", "ID");
				columns.put("SRC_FK_TABLE", "VW_CVC_LOCATION_ROOM");
				
				Object value =		dataMapperUtil.getDestFKValue(columns, mainRooms.get(0).get("ROOM_ID"), "ID", sourceRequestId);
				
				String assignMainRoom = "UPDATE BI_REQUEST_ACTIVITY_DAY SET MAIN_ROOM = "+value+ ", UPDATED_BY = '"+mainRooms.get(0).get("UPDATED_BY")+"',"
						+ "   CREATED_BY  = '"+mainRooms.get(0).get("UPDATED_BY")+"' WHERE ID ="+activityDay.get("ID");
				dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(assignMainRoom);
				
			}
		}
		
	}

	
	public void populateBreakoutRooms(Long requestId,Long sourceRequestId) throws Exception {
		Map<String,Object> activitiesClause = new HashMap<>();
		activitiesClause.put("REQUEST_ID", requestId);
		List<Map<String,Object>> activitydays =queryUtil.getRecords(Constants.DEST_CONNECTION_NAME,"BI_REQUEST_ACTIVITY_DAY", activitiesClause);
		
		for(Map<String, Object> activityDay : activitydays) {
			String breakRoomQuery = "SELECT distinct(ROOM_ID) AS ROOM,ENTRY_DATE FROM CX_CVC.CVC_AGENDA WHERE ROOM_MAIN IS NULL AND REQUEST_ID = "+sourceRequestId+" AND ENTRY_DATE = TO_DATE ( '"+format.format(new Date(((java.sql.Timestamp)activityDay.get("EVENT_DATE")).getTime()))+"' ,'DD-MM-YYYY')  GROUP BY ENTRY_DATE , ROOM_ID" ;
			List<Map<String,Object>> breakRooms = dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME).queryForList(breakRoomQuery);
			for(Map<String,Object> breakRoom : breakRooms) {
				Map<String,Object> columns = new HashMap<>();
				columns.put("PROCESS_ID",104);
				columns.put("SRC_FK_COLUMN", "ID");
				columns.put("DEST_FK_TABLE", "BI_LOCATION");
				columns.put("DEST_FK_COLUMN", "ID");
				columns.put("SRC_FK_TABLE", "VW_CVC_LOCATION_ROOM");
				Object value =		dataMapperUtil.getDestFKValue(columns, breakRoom.get("ROOM"), "ID", sourceRequestId);
				if(value!=null) {
						if(activityDay.get("MAIN_ROOM")!=null) {
							Long mainRoom = Long.parseLong(activityDay.get("MAIN_ROOM").toString());
							Long breakRoomId = Long.parseLong(value.toString());
							if(mainRoom!=breakRoomId) {
								String breakOutRoomQuery = "INSERT INTO BI_REQUEST_ACT_DAY_BREAK_ROOM (REQUEST_ACTIVITY_DAY_ID, BREAK_ROOMS) VALUES (?, ?)";
								dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(breakOutRoomQuery, new Object[] {activityDay.get("ID"),breakRoomId});
							}
						}
				}
			}
			
		}

	}
	
	
	public Object executeFunction(String functionName,Object param) throws Exception {
	
		switch (functionName) {
		case "processBM":
			   return this.processBM(param);
		case "processRequester":
			return this.processRequester(param);
		case "processCateringType":
			return this.processCateringType(param);
		default:
			break;
		}
		
		
		return null;
	}
}