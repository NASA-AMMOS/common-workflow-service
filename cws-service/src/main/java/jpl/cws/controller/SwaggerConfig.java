package jpl.cws.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

@Configuration
public class SwaggerConfig {

    @Value("${cws.version}")
    private String cwsVersion;

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("CWS API")
                        .description("Documentation of the endpoints used by CWS. Once authenticated, requests can be made to these endpoints.\nTo authenticate, right click on this page --> Inspect --> Click the 'Application' tab --> Select the URL under the Cookies tab on the left --> Copy the value of the cwsToken cookie.")
                        .version(cwsVersion)
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://github.com/NASA-AMMOS/common-workflow-service?tab=Apache-2.0-1-ov-file")))
                .addSecurityItem(new SecurityRequirement().addList("cwsToken"))
                .components(new Components()
                        .addSecuritySchemes("cwsToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("cwsToken")));
    }
}
