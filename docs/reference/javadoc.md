# Javadoc

Aggregated Java API documentation for the CWS modules (`cws-core`,
`cws-tasks`, `cws-service`, and others) is generated from source and published
alongside this site.

<p><a class="md-button md-button--primary" href="../javadoc/index.html">Open the CWS Javadoc</a></p>

The Javadoc is most useful when [writing custom tasks](../developer/custom-tasks.md),
[custom initiators](../developer/custom-initiators.md), or otherwise
[adapting CWS for a mission](../developer/adaptation.md), where you extend CWS
base classes and call into its APIs directly.

!!! note "Availability"
    The Javadoc is generated during the documentation build. If the link above
    does not resolve for a particular version, that version was published
    without the API docs (for example, a build where the Java toolchain was
    unavailable).

## Generating Javadoc locally

From the repository root, with a JDK 17 toolchain and Maven configured to
resolve the project's dependencies:

```bash
mvn -P core,docs -DskipTests javadoc:aggregate
```

The aggregated output is written to `target/site/apidocs/`. Open
`target/site/apidocs/index.html` in a browser.
