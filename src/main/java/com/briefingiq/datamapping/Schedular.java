package com.briefingiq.datamapping;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.briefingiq.datamapping.dao.DBConnectionUtil;

public class Schedular {

	
	final static Logger logger = Logger.getLogger(Schedular.class);
	
	
	@Autowired
	DBConnectionUtil dbConnectionUtil;
	
	@Scheduled(initialDelay=20,fixedRate=60000)
	public void checkDB() {
		dbConnectionUtil.getTemplate(Constants.SRC_CONNECTION_NAME).execute("select 1 from dual");
		dbConnectionUtil.getTemplate(Constants.DEST_CONNECTION_NAME).execute("select 1 from dual");
		dbConnectionUtil.getTemplate(Constants.DBMAP_CONNECTION_NAME).execute("select 1 from dual");
		logger.debug("Schedular executed");
	}
}
