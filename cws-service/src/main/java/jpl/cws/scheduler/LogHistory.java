package jpl.cws.scheduler;

import java.util.ArrayList;
import java.util.List;

public class LogHistory {

	public String procDefKey = null;
	public String procInstId = null;
	public String startTime = null;
	public String endTime = null;
	public String state = null;
	public Long duration = 0L;
	
	public List<HistoryDetail> details = new ArrayList<HistoryDetail>();
}
