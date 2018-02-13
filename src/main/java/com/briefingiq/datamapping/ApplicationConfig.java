package com.briefingiq.datamapping;

import java.util.concurrent.Executor;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.JndiDataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jndi.JndiTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
public class ApplicationConfig {
	
	@Autowired
	Environment env;
	
	@Bean(name="srcDataSource")
	@ConfigurationProperties(prefix = "source.datasource")
	public DataSource srcDataSource() throws NamingException {

		if(env.getProperty("source.datasource.jndi-name")!=null){
			return (DataSource) new JndiTemplate().lookup(env.getProperty("source.datasource.jndi-name"));
		}else{
			return DataSourceBuilder.create().build();
		}
	}
	
	@Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("BIQDATAMAPPING-");
        executor.initialize();
        return executor;
    }
	
	@Bean(name="srcTemplate")
	public JdbcTemplate srcTemplate() throws NamingException {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(this.srcDataSource());
		return jdbcTemplate;
	}
	
	
	@Bean(name="destDataSource")
	@ConfigurationProperties(prefix = "dest.datasource")
	public DataSource destDataSource() throws NamingException {
		if(env.getProperty("dest.datasource.jndi-name")!=null){
			return (DataSource) new JndiTemplate().lookup(env.getProperty("dest.datasource.jndi-name"));
		}else{
			return DataSourceBuilder.create().build();
		}
	}
	
	@Bean(name="destTemplate")
	public JdbcTemplate destTemplate() throws NamingException {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(this.destDataSource());
		return jdbcTemplate;
	}
	
	
	@Bean(name="dbMapDataSource")
	@ConfigurationProperties(prefix = "dbmap.datasource")
	public DataSource dbMapDataSource() throws NamingException {
		if(env.getProperty("dbmap.datasource.jndi-name")!=null){
			return (DataSource) new JndiTemplate().lookup(env.getProperty("dbmap.datasource.jndi-name"));
		}else{
			return DataSourceBuilder.create().build();
		}

	}
	
	@Bean(name="dbMapTemplate")
	public JdbcTemplate dbMapemplate() throws NamingException {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(this.dbMapDataSource());
		return jdbcTemplate;
	}

	@Bean
	public Schedular schedular() {
		return new Schedular();
	}
	
}