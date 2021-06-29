package jpl.cws.engine.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;

import jpl.cws.core.db.SchedulerDbService;
import jpl.cws.engine.WorkerService;

public class ProcessStartReqProcessor implements Runnable {
	
	private Logger log;
	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private SchedulerDbService schedulerDbService;
	private WorkerService workerService;
	private List<Map<String,Object>> procReqData;
	private ExecutorService processorPool;
	private String limitToProcDefKey;

	public ProcessStartReqProcessor(
			ExecutorService processorPool,
			RepositoryService repositoryService,
			RuntimeService runtimeService,
			SchedulerDbService schedulerDbService,
			WorkerService workerService,
			Logger log,
			String limitToProcDefKey) {
		
		this.processorPool = processorPool;
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.schedulerDbService = schedulerDbService;
		this.workerService = workerService;
		this.log = log;
		this.limitToProcDefKey = limitToProcDefKey;
	}


	@Override
	public void run() {
		// FIXME: remove this check once confident
		if (WorkerService.processorExecuteCount.decrementAndGet() < 0) { 
			log.error("unexpected counter value! " + WorkerService.processorExecuteCount.get());
		}
		
		log.trace("ProcessStartReqProcessor run()...");
		try {
			// Sleep for a random amount (up to a quarter second).
			// This is to avoid all workers hitting the database at the exact same time
			// when a process request (topic) comes in.
			//
			Thread.sleep((long)(Math.random() * 250.0));
			
			// Attempt to claim a start request
			//
			procReqData = workerService.claimWithCounter(limitToProcDefKey);

			if (procReqData == null || procReqData.isEmpty()) {
				log.trace("didn't claim any rows");
				return; // did not claim anything
			}
			
			// We have claimed some rows, so start the processes for these
			//
			attemptProcessStarts();
		}
		catch (Throwable t) {
			log.error("Unexpected Failure.  NOT putting request message back on the queue.", t);
		}
	}


	/**
	 * 
	 */
	private void attemptProcessStarts() {
		for (Map<String,Object> procReq : procReqData) {
			//workerService.registerProcStartReqUuid(procReq.get("uuid").toString());
			processorPool.execute(
				new ProcessStarterThread(repositoryService, runtimeService, schedulerDbService, workerService, procReq, log)
			);
		}
	}

}
