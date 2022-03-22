
package jpl.cws.core.code;

import java.util.*;
import java.util.regex.*;
import java.io.*;

//-----------------------------------------------------------------------------
// This class provides a place to define custom methods.
//  Out of the box, the CwsCodeBase superclass provides access to the CWS
//  installation hostname and port via variables:
//    ${cws.hostname}
//    ${cws.port}
//
//  Also, provided by the superclass are these methods:
//    String getEnv(String envVar)
//
//  Example of calling a snippet from a BPMN model:
//    ${cws.getEnv("JAVA_HOME")}
//
//-----------------------------------------------------------------------------
public class CustomMethods extends CwsCodeBase {
	public static final String AWS_REGION = "us-gov-west-1";
	

	public String heyYou() {
		return "hey you!";
	}
 
	public String basename(String filename) {
        int lastSlashIdx = filename.lastIndexOf("/");
        if (lastSlashIdx != -1) {
            filename = filename.substring(lastSlashIdx + 1);
    	}
                
        // everything except for extension
		return filename.substring(0,filename.lastIndexOf("."));
	}

	public String getRandUuid() {
		return UUID.randomUUID().toString();
	}
}


