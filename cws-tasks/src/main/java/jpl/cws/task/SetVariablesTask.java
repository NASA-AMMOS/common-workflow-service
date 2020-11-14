package jpl.cws.task;

import java.util.Iterator;

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.camunda.bpm.engine.delegate.Expression;

/**
 * Built-in task that reads from a properties file, setting them as process variables
 * 
 * REQUIRED parameter:  srcPropertiesFile
 * 
 * REFERENCE: http://commons.apache.org/proper/commons-configuration/apidocs/org/apache/commons/configuration/HierarchicalINIConfiguration.html
 */
public class SetVariablesTask extends CwsTask {

	private Expression srcPropertiesFile;
	private String srcPropertiesFileString;

	public SetVariablesTask() {
		log.trace("SetVariablesTask constructor...");
	}

	@Override
	public void initParams() throws Exception {
		srcPropertiesFileString = getStringParam(srcPropertiesFile, "srcPropertiesFile");
	}

	@Override
	public void executeTask() throws Exception {
		log.info("SetVariablesTask (" + srcPropertiesFileString + ")");
		
		HierarchicalINIConfiguration fc = new HierarchicalINIConfiguration();
		
		fc.setDelimiterParsingDisabled(true);		// Do not convert any values into an .ini List
		fc.load(srcPropertiesFileString);
		
		for (String sectionName : fc.getSections()) {
			Iterator<String> keysIter = fc.getSection(sectionName).getKeys();
			
			while (keysIter.hasNext()) {
				String key = keysIter.next();
				String keyToSet = (sectionName==null ? "" : sectionName+"_")+key;
				
				keyToSet = keyToSet.replace("..",  ".");	// Fixes bug in library that causes variables with one dot to be converted to dotdot
				
				String valToSet = fc.getProperty((sectionName==null?"":sectionName)+"."+key).toString();
				log.info("SETTING VARIABLE: "+keyToSet+" = "+valToSet);
				this.setOutputVariableActualName(keyToSet, valToSet);
			}
		}
		
		log.info("SetVariablesTask operation complete.");
	}
	
	public Expression getSrcPropertiesFile() {
		return srcPropertiesFile;
	}

	public void setSrcPropertiesFile(Expression srcPropertiesFile) {
		this.srcPropertiesFile = srcPropertiesFile;
	}

}
