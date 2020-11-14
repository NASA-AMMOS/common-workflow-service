package jpl.cws.scheduler;

import java.util.Date;

public class HistoryDetail {

	public Date date;
	public String type;
	public String activity;
	public String message;
	
	public HistoryDetail(Date date, String type, String activity, String message) {
		this.date = date;
		this.type = type;
		this.activity = activity;
		this.message = message;
	}
}
