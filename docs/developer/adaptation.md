# Adapting CWS for a Mission

CWS can be adapted and tailored for a specific mission or project **without
forking the core codebase**. The `cws-adaptation` module is the designated
extension point.

## What you can add

| Extension | Location |
| --- | --- |
| Custom Java code | `cws-adaptation/src/main/java/` |
| Custom REST API endpoints | Same package, using Spring `@Controller`/`@RestController` |
| Custom process initiators | Package `jpl.cws.process.initiation.custom` |
| An external database | SQL templates in `install/sql/` + Java extending `DbService` |
| Custom UI elements | Adaptation-specific FTL templates and JavaScript |

## Getting started

1. Ensure you have a local build environment set up (see
   [Building from Source](../install/building.md)).
2. Work within the `cws-adaptation` module — it depends on `cws-service` and
   `cws-core` and is packaged into the console WAR.
3. Build your adaptation: `cd cws-adaptation && mvn clean package`.
4. Deploy the resulting `cws-adaptation.jar` into the console's
   `WEB-INF/lib/`.

## Adaptation workers modal

Customize the Deployments page workers view via the JavaScript function
`addAdaptationWorkersInfo` in
`cws-ui/src/main/webapp/js/adaptation-workers-modal.js`:

```javascript
function addAdaptationWorkersInfo(dataProcKey, listWorkers) {
    // Your custom logic here
    return;
}
```

## Further reading

- [External Database](external-database.md) — connect to a separate schema.
- [Custom Tasks](custom-tasks.md) — add new task types.
- [Custom Initiators](custom-initiators.md) — create triggers.
- [Security Plugins](security-plugin.md) — custom authentication schemes.
