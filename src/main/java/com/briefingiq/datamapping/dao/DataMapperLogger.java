package com.briefingiq.datamapping.dao;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataMapperLogger {
	
	@Autowired
	@Qualifier("dbMapTemplate")
	private JdbcTemplate jdbcTemplate;
	

	public void log(String message,Boolean status,Long requestId) {
			String sql = "insert into T_DATA_MAPPER_LOG (id,request_id,message,DATE_TIME,status) values (BI_ASSET_DETAIL_DOCUMENTS_SEQ.nextval,?,?,?,?)";
			Object[] args = {requestId,message,new Date(),status== true ? 1 : 0};
			jdbcTemplate.update(sql, args);

		
	}
	
}