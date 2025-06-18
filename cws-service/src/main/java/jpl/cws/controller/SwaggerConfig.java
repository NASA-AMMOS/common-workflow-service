package jpl.cws.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@EnableWebMvc
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .pathMapping("/rest/");
    }

    // add apikey to swagger auth


    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("CWS API")
                .description("Documentation of the endpoints used by CWS. Once authenticated, requests can be made to these endpoints.\nTo authenticate, right click on this page --> Inspect --> Click the 'Application' tab --> Select the URL under the Cookies tab on the left --> Copy the value of the cwsToken cookie.")
                .version("2.7.0")   // update this each CWS release
                .license("Apache 2.0")
                .licenseUrl("https://github.com/NASA-AMMOS/common-workflow-service?tab=Apache-2.0-1-ov-file")
                .build();
    }
}
