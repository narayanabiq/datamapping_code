package com.briefingiq.datamapping.model;

import java.sql.Timestamp;
import java.util.Date;

public class CVCRequest {

	private Long requestId;
	private String companyName;
	private Date eventStartDate;
	private String hostName;
	private String location;
	private String country;
	private String status;
	private Date startDate;
	private Date endDate;
	private Long biRequestId; 
	private String reqStatus;
	private String briefingManager;
	
	public Long getBiRequestId() {
		return biRequestId;
	}
	public void setBiRequestId(Long biRequestId) {
		this.biRequestId = biRequestId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public void setEndDate(Timestamp endDate) {
		this.endDate = endDate;
	}
	public Long getRequestId() {
		return requestId;
	}
	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public Date getEventStartDate() {
		return eventStartDate;
	}
	public void setEventStartDate(Date eventStartDate) {
		this.eventStartDate = eventStartDate;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getReqStatus() {
		return reqStatus;
	}
	public void setReqStatus(String reqStatus) {
		this.reqStatus = reqStatus;
	}
	public String getBriefingManager() {
		return briefingManager;
	}
	public void setBriefingManager(String briefingManager) {
		this.briefingManager = briefingManager;
	}
}