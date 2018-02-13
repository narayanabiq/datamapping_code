package com.briefingiq.datamapping.model;

public abstract class PaginatedModel extends ClientModel{
	
	private Pagination pagination;

	public Pagination getPagination() {
		if(pagination==null) pagination = new Pagination();
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}


}
