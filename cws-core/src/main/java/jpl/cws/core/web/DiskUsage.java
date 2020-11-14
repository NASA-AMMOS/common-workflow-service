package jpl.cws.core.web;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;

public class DiskUsage {

	public long databaseSize;
	
	public List<WorkerInfo> workers = new ArrayList<WorkerInfo>();

	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}
}
