package com.briefingiq.datamapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.briefingiq.datamapping.dao.DBConnectionUtil;
import com.briefingiq.datamapping.dao.DataMapperUtil;
import com.briefingiq.datamapping.dao.QueryUtil;
import com.briefingiq.datamapping.model.CVCRequest;
import com.briefingiq.datamapping.model.CVCRequests;
import com.briefingiq.datamapping.model.Pagination;
import com.briefingiq.datamapping.model.ProcessLog;

@RestController
@RequestMapping("requests")
public class CvcRequestsController {
	
	final static Logger logger = Logger.getLogger(CvcRequestsController.class);
	
	@Autowired
	QueryUtil queryUtil;
	
	@Autowired
	DataMapperUtil dataMapperUtil;
	
	@Autowired
	DBConnectionUtil dbConnectionUtil;

	@RequestMapping(method=RequestMethod.GET)
	public CVCRequests getRequests(
			@RequestParam(name="requestId",required=false) Long requestId,
			@RequestParam(name="fromDate",required=false) Long fromdate,
			@RequestParam(name="toDate",required=false) Long toDate,
			@RequestParam(name="processed",required=false) Boolean processed,
			@RequestParam(name="status",required=false) List<String> status,
			@RequestParam(name="start") Long start,
			@RequestParam(name="limit") Long limit,
			@RequestParam(name="briefingManager",required=false) String briefingManager
			) {

		
		if(fromdate!=null && toDate==null) {
			toDate = new Date().getTime();
		}else if(fromdate==null && toDate!=null) {
			fromdate = new Date().getTime();
		}
		
		
		
		Integer count = queryUtil.getRequestDataCount(requestId,fromdate,toDate,processed,status,briefingManager,start,start+limit);
		List data = queryUtil.getRequestData(requestId,fromdate,toDate,processed,status,briefingManager,start,start+limit);
		
		CVCRequests requests = new CVCRequests();
		requests.setRequests(data);
		
		Pagination pagination = new Pagination();
		pagination.setFrom(start.intValue());
		pagination.setMaximum(200);
		pagination.setTotal(count);
		pagination.setReturned(data.size());

		requests.setPagination(pagination);
		
		return requests;
		
	}
	@RequestMapping(method=RequestMethod.PUT,value="/storedprocedure")
	public void storedProcedure(@RequestParam(name="requestId",required=false) Long requestId,
								@RequestParam(name="fromDate",required=false) Long fromDate,
								@RequestParam(name="toDate",required=false) Long toDate,
								@RequestParam(name="status",required=false) String status,
								@RequestParam(name="email",required=false) String email,
								@RequestParam(name="truncate",required=false) String truncate) throws Exception {
		
		dataMapperUtil.storedProcedure(requestId,fromDate,toDate,status,email,truncate);
		
	}
	@RequestMapping(method=RequestMethod.GET,value="/load")
	public void loadRequests() throws Exception {
		
		 dataMapperUtil.loadData();
	}
	
	@RequestMapping(method=RequestMethod.DELETE,value="/delete")
	public void deleteRequests(@RequestParam("requestIds") List<String> requestIds) throws Exception {
		
		dataMapperUtil.deleteRequests(requestIds);
	}
	
	@RequestMapping(method=RequestMethod.DELETE , value="/deletedata")
	public void deleteData(@RequestParam("data") List<String> requests) throws Exception {
		queryUtil.deleteRequests(requests,true);
		
	}
	
	@RequestMapping(method=RequestMethod.PUT)
	public void processRequest(@RequestParam("requests") List<Long> requests) throws Exception {
		for(Long request : requests) {
			Map<String,Object> requestDtls = dbConnectionUtil.getTemplate(Constants.DBMAP_CONNECTION_NAME).queryForMap("SELECT * FROM CVC_REQUEST_PROCESS WHERE REQUEST_ID = "+request);
				if(requestDtls.get("BI_REQUEST_ID")==null) {					
					dataMapperUtil.processData("REQUEST",request);
				}else if(requestDtls.get("STATUS").toString().equalsIgnoreCase("Failed")){
						dbConnectionUtil.getTemplate(Constants.DBMAP_CONNECTION_NAME).update(" delete from T_DATA_MAPPER_LOG where REQUEST_ID in ( "+ request +")");
						dataMapperUtil.processData("REQUEST",request);
				}else if(requestDtls.get("BI_REQUEST_ID")!=null) {
					logger.debug("Selected request "+request+" is already processed");
				}
		}
		
	}
		
	@RequestMapping(method=RequestMethod.GET,value="/logs")
	public List<ProcessLog> getRequestProcessLog(@RequestParam("requestId") Long requestId){
	List<ProcessLog> processLogs = new ArrayList<ProcessLog>();

	return queryUtil.getProcessLog(requestId);
		
	}
	

}