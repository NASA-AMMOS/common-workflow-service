# Developing Custom Initiators

CWS supports custom process initiators — both **internal** (runs inside the
CWS JVM) and **external** (runs outside CWS, calls the REST API).

## Internal initiator development

An internal initiator extends `CwsProcessInitiator` and runs within the CWS
console process.

### Steps

1. Navigate to `cws-adaptation/src/main/java/` and the package
   `jpl.cws.process.initiation.custom`.

2. Create a class extending `CwsProcessInitiator`:

    ```java
    public class MyInitiator extends CwsProcessInitiator {

        private int delayMs;

        public MyInitiator(String procDefKey, int delayMs) {
            super(procDefKey);
            this.delayMs = delayMs;
        }

        @Override
        public void run() {
            while (isActive()) {
                // Check your trigger condition
                if (shouldStart()) {
                    scheduleProcess(
                        procVariables,    // Map<String,Object>
                        null,             // procBusinessKey (null = auto)
                        "my-trigger-key"  // initiationKey
                    );
                }
                Thread.sleep(delayMs);
            }
        }

        @Override
        public ConstructorArgumentValues getConstructorArgumentValues() {
            ConstructorArgumentValues args = new ConstructorArgumentValues();
            args.addGenericArgumentValue(getProcDefKey());
            args.addGenericArgumentValue(delayMs);
            return args;
        }

        @Override
        public boolean isValid() {
            return delayMs > 0;
        }
    }
    ```

3. Build and deploy:

    ```bash
    cd cws-adaptation
    mvn clean package
    ```

    Copy the resulting `cws-adaptation.jar` to the console's `WEB-INF/lib/`.

4. Register your initiator in `cws-process-initiators.xml` (under
   `cws-ui/src/main/resources/`) so it appears in the console's Initiators page.

### Key points

- Always check `isActive()` in your loop — this allows CWS to stop the
  initiator cleanly.
- Use `Thread.sleep()` in loops to avoid busy-waiting.
- Call `scheduleProcess()` when your trigger condition is met.

## External initiator development

An external initiator is any program that calls the CWS
[REST API](../user-guide/launching/rest.md) to schedule processes.

```bash
curl -k -X POST \
  "https://<cws-host>:38443/cws-ui/rest/process/<procDefKey>/schedule" \
  -H "cwsToken: <token>" \
  --data "variable1=value1"
```

External initiators can be written in any language and run anywhere with
network access to the CWS console.

## Choosing internal vs. external

See [Internal & External Initiators](../user-guide/initiators/internal-external.md)
for a comparison.
