package com.briefingiq.datamapping.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.briefingiq.datamapping.Constants;
import com.briefingiq.datamapping.model.CVCRequest;
import com.briefingiq.datamapping.model.ProcessLog;

@Component
public class QueryUtil {
	
	
	final static Logger logger = Logger.getLogger(DBConnectionUtil.class);

	
	/*@Autowired
	@Qualifier( "srcTemplate")
	private JdbcTemplate jdbcTemplate;*/
	
	@Autowired
	DBConnectionUtil dbConnectionUtil;
	
	@Autowired
	DataMapperLogger dblogger;
	
	
	@Autowired
	DaoFunctions daoFunctions;
	
	public SimpleDateFormat format = new SimpleDateFormat("dd-MM-YYYY");
	
	public List getRequestData(Long requestId,Long fromDate,Long toDate,Boolean processed,List<String> status, String briefingManager ,Long start,Long end) {
		
	//	JdbcTemplate jdbcTemplate = dbConnectionUtil.getTemplate("DESTNATION");
		
		String query = "select * from (  select  row_number() over (order by event_start_date desc) rn, req.request_id requestId,req.bi_request_id biRequestId,req.company_name companyName,req.event_start_date eventStartDate,req.host_name hostName,req.country country,"
				+ "req.location location,req.status status,req.start_date startDate,req.end_date endDate, cvc_status as reqStatus , ac_id as briefingManager"
				+ " from cvc_request_process req  where 1=1  ";
		
		if(requestId!=null)
			query = query + " and req.request_id = "+requestId;
		
		if(processed) {
			query = query + " and req.bi_request_id is not null";
		}
		
		if(fromDate!=null) {
			query = query + " and event_start_date >=  TO_DATE ( '"+format.format(new Date(fromDate))+"' ,'DD-MM-YYYY' ) " ;
		}
		
		if(toDate!=null){
			query = query + " and event_start_date <=  TO_DATE ( '"+format.format(new Date(toDate))+"' ,'DD-MM-YYYY' ) " ;
		}
		
		if(status!=null && !status.isEmpty()) {
			List<String> states = new ArrayList<>();
			states.addAll(status);
			states.replaceAll(state -> " '"+ state.toUpperCase() + "' ");
			query = query + "  and upper(cvc_status)  in ("+ String.join(",", states) +" )";
		}
		if(briefingManager!=null && !briefingManager.isEmpty()) {
			query = query + " AND upper(ac_id) like '%"+briefingManager.toUpperCase()+"%'";
		}
			
		query = query + " order by req.event_start_date desc ) where rn > "+start + " and rn <= "+end;
				
	List<CVCRequest> data =	 dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).query(query,new BeanPropertyRowMapper(CVCRequest.class));
		
		return data;
		
	}
	
	public int getRequestDataCount(Long requestId,Long fromDate,Long toDate,Boolean processed,List<String> status, String briefingManager  ,Long start,Long end) {
		
		//JdbcTemplate jdbcTemplate = dbConnectionUtil.getTemplate("DESTNATION");
		
		String query = " select  count(*) as total from cvc_request_process req  where 1=1  ";
		
		if(requestId!=null)
			query = query + " and req.request_id = "+requestId;
		
		if(processed) {
			query = query + " and req.bi_request_id is not null";
		}
		
		if(fromDate!=null) {
			query = query + " and event_start_date >=  TO_DATE ( '"+format.format(new Date(fromDate))+"' ,'DD-MM-YYYY' ) " ;
		}
		
		if(toDate!=null){
			query = query + " and event_start_date <=  TO_DATE ( '"+format.format(new Date(toDate))+"' ,'DD-MM-YYYY' ) " ;
		}
		if(status!=null && !status.isEmpty()) {
			List<String> states = new ArrayList<>();
			states.addAll(status);
			states.replaceAll(state -> " '"+ state.toUpperCase() + "' ");
			query = query + "  and upper(cvc_status)  in ("+ String.join(",", states) +" )";
		}
		if(briefingManager!=null && !briefingManager.isEmpty()) {
			query = query + " AND upper(ac_id) like '%"+briefingManager.toUpperCase()+"%'";
		} 
		
		query = query + " order by req.event_start_date desc ";
		
	List<Map<String, Object>> data = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).queryForList(query);
		
		return Integer.parseInt(data.get(0).get("TOTAL").toString());
		
}

	
	public void storedProcedure(Long requestId,Long fromDate,Long toDate,String status,String email,String truncate) throws ParseException, SQLException{
		
		final String procedureCall = "{call cvc_bi_conv_master.main(?,?,?,?,?,?,?,?)}";
		Connection connection = null;
		CallableStatement callableSt = null;
		try {

			//Get Connection instance from dataSource
			connection = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).getDataSource().getConnection();
			callableSt = connection.prepareCall(procedureCall);
			callableSt.registerOutParameter(1, Types.VARCHAR);
			callableSt.registerOutParameter(2, Types.VARCHAR);
			if(truncate!=null){
				callableSt.setString(3, truncate);
			}else{
				callableSt.setString(3, null);
			}
			if(requestId!=null) {
				callableSt.setString(4, requestId.toString());
			}else{
				callableSt.setString(4, null);
			}
			if(fromDate!=null) {
				String from=ConverterUtil.getTimestampFormat(new Date(fromDate), "dd-MMM-yy");
				callableSt.setString(5, from);
			}else{
				callableSt.setString(5, null);
			}
			if(toDate!=null) {
				String to=ConverterUtil.getTimestampFormat(new Date(toDate), "dd-MMM-yy");
				callableSt.setString(6, to);
			}else{
				callableSt.setString(6, null);
			}
			if(status!=null){
				callableSt.setString(7, status);
			}else{
				callableSt.setString(7, null);
			}
			if(status!=null){
				callableSt.setString(8, email);
			}else{
				callableSt.setString(8, null);
			}

			//Call Stored Procedure
			callableSt.executeUpdate();
			System.out.println(callableSt.getString(1));

			}catch (SQLException e) {

			e.printStackTrace();

			} finally {
				if(!callableSt.isClosed())
					try {
						callableSt.close();
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

			if(connection != null)
			try {
			connection.close();
			} catch (SQLException e) {
			e.printStackTrace();
			}
			}
		
		
		
	}
	

	public void deleteRequests(List<String> requests,boolean cleanLogData) throws Exception{
		
		requests.removeAll(Collections.singletonList(""));
		String requestIds = StringUtils.join(requests,"," );
		
		String query1 =  "delete from bi_request_attendees where request_id in ( "+requestIds +" )";
		String query2 =  "delete from bi_location_calendar where request_id in ( "+requestIds +" )";
		String query3 =  "delete from bi_request_activity where request_activity_day_id in (select id  from bi_request_activity_day where request_id in ( "+requestIds+" ))";
		String query4 =  "delete from bi_request_catering_activity where request_activity_day_id in (select id  from bi_request_activity_day where request_id in ( "+ requestIds +" ))";
		String query5 =  "delete from bi_request_presenter where BI_REQUEST_TOPIC_ACTIVITY_ID in (select id from bi_request_topic_activity where request_activity_day_id in (select id  from bi_request_activity_day where request_id in ( "+requestIds+" )))";
		String query6 =  "delete from bi_request_topic_activity where request_activity_day_id in (select id  from bi_request_activity_day where request_id in ( "+requestIds+" ))";
		String query7 =  "delete from bi_request_activity_day where request_id in ( "+requestIds+" )";
		String query8 =  "delete from bi_request_asset where request_id in ( "+requestIds+" )";
		String query9 =  "delete from bi_request_documents where request_id in ( "+requestIds+" )";
		String query10 = "delete from bi_request where id in ( "+requestIds+" )";
    	String query15=  "delete from bi_cvc_agenda where cvc_request_id in (select REQUEST_ID from cvc_request_process where bi_request_id in ( "+requestIds+" ))";
    	String query16=  "delete from bi_cvc_agenda_presenter where id in (select REQUEST_ID from cvc_request_process where bi_request_id in ( "+requestIds+" ))";
    	
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue("ids", requests);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query1);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query2);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query3);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query4);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query5);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query6);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query7);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query8);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query9);
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query10);		
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query15);		
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query16);	
		
		if(cleanLogData) {
			this.clearProcessData(requests);
		}
	}
	
	public void clearProcessData(List<String> requests) {
		requests.removeAll(Collections.singletonList(""));
		String requestIds = StringUtils.join(requests,"," );
		
		String query11 = "delete from T_DATA_MAPPER_LOG where REQUEST_ID in (select REQUEST_ID from cvc_request_process where bi_request_id in ( "+requestIds+" ))";
		String query12=  "UPDATE CVC_REQUEST_PROCESS SET status = 'INIT' , start_date = NULL , end_date  = NULL, bi_request_id=NULL where  bi_request_id in ( "+requestIds+") ";			
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query11);		
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(query12);		

	}
	
	public void loadData() throws Exception {
		String query="INSERT INTO cvc_request_process (request_id,    company_name,    event_start_date,    host_name,    country,    location, status, cvc_status, ac_id ) "+
					" SELECT    REQUESTID, COMPANYNAME, EVENTSTARTDATE, HOSTNAME, COUNTRY, LOCATION, STATUS, DESCRIPTION, AC_ID "
					+ "  FROM VW_CVC_REQUEST_PROCESS WHERE  REQUESTID NOT IN (SELECT REQUEST_ID FROM CVC_REQUEST_PROCESS) ORDER BY REQUESTID  ";	
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).execute(query);		
	}
	
	
	public List getProcessLog(Long requestId) {
		// JdbcTemplate jdbcTemplate = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME);
		List<ProcessLog> logData =		 dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).query("select message AS message, status from T_DATA_MAPPER_LOG WHERE REQUEST_ID ="+requestId+" ORDER BY DATE_TIME ASC",new BeanPropertyRowMapper(ProcessLog.class));
		return logData;
	}

	
	public Long  insertRecord(String connectionName, String tableName, Map<String, Object>  columnData) throws Exception {
		try {
				final TreeMap<String,Object> columnsData = new TreeMap<String,Object>(columnData);
				
				columnsData.values().removeAll(Collections.singletonList(null));
				
				Object idValue = columnData.get("ID");
				
				columnsData.remove("ID");
				
				KeyHolder holder = new GeneratedKeyHolder();
				
				List<String> valueList = new ArrayList<String>(Collections.nCopies(columnsData.size(), "?"));
				String insertQuery = "insert into " + tableName +" ( ID , " + StringUtils.join(columnsData.keySet(),",") +" ) values ("+ idValue +" ,"+StringUtils.join(valueList,",") + ") ";
				
				logger.debug(" Insert query "+insertQuery);
				//JdbcTemplate dbTemplate = dbConnectionUtil.getTemplate(connectionName);  
				
				dbConnectionUtil.getTemplate(connectionName).update(new PreparedStatementCreator() {           
		
		            @Override
		            public PreparedStatement createPreparedStatement(Connection connection)
		                    throws SQLException {
		                PreparedStatement statement = connection.prepareStatement(insertQuery,
		                		new String[] {"id"}); 
		        		int i=1;
		        		for(Map.Entry<String,Object> entry : columnsData.entrySet()) {
		        			logger.debug(entry.getKey()+ " -- "+entry.getValue().getClass()+" -- "+entry.getValue());
		        			if(entry.getValue().getClass().getName().contains("Date"))
		        					statement.setDate(i,QueryUtil.getUTCTime(((java.sql.Date) entry.getValue())));
		        			else if(entry.getValue().getClass().getName().contains("Timestamp"))
		        				statement.setTimestamp(i, QueryUtil.getUTCTime((java.sql.Timestamp) entry.getValue()));
//		        			else if(entry.getValue().toString().endsWith(".nextval"))
//		        				statement.setLong(i, QueryUtil.getSequenceValue(connection,entry.getValue().toString()));
		        			else 
		        				statement.setObject(i, entry.getValue());
		        			
		        			i++;
		        		}
		                return statement;
		            }
		        }, holder);
		return holder.getKey().longValue();
		}catch (Exception e) {
			
			logger.debug("Exception while inserting record in "+tableName+" with error "+e.getMessage());			
			dblogger.log("Exception while inserting record in "+tableName+" with error "+e.getMessage(), false,null);	
			throw e;
		}
	}
	
	public void  insertRecordWOPK(String connectionName, String tableName, Map<String, Object>  columnData) throws Exception {
		try {
				final TreeMap<String,Object> columnsData = new TreeMap<String,Object>(columnData);
				
				columnsData.values().removeAll(Collections.singletonList(null));
				
				
				
				KeyHolder holder = new GeneratedKeyHolder();
				
				List<String> valueList = new ArrayList<String>(Collections.nCopies(columnsData.size(), "?"));
				String insertQuery = "insert into " + tableName +" ( " + StringUtils.join(columnsData.keySet(),",") +" ) values ( "+StringUtils.join(valueList,",") + ") ";
				
				logger.debug(" Insert query "+insertQuery);
			//	JdbcTemplate dbTemplate = dbConnectionUtil.getTemplate(connectionName);  
				
				 dbConnectionUtil.getTemplate(connectionName).update(new PreparedStatementCreator() {           
		
		            @Override
		            public PreparedStatement createPreparedStatement(Connection connection)
		                    throws SQLException {
		                PreparedStatement statement = connection.prepareStatement(insertQuery); 
		        		int i=1;
		        		for(Map.Entry<String,Object> entry : columnsData.entrySet()) {
		        			logger.debug(entry.getKey()+ " -- "+entry.getValue().getClass()+" -- "+entry.getValue());
		        			if(entry.getValue().getClass().getName().contains("Date"))
		        					statement.setDate(i,QueryUtil.getUTCTime(((java.sql.Date) entry.getValue())));
		        			else if(entry.getValue().getClass().getName().contains("Timestamp"))
		        				statement.setTimestamp(i, QueryUtil.getUTCTime((java.sql.Timestamp) entry.getValue()));
		        	//		else if(entry.getValue().toString().endsWith(".nextval"))
		        	//			statement.setLong(i, QueryUtil.getSequenceValue(connectionName,entry.getValue().toString()));
		        			else 
		        				statement.setObject(i, entry.getValue());
		        			
		        			i++;
		        		}
		                return statement;
		            }
		        });
		}catch (Exception e) {
			
			logger.debug("Exception while inserting record in "+tableName+" with error "+e.getMessage());			
			dblogger.log("Exception while inserting record in "+tableName+" with error "+e.getMessage(), false,null);	
			throw e;
		}
	}
	
/*	public List<Map<String,Object>> executeQuery(String connectionName, String query){
		
		
	}*/
	
	public static Long getSequenceValue(String connectionName,String sequenceValue) throws SQLException  {
		
			
		
		
		return null;
	}
	
	
	/*public Long getSequenceValue(String sequenceValue,Connection conn) throws Exception {
		
		String sqlIdentifier = "select "+ sequenceValue +" from dual";
		PreparedStatement pst = conn.prepareStatement(sqlIdentifier);
		   ResultSet rs = pst.executeQuery();
		   if(rs.next())
			   return rs.getLong(1);
		   return null;
	}
	
	public static Long getSequenceValue(Connection conn,String sequenceValue) throws SQLException  {
		
		String sqlIdentifier = "select "+ sequenceValue +" from dual";
		PreparedStatement pst = conn.prepareStatement(sqlIdentifier);
		   ResultSet rs = pst.executeQuery();
		   if(rs.next())
			   return rs.getLong(1);
		   return null;
	}*/
	
	
	public List<Map<String, Object>> getRecords(String connectionName, String tableName, Map<String, Object>  columnData) throws Exception {
		List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
		try {

			columnData.values().removeAll(Collections.singletonList(null));
			
			String selectQuery = "select * from " + tableName +" where 1=1 ";
			List<String> columnNames=new ArrayList<>(columnData.keySet());
			columnNames.replaceAll(columnName -> " AND UPPER ("+ columnName + ") = UPPER(?) ");
			
			Object[] args = columnData.values().toArray();
			
		//	JdbcTemplate dbTemplate = dbConnectionUtil.getTemplate(connectionName);  
			
			logger.debug(" Select query "+selectQuery + StringUtils.join(columnNames," "));
			
			rows = dbConnectionUtil.getTemplate(connectionName).queryForList(selectQuery + StringUtils.join(columnNames," "), args);
		}catch (Exception e) {
			dblogger.log("Exception while retrieve from "+tableName+" with error "+e.getMessage(), false,null);
		}
		
			
	return rows;
	}
	
	public List<Map<String, Object>> getRecordsByQuery(String connectionName, String selectQuery, Map<String, Object>  columnData) throws Exception {
		
		List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
		try {
				columnData.values().removeAll(Collections.singletonList(null));
				
				Object[] args = columnData.values().toArray();
				
				//JdbcTemplate dbTemplate = dbConnectionUtil.getTemplate(connectionName);  
				
				logger.debug(" Select query "+selectQuery);
				
				rows =  dbConnectionUtil.getTemplate(connectionName).queryForList(selectQuery, args);
		}catch (Exception e) {
			dblogger.log("Exception while retrieve data using "+selectQuery, false,null);
			throw e;
		}	
		return rows;
	}
	
	
	
	public List<Map<String,Object>> find(Connection connection, String sql, Map<String, Object> whereClause) throws Exception {
		List<Map<String,Object>> rows = new ArrayList<Map<String,Object>>();
		PreparedStatement statement = connection.prepareStatement(sql);
		int i=1;
		for(Map.Entry<String,Object> entry : whereClause.entrySet()) {
			statement.setObject(i, entry.getValue());
			i++;
		}
		ResultSet resultSet = statement.executeQuery();
		ResultSetMetaData metaData = resultSet.getMetaData();
		 while (resultSet.next()) {
			   Map<String, Object> row = new HashMap<String,Object>();
			   
				for(int r=1;r <= metaData.getColumnCount(); r++ ) {
						row.put(metaData.getColumnName(r),resultSet.getObject(metaData.getColumnName(r)));
				}

			   rows.add(row);
		   }
		 return rows;
	}
	
	public void checkAndInsert(String tableName, Map<String, Object> whereClause) throws Exception{
		List data = this.getRecords(Constants.DEST_CONNECTION_NAME, tableName, whereClause);
		if(data.isEmpty())
				this.insertRecordWOPK(Constants.DEST_CONNECTION_NAME, tableName,whereClause);		
	}
	
	public Boolean checkIsExists(String tableName, Map<String, Object> whereClause) throws Exception{
		List data = this.getRecords(Constants.DEST_CONNECTION_NAME, tableName, whereClause);
		if(data.isEmpty()) 
			return false;
		else
			return true;
	}
	
	public void updateDataAsperBIQ(Object requestId,Long sourceRequestId) {
		try {
		//	JdbcTemplate jdbcTemplate = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME);
		//	JdbcTemplate srcTemplate = dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME);
			String updateExtAtendees= "UPDATE BI_REQUEST_ATTENDEES SET ATTENDEE_TYPE = 'externalattendees' WHERE UPPER(ATTENDEE_TYPE) = UPPER('external') AND REQUEST_ID ="+requestId;
			String updateIntAtendees= "UPDATE BI_REQUEST_ATTENDEES SET ATTENDEE_TYPE = 'internalattendee' WHERE UPPER(ATTENDEE_TYPE) = UPPER('internal') AND REQUEST_ID ="+requestId;
//			String updateArival = "UPDATE BI_REQUEST_ACTIVITY_DAY SET ARRIVAL = to_char(ARRIVAL_TS, 'hh:mi am') , ADJOURN = to_char(ADJOURN_TS, 'hh:mi am') WHERE ARRIVAL IS NULL AND ADJOURN IS NULL AND REQUEST_ID ="+requestId;
			String updateActivityCateringId = "UPDATE  BI_REQUEST_CATERING_ACTIVITY CAT SET CAT.REQUEST_ACTIVITY_DAY_ID =  ( SELECT ID FROM BI_REQUEST_ACTIVITY_DAY ACT WHERE ACT.REQUEST_ID = CAT.REQUEST_ID AND ACT.EVENT_DATE = CAT.ACTIVITY_DATE AND ROWNUM = 1  ) WHERE CAT.REQUEST_ACTIVITY_DAY_ID IS NULL AND CAT.ACTIVITY_DATE IS NOT NULL AND CAT.REQUEST_ID IS NOT NULL AND REQUEST_ID ="+requestId;
			String updateActivityTopicId = "UPDATE  BI_REQUEST_TOPIC_ACTIVITY CAT SET CAT.REQUEST_ACTIVITY_DAY_ID =  ( SELECT ID FROM BI_REQUEST_ACTIVITY_DAY ACT WHERE ACT.REQUEST_ID = CAT.REQUEST_ID AND ACT.EVENT_DATE = CAT.ACTIVITY_DATE AND ROWNUM = 1  ) WHERE CAT.REQUEST_ACTIVITY_DAY_ID IS NULL AND CAT.ACTIVITY_DATE IS NOT NULL AND CAT.REQUEST_ID IS NOT NULL AND REQUEST_ID ="+requestId;
//			String updateRequestDuration = "UPDATE BI_REQUEST SET DURATION = (select max(EVENT_DATE) - min(EVENT_DATE) +1  from BI_REQUEST_ACTIVITY_DAY where REQUEST_ID= "+requestId+ " ) WHERE ID = "+requestId;
			
			daoFunctions.correctActvityDays(Long.parseLong(requestId.toString()));
			daoFunctions.populateMainRoom(Long.parseLong(requestId.toString()), sourceRequestId);
			daoFunctions.populateBreakoutRooms(Long.parseLong(requestId.toString()), sourceRequestId);
			
			dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(updateExtAtendees);
			dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(updateIntAtendees);
//			jdbcTemplate.update(updateArival);
			dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(updateActivityCateringId);
			dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(updateActivityTopicId);
//			jdbcTemplate.update(updateRequestDuration);
			
			
			
			
/*			Map<Object,List> breakRooms = new HashMap<Object,List>();
			List<Map<String, Object>> activities= dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).queryForList("SELECT distinct(cap.room) rooms, cap.request_activity_day_id FROM bi_request_catering_activity cap, BI_REQUEST_ACTIVITY_DAY acd WHERE acd.id = cap.request_activity_day_id and cap.REQUEST_ID = "+requestId +" and cap.room not in (acd.main_room) GROUP BY cap.request_activity_day_id,cap.room " );
			
			for(Map<String,Object> activity : activities) {
			if(activity.get("rooms")!=null) { 
		        List rooms =  breakRooms.get(activity.get("REQUEST_ACTIVITY_DAY_ID"));
		        if(rooms==null) rooms = new ArrayList<>();
		        rooms.add(activity.get("rooms"));
			    breakRooms.put(activity.get("REQUEST_ACTIVITY_DAY_ID") , rooms);
			  }
			}
			activities= dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).queryForList("SELECT distinct(cap.room) rooms, cap.request_activity_day_id FROM bi_request_topic_activity cap, BI_REQUEST_ACTIVITY_DAY acd WHERE acd.id = cap.request_activity_day_id and cap.REQUEST_ID = "+requestId +" and cap.room not in (acd.main_room) GROUP BY cap.request_activity_day_id,cap.room ");
			for(Map<String,Object> activity : activities) {
				if(activity.get("rooms")!=null) { 
			        List rooms =  breakRooms.get(activity.get("REQUEST_ACTIVITY_DAY_ID"));
			        if(rooms==null) rooms = new ArrayList<>();
			        rooms.add(activity.get("rooms"));
				    breakRooms.put(activity.get("REQUEST_ACTIVITY_DAY_ID") , rooms);
				}
			}

			for(Map.Entry<Object, List> entry : breakRooms.entrySet()) {
				Set rooms = new HashSet<>();
				rooms.addAll(entry.getValue());
				
				String breakRoomQuery = "INSERT INTO BI_REQUEST_ACT_DAY_BREAK_ROOM (REQUEST_ACTIVITY_DAY_ID, BREAK_ROOMS) VALUES (?, ?)";
				for(Object room : rooms) {
					if(entry.getKey()!=null && room!=null)
						dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(breakRoomQuery, new Object[] {entry.getKey(),room});
				}
			}*/

			Map<String,Object> requestData = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).queryForMap("SELECT * FROM BI_REQUEST WHERE ID = "+requestId);
			
			if(requestData.get("BRIEFING_MANAGER")!=null)	
				daoFunctions.insertLocationUser(Long.parseLong(requestData.get("BRIEFING_MANAGER").toString()), Long.parseLong(requestData.get("LOCATION_ID").toString()), 2);
				
			
			List<Map<String,Object>> requests=   dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME).queryForList("SELECT OPP_NUMBER FROM VW_CVC_REQUEST WHERE ID ="+sourceRequestId);
			
			Object oppNumber = requests.get(0).get("OPP_NUMBER");
			if(oppNumber!=null) {
				List<String> oppNumbers = Arrays.asList(oppNumber.toString().replaceAll(" ",",").split(","));
				oppNumbers.removeAll(Collections.singletonList("null"));
				oppNumbers.removeAll(Collections.singletonList(null));
				oppNumbers.removeAll(Collections.singletonList(" "));
				for(String oppId : oppNumbers) {
					dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update("INSERT INTO BI_REQUEST_OPPORTUNITY (OPPORTUNITY_ID,REQUEST_ID) VALUES ('"+oppId+"',"+requestId+")");
				}
			}
			
			daoFunctions.updateCostCenter(Long.parseLong(requestId.toString()), sourceRequestId);
			
			//dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update("UPDATE BI_LOCATION SET PARENT_ID = "+ Long.parseLong(requestData.get("LOCATION_ID").toString()) +" where PARENT_ID is null and id not in ( "+ Long.parseLong(requestData.get("LOCATION_ID").toString()) +" )");
		}catch (Exception e) {
			e.printStackTrace();
			logger.debug("Exception while updateDataAsperBIQ (BIQ ID "+requestId+" CVC ID "+sourceRequestId+" )");
		}
			logger.debug(" updateDataAsperBIQ executed");		
	}	
	
	public static boolean isDayLightSavingDate(Date date) {
		return TimeZone.getTimeZone("PST").inDaylightTime(date);
	}
	
	public static boolean isDayLightSavingDate(java.sql.Date date) {
		return TimeZone.getTimeZone("PST").inDaylightTime(new Date(date.getTime()));
	}
	public static Date getUTCTime(Date date) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			if(TimeZone.getTimeZone("PST").inDaylightTime(date))
				calendar.add(Calendar.HOUR_OF_DAY, 7);
			else
				calendar.add(Calendar.HOUR_OF_DAY, 8);
			
			return calendar.getTime();
	}
	
	public static java.sql.Date getUTCTime(java.sql.Date date) {
		Date utilDate = new Date(date.getTime());
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(utilDate);
		if(TimeZone.getTimeZone("PST").inDaylightTime(utilDate))
			calendar.add(Calendar.HOUR_OF_DAY, 7);
		else
			calendar.add(Calendar.HOUR_OF_DAY, 8);
		return new java.sql.Date(calendar.getTime().getTime());
	}
	
	public static java.sql.Timestamp getUTCTime(java.sql.Timestamp date){
		Date utilDate = new Date(date.getTime());
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(utilDate);
		if(TimeZone.getTimeZone("PST").inDaylightTime(utilDate))
			calendar.add(Calendar.HOUR_OF_DAY, 7);
		else
			calendar.add(Calendar.HOUR_OF_DAY, 8);
		return new java.sql.Timestamp(calendar.getTime().getTime());
	}
	
	
	public static void main(String args[]) {
			List<Long> data = new ArrayList<>();
			data.add(130L);

			data.add(130L);
			System.out.println(StringUtils.join(data,","));
	}
}