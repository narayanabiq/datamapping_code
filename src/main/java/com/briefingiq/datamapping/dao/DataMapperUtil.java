package com.briefingiq.datamapping.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.briefingiq.datamapping.Constants;
import com.briefingiq.datamapping.model.CVCRequest;

@Component
public class DataMapperUtil {
	
	
	final static Logger logger = Logger.getLogger(DataMapperUtil.class);
	
	
	@Autowired
	DataMapperLogger dataMapperLogger;

	@Autowired
	QueryUtil queryUtil;
	
	@Autowired
	DBConnectionUtil dbConnectionUtil;
	
	@Autowired
	DaoFunctions daoFunctions;
	
	
	public void loadData() throws Exception{
		//dataMapperLogger.log("Inserting requests to CVC_REQUEST_PROCESS",true);
		   queryUtil.loadData();
		//dataMapperLogger.log(list.size()+ " requests inserted to CVC_REQUEST_PROCESS",true);
	}
	
	public void storedProcedure(Long requestId,Long fromDate,Long toDate,String status,String email,String truncate) throws Exception{
		queryUtil.storedProcedure(requestId,fromDate,toDate,status,email,truncate);
	}
	
	public void deleteRequests(List<String> requests) throws Exception{
		//dataMapperLogger.log("Deleting requests from CVC_REQUEST_PROCESS",true);
		queryUtil.deleteRequests(requests,true);
		//dataMapperLogger.log(rows+" requests deleted from CVC_REQUEST_PROCESS",true);
	}
	
	public List<Map<String, Object>> getColumnMapping(Long srdId,Long destId,Long requestId) throws Exception {
		dataMapperLogger.log("Getting column mapping information "+srdId+" ->"+destId,true,requestId);
		
		Map<String,Object> whereClause = new HashMap<String,Object>();
		whereClause.put("SRC_TABLE", srdId);
		whereClause.put("DEST_TABLE", destId);
		
		
		
		List<Map<String, Object>> columnMappings = queryUtil.getRecords(Constants.DBMAP_CONNECTION_NAME, "T_TABLE_COLUMN_MAP", whereClause);
		
		dataMapperLogger.log(" Column mapping details retrived. "+columnMappings.size()+" mappings found",true,requestId);
		return columnMappings;
	}
	
	public List<Map<String,Object>> getSourceFkData( String tableName,String columnName,Object value)throws Exception {		
		Map<String,Object> whereClause = new HashMap<String,Object>();
		whereClause.put(columnName, value);

		List<Map<String, Object>> sourceData = queryUtil.getRecords(Constants.SRC_CONNECTION_NAME, tableName, whereClause);
		
		return sourceData;
	}
	
	
	public Object executeQuery(String query, Object value) throws Exception {
		Connection connection =null;
		PreparedStatement statement =null;
		ResultSet rs = null;
		try {
			connection = dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).getDataSource().getConnection();
			statement = connection.prepareStatement(query);
			statement.setObject(1, value);
			rs = statement.executeQuery();
			rs.next();
			return rs.getObject(1);
		}catch (Exception e) {
				e.printStackTrace();
			return null;
		}finally {
			if(rs!=null) rs.close();
			if(statement!=null) statement.close();
			if(connection!=null &&  !connection.isClosed()) connection.close();
		}
	}
	
	
	public Object getDestFKValue(Map<String, Object> columns,Object sourceValue, String sourceColumn,Long requestId) throws Exception {
		
		dataMapperLogger.log("Identification of forien key reference for coulumn "+sourceColumn,true,requestId);
		
		if(sourceValue==null) 		
			dataMapperLogger.log("Source column value is null, returns null for destination column.",true,requestId);
		if(sourceValue==null) return null;
		
		
		Map<String,Object> processWhereClause = new HashMap<String,Object>();
		processWhereClause.put("ID", columns.get("PROCESS_ID").toString());
		List<Map<String,Object>> processData = queryUtil.getRecords(Constants.DBMAP_CONNECTION_NAME, "T_DATA_MAP_PROCESS_DETAILS", processWhereClause);
		
		Map<String,Object> whereConditions = new HashMap<String,Object>();
		whereConditions.put(columns.get("SRC_FK_COLUMN").toString(), sourceValue);
		List<Map<String,Object>> sourceMasterData = queryUtil.getRecords(Constants.SRC_CONNECTION_NAME,columns.get("SRC_FK_TABLE").toString(), whereConditions);

		dataMapperLogger.log("Child records retrived from source table",true,requestId);
		Map<String,Object> columnMapConditions = new HashMap<String,Object>();		
		columnMapConditions.put("SRC_TABLE",processData.get(0).get("SRC_TABLE_ID"));
		columnMapConditions.put("DEST_TABLE", processData.get(0).get("DEST_TABLE_ID"));

		List<Map<String,Object>> columnMappings =queryUtil.getRecords(Constants.DBMAP_CONNECTION_NAME,"T_TABLE_COLUMN_MAP",columnMapConditions);

		dataMapperLogger.log("Read Column mapping deails. "+ processData.get(0).get("SRC_TABLE_ID")+" -> "+processData.get(0).get("DEST_TABLE_ID"),true,requestId);
		
		Map<String, Object> whereClause = new HashMap<String,Object>();
		for(Map<String, Object> columnsMap : columnMappings) {
			if(columnsMap.get("UNIQUE_COMBINATION")!=null && columnsMap.get("UNIQUE_COMBINATION").toString().equals("1")) {
			if(sourceMasterData.isEmpty()) {
				whereClause.put(columnsMap.get("DEST_COLUMN").toString(),sourceValue);
			}else {
				Map<String, Object> sourceData = sourceMasterData.get(0);
				whereClause.put(columnsMap.get("DEST_COLUMN").toString(), sourceData.get(columnsMap.get("SRC_COLUMN")));
			}
			}

		}

		dataMapperLogger.log("Preparing whare clause for destnation table."+columns.get("DEST_FK_TABLE").toString(),true,requestId);
		logger.debug("where clause "+whereClause);
		List<Map<String,Object>> destData = queryUtil.getRecords(Constants.DEST_CONNECTION_NAME,columns.get("DEST_FK_TABLE").toString(),whereClause);
		

		if(destData.isEmpty()) {
			
			dataMapperLogger.log("Destination record not found. Inserting new record in destnation table.",true,requestId);
			
					for(Map<String, Object> columnsMap : columnMappings) {
							if(columnsMap.get("SEQ_NAME")!=null) {
								whereClause.put(columnsMap.get("DEST_COLUMN").toString(), columnsMap.get("SEQ_NAME")+".nextval");
							}
							if(columnsMap.get("DEFAULT_VALUE")!=null) {
								whereClause.put(columnsMap.get("DEST_COLUMN").toString(), columnsMap.get("DEFAULT_VALUE"));
							}
					}
				Long dest = queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, columns.get("DEST_FK_TABLE").toString(), whereClause);
				dataMapperLogger.log("New record inserted in destnation table."+dest,true,requestId);
				return dest;
		}
		dataMapperLogger.log("Forign key reference is identified in destination table.",true,requestId);
		return destData.get(0).get(columns.get("DEST_FK_COLUMN").toString().toUpperCase()).toString();
	}
	
	
	public List<Map<String, Object>> getSourceData(String tableName,Long id) throws Exception{
			Map<String,Object> whereClause = new HashMap<String,Object>();
			whereClause.put("id", id);
			
			List<Map<String, Object>> sourceData = queryUtil.getRecords(Constants.SRC_CONNECTION_NAME, tableName, whereClause);
			
			logger.debug("  records count "+sourceData.size());
			
			return sourceData;			
	}
	
	
	public List<Map<String, Object>> getDestinationData(List<Map<String, Object>> sourceData,List<Map<String, Object>> columnMappings,Long requestId) throws Exception {
		dataMapperLogger.log(" Preparing destination table information. Table id "+columnMappings.get(0).get("DEST_TABLE"),true,requestId);
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		
		Map<String,Object> recordData = new HashMap<String,Object>();
		for(Map<String, Object> columns : columnMappings) {
		
			Map<String,Object> source = sourceData.get(0);
			
			String key=columns.get("DEST_COLUMN").toString();
			Object value = null;
			
			if(columns.get("DEFAULT_VALUE")!=null) {
				
				value = columns.get("DEFAULT_VALUE");
				if(value.toString().equalsIgnoreCase("CURRENT_TIMESTAMP")){
					value = new java.sql.Timestamp(new Date().getTime());
				}else if(value.toString().equalsIgnoreCase("SYS_GUID()")){
					value=UUID.randomUUID().toString().toUpperCase();
				}
				switch (columns.get("DEST_DATATYPE").toString()) {
				case "NUMBER" :
					value = ConverterUtil.toNumber(value);
					break;
				case "INTEGER" :
					value = ConverterUtil.toNumber(value);
					break;
				default:
					break;
				}
			}
			else if(columns.get("SEQ_NAME")!=null) {
					value =columns.get("SEQ_NAME").toString()+".nextval";	
			}else if(columns.get("SRC_FK_TABLE")!=null) {
					if(!columns.get("SRC_FK_TABLE").toString().equalsIgnoreCase("1")) {

				value = this.getDestFKValue(columns, source.get(columns.get("SRC_COLUMN").toString().toUpperCase()),columns.get("SRC_COLUMN").toString(),requestId);
					}
			}else if(columns.get("EXEC_FUNCTION")!=null) {
				
				  value =  daoFunctions.executeFunction(columns.get("EXEC_FUNCTION").toString(), source.get(columns.get("SRC_COLUMN").toString().toUpperCase()));
			
			}else if (columns.get("FK_QUERY")!=null){
					value = this.executeQuery(columns.get("FK_QUERY").toString(),source.get(columns.get("SRC_COLUMN").toString().toUpperCase()));
			}	else {
			
				value =  source.get(columns.get("SRC_COLUMN").toString().toUpperCase());
				
				
				switch (columns.get("DEST_DATATYPE").toString()) {
				case "DATE":
					value = ConverterUtil.toDate(value);
					break;
				case "NUMBER" :
					value = ConverterUtil.toNumber(value);
					break;
				case "INTEGER" :
					value = ConverterUtil.toNumber(value);
					break;
				case "BOOLEAN" :
					value = ConverterUtil.toBoolean(value);
					break;
				case "TIMESTAMP" :
					value = ConverterUtil.toTimestamp(value);
					break;
				default:
					break;
				}
			}
			
			recordData.put(key, value);
			
		}
		dataMapperLogger.log(" destination table row constructed. Table id "+columnMappings.get(0).get("DEST_TABLE"),true,requestId);
		data.add(recordData);
		
		return data;
	}
	
	
	
	@Async
	public void processData(String processName,Long requestId) throws Exception {
		CVCRequest request=new CVCRequest();
		request.setStartDate(new Timestamp(System.currentTimeMillis()));
		request.setStatus("In Progress");
		request.setRequestId(requestId);
		updateCVCRequestProcess(request);
		dataMapperLogger.log(" Data migration process started for the CVC request "+requestId,true,requestId);
		String processDetails = "SELECT src.id SRCTID,src.TABLE_NAME SRCTNAME,"
				+ "dest.id DESTTID,dest.table_name DESTTNAME,dt.PROCESS_QUERY QUERY FROM t_data_map_process_details dt, t_data_map_process pr, t_src_table src, t_dest_table dest " + 
				"WHERE pr.id = dt.process_id AND src.id = dt.SRC_TABLE_ID AND dest.id = dt.DEST_TABLE_ID AND pr.process_name = 'REQUEST' " + 
				"ORDER BY dt.process_order asc";
		Map<String, Object> destPks = new HashMap<String,Object>();
			try {
				List<Map<String, Object>> rows = queryUtil.getRecordsByQuery(Constants.DBMAP_CONNECTION_NAME, processDetails, new HashMap<>() ); 
				
				
				
				for(Map<String, Object> row : rows) {
					if(row.get("QUERY")==null) {
							dataMapperLogger.log("Data migration process started for the the table "+row.get("SRCTNAME")+" -> "+row.get("DESTTNAME").toString(),true,requestId);
							List<Map<String, Object>> columnMappings  = this.getColumnMapping(Long.parseLong(row.get("SRCTID").toString()),(Long.parseLong(row.get("DESTTID").toString())),requestId);
							List<Map<String, Object>> data = this.getSourceData(row.get("SRCTNAME").toString(),requestId);
							List<Map<String, Object>> destData =this.getDestinationData(data, columnMappings, requestId);
							Long pk =queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, row.get("DESTTNAME").toString(), destData.get(0));
							dataMapperLogger.log("Data migration process completed for the table "+row.get("SRCTNAME")+" -> "+row.get("DESTTNAME").toString(),true,requestId);
							destPks.put(row.get("DESTTNAME").toString(), pk);
					}else {
						dataMapperLogger.log("Data migration process started for the the table "+row.get("SRCTNAME")+" -> "+row.get("DESTTNAME").toString(),true,requestId);
						Map<String, Object> whereClause = new HashMap<String,Object>();
						whereClause.put("1", requestId);
						List<Map<String, Object>> chRows = queryUtil.getRecordsByQuery(Constants.SRC_CONNECTION_NAME, row.get("QUERY").toString(), whereClause);
						
						for(Map<String, Object> chRow : chRows) {
							
							List<Map<String, Object>> columnMappings  = this.getColumnMapping(Long.parseLong(row.get("SRCTID").toString()),(Long.parseLong(row.get("DESTTID").toString())),requestId);
							List<Map<String, Object>> data = this.getSourceData(row.get("SRCTNAME").toString(),Long.parseLong(chRow.get("ID").toString()));
							List<Map<String, Object>> destData =this.getDestinationData(data, columnMappings,requestId);
							Long pk = queryUtil.insertRecord(Constants.DEST_CONNECTION_NAME, row.get("DESTTNAME").toString(), destData.get(0));
							dataMapperLogger.log("Data migration process completed for the table "+row.get("SRCTNAME")+" -> "+row.get("DESTTNAME").toString(),true,requestId);
							destPks.put(row.get("DESTTNAME").toString(), pk);
						}
					}
					
				}
				queryUtil.updateDataAsperBIQ(destPks.get("BI_REQUEST"),requestId);
				dataMapperLogger.log(" Data migration completed. Request created with id "+destPks.get("BI_REQUEST")+" BIQ Data base",true,requestId);
				request.setStatus("Completed");
				request.setBiRequestId((Long) destPks.get("BI_REQUEST"));
				request.setEndDate(new Timestamp(System.currentTimeMillis()));
				updateCVCRequestProcess(request);
			}catch (Exception e) {
				System.out.println(ExceptionUtils.getFullStackTrace(e));
				request.setStatus("Failed");
				List<String> requests = new ArrayList<>();
				requests.add(destPks.get("BI_REQUEST").toString());
				queryUtil.deleteRequests(requests,false);
				updateCVCRequestProcess(request);
				e.printStackTrace();
			}finally {
				//if(statement!=null) statement.close();
				//if(connection!=null && !connection.isClosed()) connection.close();
			}
			
		
	}

	public void updateCVCRequestProcess(CVCRequest request) throws Exception{
		StringBuffer buffer=new StringBuffer();
		buffer.append("update cvc_request_process set status='"+request.getStatus()+"' , start_date='"+ConverterUtil.getTimestampFormat(request.getStartDate(),"dd-MMM-yy h:mm:ss.SSS a")+"' ");
		if(request.getEndDate()!=null) buffer.append(" , end_date='"+ConverterUtil.getTimestampFormat(request.getEndDate(),"dd-MMM-yy h:mm:ss.SSS a")+"' ");
		if(request.getBiRequestId()!=null) buffer.append(" , bi_request_id="+request.getBiRequestId()+" ");
		buffer.append(" where request_id='"+request.getRequestId()+"'");
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).update(buffer.toString());
		
	}
	
	public static void  main(String args[]) throws Exception {
		DataMapperUtil util = new DataMapperUtil();
		 Long requestId= 22744L;
		util.processData("REQUEST",requestId);
/*		 Long request= 22744L;
		List<Map<String, Object>> columnMappings  = util.getColumnMapping(1L,1L);
		List<Map<String, Object>> data = util.getSourceData("CVC_REQUEST",request);
		List<Map<String, Object>> destData =util.getDestinationData(data, columnMappings);
		
		System.out.println("  dest data "+destData.toString());
		
		QueryUtil queryUtil = new QueryUtil();*/
	//	queryUtil.insertRecord(DBConnectionUtil.getDestConnection(), "BI_REQUEST", destData.get(0));
		
	}
}
