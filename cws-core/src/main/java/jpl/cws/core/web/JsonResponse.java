package jpl.cws.core.web;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringEscapeUtils;

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
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(this);

		return StringEscapeUtils.unescapeJava(json);
	}
}
