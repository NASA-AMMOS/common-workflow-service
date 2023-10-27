package jpl.cws.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogHistory {

	public String procDefKey = null;
	public String procInstId = null;
	public String startTime = null;
	public String endTime = null;
	public String state = null;
	public Long duration = 0L;
	
	public List<HistoryDetail> details = new ArrayList<HistoryDetail>();
	public Map<String, String> inputVariables = new HashMap<String, String>();
	public Map<String, String> outputVariables = new HashMap<String, String>();
}
