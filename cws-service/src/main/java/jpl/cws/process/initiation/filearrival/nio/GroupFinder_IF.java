package jpl.cws.process.initiation.filearrival.nio;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author awt
 *
 */
public interface GroupFinder_IF {

	/**
	 * Initializes the finder with context information
	 * @param context
	 */
	public void initialize(Map<String,Object> context);

	/**
	 * Retrieves the input partners for the specified
	 * input file.  If all partners are found, then the returned
	 * list will contain all partners (including supplied file).
	 * Otherwise, this will return null.
	 *
	 * @param filename
	 * @param localCacheDir
	 * @param globalCacheDir
	 * @return List of File objects that make up the entire set
	 * of input partners.  This list includes the specified file.
	 */
	public List<String> getInputPartners(String triggerFileName, String cacheDir, String unmatchedDir) throws IOException;
}