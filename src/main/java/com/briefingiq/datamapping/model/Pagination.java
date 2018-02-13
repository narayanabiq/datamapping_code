package com.briefingiq.datamapping.model;

public class Pagination {

	private Integer from;
	private Integer returned;
	private Integer total;
	private Integer maximum;

	public Integer getMaximum() {
		return maximum;
	}


	public void setMaximum(Integer maximum) {
		this.maximum = maximum;
	}


	public Integer getFrom() {
		return from;
	}


	public void setFrom(Integer from) {
		this.from = from;
	}


	public Integer getReturned() {
		return returned;
	}


	public void setReturned(Integer returned) {
		this.returned = returned;
	}


	public Integer getTotal() {
		return total;
	}


	public void setTotal(Integer total) {
		this.total = total;
	}


}
