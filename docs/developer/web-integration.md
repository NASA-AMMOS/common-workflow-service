# Web Integration & CORS

CWS supports integration with external web applications through CORS
configuration and experimental Web Components.

## Enabling CORS

By default, the CWS REST API only accepts requests from the console's own
origin. To call the API from other hosts (external dashboards, custom UIs,
single-page apps), enable CORS by adding a filter to the Tomcat `web.xml`.

### For the CWS REST API

Edit:

```
<CWS_ROOT>/server/apache-tomcat-<version>/webapps/cws-ui/WEB-INF/web.xml
```

### For the Camunda REST API

Edit:

```
<CWS_ROOT>/server/apache-tomcat-<version>/webapps/engine-rest/WEB-INF/web.xml
```

### Filter configuration

Add after the last `<listener>` in the XML:

```xml
<filter>
    <filter-name>CorsFilter</filter-name>
    <filter-class>org.apache.catalina.filters.CorsFilter</filter-class>
    <init-param>
        <param-name>cors.allowed.origins</param-name>
        <param-value>https://your-app.example.com</param-value>
    </init-param>
    <init-param>
        <param-name>cors.allowed.methods</param-name>
        <param-value>GET,POST,HEAD,OPTIONS,PUT</param-value>
    </init-param>
    <init-param>
        <param-name>cors.allowed.headers</param-name>
        <param-value>Content-Type,X-Requested-With,accept,Authorization,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,Last-Modified,X-Auth-Token</param-value>
    </init-param>
    <init-param>
        <param-name>cors.exposed.headers</param-name>
        <param-value>Access-Control-Allow-Origin,Access-Control-Allow-Credentials</param-value>
    </init-param>
    <init-param>
        <param-name>cors.support.credentials</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>CorsFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Replace `https://your-app.example.com` with your allowed origins
(comma-separated for multiple).

See the [Tomcat CORS Filter documentation](https://tomcat.apache.org/tomcat-11.0-doc/config/filter.html#CORS_Filter)
for all available options.

## CWS Web Components (Beta)

CWS provides experimental Web Components that you can embed in external web
pages to display CWS data (process status, worker health, etc.) without
building a custom integration from scratch.

!!! warning "Beta"
    Web Components are experimental and subject to change.

See the `project_webapp_root`
[configuration setting](../reference/configuration.md#host-installation) to set
up a project-specific webapp inside the CWS server that is accessible without
CWS security.
