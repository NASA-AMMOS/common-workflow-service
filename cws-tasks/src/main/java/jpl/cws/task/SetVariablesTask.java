package jpl.cws.task;

import java.util.Iterator;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import java.io.File;
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
		
		try {
			FileBasedConfigurationBuilder<INIConfiguration> builder = 
				new FileBasedConfigurationBuilder<INIConfiguration>(INIConfiguration.class)
					.configure(new Parameters().fileBased().setFile(new File(srcPropertiesFileString)));
			
			INIConfiguration fc = builder.getConfiguration();
			
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
		} catch (ConfigurationException e) {
			log.error("Error loading configuration file: " + srcPropertiesFileString, e);
			throw new Exception("Failed to load configuration file: " + srcPropertiesFileString, e);
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
