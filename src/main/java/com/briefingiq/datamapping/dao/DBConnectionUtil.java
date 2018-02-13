package com.briefingiq.datamapping.dao;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DBConnectionUtil {
	

	final static Logger logger = Logger.getLogger(DBConnectionUtil.class);
	
	
	@Autowired
	@Qualifier(value="srcDataSource") 
	private DataSource srcDataSource;
	
	@Autowired
	@Qualifier("destDataSource")
	private DataSource destDataSource;
	
	@Autowired
	@Qualifier("dbMapDataSource")
	private DataSource dataMappingDataSource;
	
	
	
	@Autowired
	@Qualifier("destTemplate")
	private JdbcTemplate destTemplate;
	
	@Autowired
	@Qualifier("dbMapTemplate")
	private JdbcTemplate dbMapTemplate;
	
	@Autowired
	@Qualifier("srcTemplate")
	private JdbcTemplate srcTemplate;

	
	/*public static Connection getSourceConnection() throws Exception {
		 Class.forName("oracle.jdbc.driver.OracleDriver");
		 Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@//briefingiqdb.clorkmuixiwz.us-west-2.rds.amazonaws.com:1521/ORCL",
				 "CX_CVC", "tiger");
		return connection;
	}
	
	
	public static Connection getDestConnection() throws Exception {
		 Class.forName("oracle.jdbc.driver.OracleDriver");
		 Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@//briefingiqdb.clorkmuixiwz.us-west-2.rds.amazonaws.com:1521/ORCL",
			//	 "briefingiq_admin", "testing1234");
				 "biqadmin_new", "tiger");
		return connection;
	}
	
	
	public static Connection getDataMappingConnection() throws Exception {
		 Class.forName("oracle.jdbc.driver.OracleDriver");
		 Connection connection = DriverManager.getConnection("jdbc:oracle:thin:@//briefingiqdb.clorkmuixiwz.us-west-2.rds.amazonaws.com:1521/ORCL",
				 "CX_CVC", "tiger");
		return connection;
	}
	
	public Connection getConnection(String connectionName) throws Exception{		
		Connection connection= null;
		
		switch (connectionName) {
		case "SOURCE":
			connection = srcDataSource.getConnection();
		break;
		case "DESTNATION" :
			connection = destDataSource.getConnection();
			break;
		
		case "DATAMAPPING" :
			connection = dataMappingDataSource.getConnection();
			break;
		default:
			break;
		}
		
		return connection;
		
	}*/
	
	
	public JdbcTemplate getTemplate(String dbName){
		
		JdbcTemplate jdbcTemplate= null;
		
		switch (dbName) {
		case "SOURCE":
			jdbcTemplate = this.srcTemplate;
		break;
		case "DESTNATION" :
			jdbcTemplate = this.destTemplate;
			break;
		
		case "DATAMAPPING" :
			jdbcTemplate = this.dbMapTemplate;
			break;
		default:
			break;
		}
		
		return jdbcTemplate;
	}
	
}