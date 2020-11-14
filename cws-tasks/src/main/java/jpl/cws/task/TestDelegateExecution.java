package jpl.cws.task;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;

/**
 * Used for JUnit tests.
 * 
 * getCurrentActivityId() will return "test", so using CwsTask.setOutputVariable
 * with variable name X will be result in an actual variable name of "test_X".
 * 
 * @author schrock
 * 
 */
public class TestDelegateExecution implements DelegateExecution {

	public static final String VAR_PREFIX = "test_";

	private Map<String, Object> variables = new HashMap<String, Object>();
	private Map<String, Object> variablesLocal = new HashMap<String, Object>();

	@Override
	public Map<String, Object> getVariables() {
		return variables;
	}

	@Override
	public Map<String, Object> getVariablesLocal() {
		return variablesLocal;
	}

	@Override
	public Object getVariable(String variableName) {
		return variables.get(variableName);
	}

	@Override
	public Object getVariableLocal(String variableName) {
		return variablesLocal.get(variableName);
	}

	@Override
	public Set<String> getVariableNames() {
		return variables.keySet();
	}

	@Override
	public Set<String> getVariableNamesLocal() {
		return variablesLocal.keySet();
	}

	@Override
	public void setVariable(String variableName, Object value) {
		variables.put(variableName, value);
	}

	@Override
	public void setVariableLocal(String variableName, Object value) {
		variablesLocal.put(variableName, value);
	}

	@Override
	public void setVariables(Map<String, ? extends Object> variables) {
		for (String variableName : variables.keySet()) {
			setVariable(variableName, variables.get(variableName));
		}
	}

	@Override
	public void setVariablesLocal(Map<String, ? extends Object> variables) {
		for (String variableName : variables.keySet()) {
			setVariableLocal(variableName, variables.get(variableName));
		}
	}

	@Override
	public boolean hasVariables() {
		return variables.size() > 0;
	}

	@Override
	public boolean hasVariablesLocal() {
		return variablesLocal.size() > 0;
	}

	@Override
	public boolean hasVariable(String variableName) {
		return variables.keySet().contains(variableName);
	}

	@Override
	public boolean hasVariableLocal(String variableName) {
		return variablesLocal.keySet().contains(variableName);
	}

	@Override
	public void removeVariable(String variableName) {
		variables.remove(variableName);
	}

	@Override
	public void removeVariableLocal(String variableName) {
		variablesLocal.remove(variableName);
	}

	@Override
	public void removeVariables(Collection<String> variableNames) {
		for (String variableName : variableNames) {
			variables.remove(variableName);
		}
	}

	@Override
	public void removeVariablesLocal(Collection<String> variableNames) {
		for (String variableName : variableNames) {
			variablesLocal.remove(variableName);
		}
	}

	@Override
	public void removeVariables() {
		variables.clear();
	}

	@Override
	public void removeVariablesLocal() {
		variablesLocal.clear();
	}

	@Override
	public BpmnModelInstance getBpmnModelInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowElement getBpmnModelElementInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProcessEngineServices getProcessEngineServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessInstanceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getEventName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBusinessKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessBusinessKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcessDefinitionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParentId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentActivityId() {
		return "test";
	}

	@Override
	public String getCurrentActivityName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getActivityInstanceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParentActivityInstanceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentTransitionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends TypedValue> T getVariableLocalTyped(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends TypedValue> T getVariableLocalTyped(String arg0,
			boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVariableScopeKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends TypedValue> T getVariableTyped(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends TypedValue> T getVariableTyped(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariableMap getVariablesLocalTyped() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariableMap getVariablesLocalTyped(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariableMap getVariablesTyped() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VariableMap getVariablesTyped(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateExecution getProcessInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DelegateExecution getSuperExecution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getTenantId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariable(String arg0, Object arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ProcessEngine getProcessEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Incident createIncident(String incidentType, String configuration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Incident createIncident(String incidentType, String configuration, String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resolveIncident(String incidentId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProcessBusinessKey(String businessKey) {
		// TODO Auto-generated method stub
		
	}

}
