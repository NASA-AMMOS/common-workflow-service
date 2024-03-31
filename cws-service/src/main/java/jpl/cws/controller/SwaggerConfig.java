package jpl.cws.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
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

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("CWS API")
                .description("Documentation of the endpoints used by CWS. Once authenticated, requests can be made to these endpoints (ensure that you are sending the cookie that results from the authentication call!")
                .version("2.6.0")
                .license("Apache 2.0")
                .licenseUrl("https://github.com/NASA-AMMOS/common-workflow-service?tab=Apache-2.0-1-ov-file")
                .build();
    }

    @Bean
    public UiConfiguration uiConfiguration() {
        return UiConfigurationBuilder
                .builder()
                .defaultModelExpandDepth(-1)
                .build();
    }
}
