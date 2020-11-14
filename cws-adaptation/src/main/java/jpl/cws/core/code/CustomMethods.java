package jpl.cws.core.code;

//-----------------------------------------------------------------------------
// This class provides a place to define custom methods.
//  Out of the box, the CwsCodeBase superclass provides access to the CWS
//  installation hostname and port via variables:
//    ${cws.hostname}
//    ${cws.port}
//
//  Also, provided by the superclass are these methods:
//    String getEnv(String envVar)
//    String getRandUuid()
//
//  Example of calling a snippet from a BPMN model:
//    ${cws.getEnv(\"JAVA_HOME\")}
//
//-----------------------------------------------------------------------------
public class CustomMethods extends CwsCodeBase {

	public String echo(String arg1) {
		return arg1;
	}

	public String hello() {
		return "hello world!";
	}

}