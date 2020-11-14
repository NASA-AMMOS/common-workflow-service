package jpl.cws.task;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;


public class RawCamundaDelegateTask implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		System.out.println(Thread.currentThread() + " -- RawCamundaDelegateTask execute()...");
		execution.setVariable(Thread.currentThread().toString(), Thread.currentThread().toString());
	}

}
