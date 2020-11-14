package jpl.cws.task;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that moves a file from one location on the file system to
 * another.
 * 
 * REQUIRED parameters: -- srcFile -- destFile
 * 
 */
public class MoveFile extends CwsTask {

	private Expression srcFile;
	private Expression destFile;
	private String srcFileString;
	private String destFileString;

	public MoveFile() {
		log.trace("MoveFile constructor...");
	}

	@Override
	public void initParams() throws Exception {
		srcFileString = getStringParam(srcFile, "srcFile");
		destFileString = getStringParam(destFile, "destFile");
	}

	@Override
	public void executeTask() throws Exception {
		log.info("in impl...");

		// Perform move
		log.info("MoveFile (" + srcFileString + " --> " + destFileString + ")");

		File srcFile = new File(srcFileString);
		File destFile = new File(destFileString);

		if (!srcFile.exists()) {
			throw new BpmnError("MoveFileInvalidSourceFile");
		}

		if (!destFile.getParentFile().exists()) {
			throw new BpmnError("MoveFileInvalidDestinationDirectory");
		}

		log.info("found src and dest files.  About to perform move operation...");
		try {
			FileUtils.moveFile(srcFile, destFile);
		} catch (Exception e) {
			throw new BpmnError(e.getMessage());
		}

		log.info("Move operation complete.");

		this.setOutputVariable("srcFile", srcFileString);
		this.setOutputVariable("destFile", destFileString);
	}
	
	public Expression getSrcFile() {
		return srcFile;
	}

	public void setSrcFile(Expression srcFile) {
		this.srcFile = srcFile;
	}

	public Expression getDestFile() {
		return destFile;
	}

	public void setDestFile(Expression destFile) {
		this.destFile = destFile;
	}

}
