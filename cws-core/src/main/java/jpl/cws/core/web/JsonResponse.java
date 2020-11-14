package jpl.cws.core.web;

import com.google.gson.GsonBuilder;

public class JsonResponse {
	public enum Status {
		SUCCESS,
		FAIL
	};
	
	private Status status;
	private String message;
	
	public JsonResponse(Status status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public Status getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}
}
