package jpl.cws.core.code;

import java.io.UnsupportedEncodingException;

import org.camunda.bpm.engine.delegate.DelegateExecution;

public class CwsCoreMethods {
	
	// FIXME:  this method should be somehow accessible from the CustomMethods class (snippets)
	
	public Object getVar(String variableName, DelegateExecution execution) throws Exception {
		if (execution.hasVariable(variableName+"_bytes")) {
			// If this variable exists, then it means that CWS set this because
			// a String value was too large.
			// See: https://groups.google.com/forum/#!searchin/camunda-bpm-users/galen/camunda-bpm-users/B8I47n28cns/RcYWBD2XbIUJ
			// See: https://jira.camunda.com/browse/CAM-2601
			try {
				return new String(
						(byte[])execution.getVariable(variableName+"_bytes"),
						"utf-8");
			} catch (UnsupportedEncodingException e) {
	//			log.error("getVariable Error", e);
				throw e;
			}
		}
		else {
			return execution.getVariable(variableName);
		}
	}
	
}
