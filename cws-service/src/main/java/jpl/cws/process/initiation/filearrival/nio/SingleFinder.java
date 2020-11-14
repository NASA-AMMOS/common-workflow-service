package jpl.cws.process.initiation.filearrival.nio;


import java.util.List;
import java.util.Map;
import java.util.Vector;


/**
 *
 * @author awt, ghollins
 *
 */
public class SingleFinder implements GroupFinder_IF {

	public void initialize(Map<String,Object> context) {
		// no-op
	}

	/* (non-Javadoc)
	 * 
	 */
	public List<String> getInputPartners(String filename, String localCacheDir, String globalCacheDir) {
		//
		// Return an empty Vector (not null), signifying that all partners were found.
		// In this case there are no partners, since there is only the single file.
		return new Vector<String>();
	}
}