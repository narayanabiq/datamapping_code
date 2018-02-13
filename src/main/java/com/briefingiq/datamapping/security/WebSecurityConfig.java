package com.briefingiq.datamapping.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  // @formatter:off
  @Override
  protected void configure(HttpSecurity http) throws Exception {http
      .authorizeRequests()
      .anyRequest().authenticated()
      .and()
  .formLogin()
      .permitAll()
      .and()
      .csrf().disable()
  .logout()
      .permitAll();
  }


  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception { 
	  auth
      .inMemoryAuthentication()
      .withUser("surya@allianceit.com").password("Password@123").roles("USER");
	  auth
      .inMemoryAuthentication()
      .withUser("john.doe@briefingiq.com").password("Password@123").roles("USER");
	  auth
      .inMemoryAuthentication()
      .withUser("deepa@allianceit.com").password("Password@123").roles("USER");
	  auth
      .inMemoryAuthentication()
      .withUser("naveen.chegondi@oracle.com").password("Password@123").roles("USER");
	  auth
      .inMemoryAuthentication()
      .withUser("dataadmin@allianceit.com").password("Password@123").roles("USER");
	  
}

}
