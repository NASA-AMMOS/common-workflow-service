# Script Task Recipes

Script Tasks in CWS use either **Groovy** or **JavaScript** (Nashorn engine). These recipes cover the most common scripting needs. All examples use Groovy unless noted.

## Setting a Variable

```groovy
execution.setVariable("myVar", "someValue")
```

Set a numeric or boolean:

```groovy
execution.setVariable("count", 42)
execution.setVariable("isReady", true)
```

Set a variable from an expression result:

```groovy
def path = execution.getVariable("inputDir") + "/" + execution.getVariable("filename")
execution.setVariable("fullPath", path)
```

## Reading a Variable

```groovy
def inputFile = execution.getVariable("inputFile")
```

Read with a default value if the variable might not exist:

```groovy
def retries = execution.getVariable("retryCount") ?: 0
execution.setVariable("retryCount", retries + 1)
```

## Posting a Log Message

CWS log messages from Script Tasks are written through the SLF4J logger that Camunda injects:

```groovy
execution.setVariable("logMessage", "Processing file: " + execution.getVariable("inputFile"))
```

Alternatively, use a Log Task (service task with `camunda:class="jpl.cws.task.LogTask"`) for structured log output — this is the preferred approach when logging is the sole purpose of the step.

For debug output within a Script Task, write to standard output (visible in worker logs):

```groovy
println "DEBUG: inputFile = " + execution.getVariable("inputFile")
```

## Parsing JSON from a File

```groovy
import groovy.json.JsonSlurper

def filePath = execution.getVariable("jsonFilePath")
def jsonText = new File(filePath).text
def data = new JsonSlurper().parseText(jsonText)

execution.setVariable("recordCount", data.records.size())
execution.setVariable("firstRecord", data.records[0].id)
```

For a JSON file with this structure:

```json
{
  "records": [
    {"id": "rec001", "status": "ready"},
    {"id": "rec002", "status": "pending"}
  ]
}
```

## Parsing JSON from a REST Response

When following a REST Task, the response body is available as a process variable (configured in the REST Task's output variable field, typically `responseBody`):

```groovy
import groovy.json.JsonSlurper

def responseBody = execution.getVariable("responseBody")
def data = new JsonSlurper().parseText(responseBody)

execution.setVariable("statusCode", data.status)
execution.setVariable("resultId", data.result.id)
```

Check the HTTP status before parsing:

```groovy
import groovy.json.JsonSlurper

def statusCode = execution.getVariable("httpStatusCode") as Integer
if (statusCode == 200) {
    def body = new JsonSlurper().parseText(execution.getVariable("responseBody"))
    execution.setVariable("result", body.data)
} else {
    execution.setVariable("result", null)
    throw new RuntimeException("REST call failed with status: " + statusCode)
}
```

## Building a JSON Payload

```groovy
import groovy.json.JsonOutput

def payload = [
    jobId: execution.getVariable("jobId"),
    inputFile: execution.getVariable("inputFile"),
    timestamp: new Date().toInstant().toString()
]

execution.setVariable("requestBody", JsonOutput.toJson(payload))
```

## Constructing a File Path

```groovy
def baseDir = execution.getVariable("outputDir")
def filename = execution.getVariable("productName") + "_" + 
               new Date().format("yyyyMMdd") + ".dat"

execution.setVariable("outputPath", baseDir + "/" + filename)
```

## Working with Lists

Collect values into a list across multiple tasks:

```groovy
// Initialize (typically in first script task)
def results = []
execution.setVariable("results", results)
```

Append to the list in a later task:

```groovy
def results = execution.getVariable("results")
results.add(execution.getVariable("latestResult"))
execution.setVariable("results", results)
```

Iterate over a list variable:

```groovy
def fileList = execution.getVariable("fileList")
fileList.each { file ->
    println "File: " + file
}
```

## Checking if a Variable Exists

```groovy
def val = execution.getVariableLocal("optionalVar")
if (val == null) {
    execution.setVariable("optionalVar", "default")
}
```

## JavaScript (Nashorn) Snippets

For teams that prefer JavaScript, Camunda's Nashorn engine supports ECMAScript 5.1:

```javascript
// Set a variable
execution.setVariable("myVar", "value");

// Read a variable
var inputFile = execution.getVariable("inputFile");

// Increment a counter
var count = execution.getVariable("count");
execution.setVariable("count", count + 1);

// Check condition and set result
var status = execution.getVariable("status");
execution.setVariable("isSuccess", status === "OK");
```

!!! note
    The Nashorn JavaScript engine (included with Java) is deprecated as of Java 15 and removed in Java 17. Use Groovy for new script tasks in CWS deployments running on Java 17.

## CWS Code Snippets (CustomMethods)

For logic that needs to be reusable across multiple process definitions, use the CWS Snippets mechanism instead of Script Tasks. Snippets are Java methods in the `CustomMethods` class, editable from the CWS web console under **Snippets**.

Call a snippet in any BPMN expression:

```
${cws.methodName(arg1, arg2)}
```

Example — echo a value:

```java
package jpl.cws.core.code;

public class CustomMethods {
    public String echo(String arg1) {
        return arg1;
    }
}
```

Example — generate a UUID:

```java
package jpl.cws.core.code;
import java.util.UUID;

public class CustomMethods {
    public String getRandUuid() {
        return UUID.randomUUID().toString();
    }
}
```

Use snippets in a sequence flow condition:

```
${cws.isDataReady() == "true"}
```

Snippets update dynamically — no process redeployment needed when you modify the snippet.

## Further reading

- [Camunda 7.24 Script Tasks](https://docs.camunda.org/manual/7.24/user-guide/process-engine/scripting/)
- [Camunda 7.24 Expression Language](https://docs.camunda.org/manual/7.24/user-guide/process-engine/expression-language/)