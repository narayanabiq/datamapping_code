package com.briefingiq.datamapping.model;

import java.util.ArrayList;
import java.util.List;

public class CVCRequests extends PaginatedModel {

	
	private List<CVCRequest> requests = new ArrayList<CVCRequest>();

	public List<CVCRequest> getRequests() {
		return requests;
	}

	public void setRequests(List<CVCRequest> requests) {
		this.requests = requests;
	}
	
}