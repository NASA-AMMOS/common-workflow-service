package jpl.cws.scheduler;

import java.util.List;


public class CwsProcessInstancesDTO {
    public int draw;
	public int recordsTotal;
	public int recordsFiltered;

	public List<CwsProcessInstance> data;

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}

	public int getRecordsTotal() {
		return recordsTotal;
	}

	public void setRecordsTotal(int recordsTotal) {
		this.recordsTotal = recordsTotal;
	}

	public int getRecordsFiltered() {
		return recordsFiltered;
	}

	public void setRecordsFiltered(int recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

	public List<CwsProcessInstance> getData() {
		return data;
	}

	public void setData(List<CwsProcessInstance> data) {
		this.data = data;
	}
}
