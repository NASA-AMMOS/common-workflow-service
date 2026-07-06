package jpl.cws.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.BooleanSchema;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/v3")
public class OpenApiController {

    @Autowired
    private ApplicationContext applicationContext;

    @org.springframework.beans.factory.annotation.Value("${cws.version}")
    private String cwsVersion;

    @GetMapping(value = "/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getApiDocs() {
        try {
            OpenAPI openAPI = createOpenApiSpec();
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            String json = mapper.writeValueAsString(openAPI);
            
            // Temporarily disable JSON cleaning to test security schemes
            // json = json.replace("\"$ref\":null,", "");
            // json = json.replace("\"$ref\":null", "");
            // json = json.replaceAll(",\\s*}", "}"); // Remove trailing commas
            // json = json.replaceAll(",\\s*]", "]"); // Remove trailing commas in arrays
            
            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to generate API docs\"}";
        }
    }

    private OpenAPI createOpenApiSpec() {
        OpenAPI openAPI = new OpenAPI()
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
                                .name("cwsToken")
                                .description("CWS authentication token")));

        // Generate paths from RestService controller
        Paths paths = generatePathsFromRestService();
        openAPI.setPaths(paths);

        return openAPI;
    }

    private Paths generatePathsFromRestService() {
        Paths paths = new Paths();
        
        try {
            // Try to get RestService controller
            try {
                RestService restService = applicationContext.getBean(RestService.class);
                Class<?> restServiceClass = restService.getClass();
                
                // Get all methods with @RequestMapping
                Method[] methods = restServiceClass.getDeclaredMethods();
                
                for (Method method : methods) {
                    if (method.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class)) {
                        org.springframework.web.bind.annotation.RequestMapping requestMapping = 
                            method.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
                        
                        // Get the class-level @RequestMapping
                        org.springframework.web.bind.annotation.RequestMapping classMapping = 
                            restServiceClass.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
                        
                        // Use the correct base path for CWS UI
                        String classPath = "/cws-ui/rest";  // Override the class mapping to use correct path
                        String methodPath = requestMapping.value()[0];
                        String fullPath = classPath + methodPath;
                        
                        // Create path item
                        PathItem pathItem = new PathItem();
                        
                        // Determine HTTP method
                        org.springframework.web.bind.annotation.RequestMethod[] httpMethods = requestMapping.method();
                        if (httpMethods.length == 0) {
                            httpMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.GET};
                        }
                        
                        // Create operation
                        Operation operation = new Operation();
                        
                        // Add summary from @Operation annotation if present
                        if (method.isAnnotationPresent(io.swagger.v3.oas.annotations.Operation.class)) {
                            io.swagger.v3.oas.annotations.Operation operationAnnotation = 
                                method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
                            operation.setSummary(operationAnnotation.summary());
                        } else {
                            operation.setSummary("REST endpoint: " + method.getName());
                        }
                        
                        // Extract parameters from method
                        List<Parameter> parameters = extractParameters(method);
                        if (!parameters.isEmpty()) {
                            operation.setParameters(parameters);
                        }
                        
                        // Add responses
                        ApiResponses methodResponses = new ApiResponses();
                        ApiResponse methodSuccessResponse = new ApiResponse()
                            .description("Successful response")
                            .content(new Content()
                                .addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType()
                                    .schema(new StringSchema())));
                        methodResponses.addApiResponse("200", methodSuccessResponse);
                        operation.setResponses(methodResponses);
                        
                        // Add operation to appropriate HTTP method
                        for (org.springframework.web.bind.annotation.RequestMethod httpMethod : httpMethods) {
                            switch (httpMethod) {
                                case GET:
                                    pathItem.setGet(operation);
                                    break;
                                case POST:
                                    pathItem.setPost(operation);
                                    break;
                                case PUT:
                                    pathItem.setPut(operation);
                                    break;
                                case DELETE:
                                    pathItem.setDelete(operation);
                                    break;
                            }
                        }
                        
                        paths.addPathItem(fullPath, pathItem);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error discovering RestService endpoints: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Error generating paths: " + e.getMessage());
            e.printStackTrace();
        }
        
        return paths;
    }
    
    private List<Parameter> extractParameters(Method method) {
        List<Parameter> parameters = new ArrayList<>();
        
        try {
            java.lang.reflect.Parameter[] methodParams = method.getParameters();
            
            for (java.lang.reflect.Parameter param : methodParams) {
                // Skip HttpServletRequest, HttpServletResponse, HttpSession, etc.
                if (isHttpParameter(param.getType())) {
                    continue;
                }
                
                Parameter openApiParam = new Parameter();
                
                // Check for @RequestParam annotation
                if (param.isAnnotationPresent(org.springframework.web.bind.annotation.RequestParam.class)) {
                    org.springframework.web.bind.annotation.RequestParam requestParam = 
                        param.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
                    
                    openApiParam.setName(requestParam.value().isEmpty() ? param.getName() : requestParam.value());
                    openApiParam.setIn("query");
                    openApiParam.setRequired(requestParam.required());
                    openApiParam.setSchema(new StringSchema());
                    
                    if (!requestParam.defaultValue().equals(org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE)) {
                        openApiParam.setDescription("Default: " + requestParam.defaultValue());
                    }
                }
                // Check for @PathVariable annotation
                else if (param.isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class)) {
                    org.springframework.web.bind.annotation.PathVariable pathVariable = 
                        param.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                    
                    openApiParam.setName(pathVariable.value().isEmpty() ? param.getName() : pathVariable.value());
                    openApiParam.setIn("path");
                    openApiParam.setRequired(true);
                    openApiParam.setSchema(new StringSchema());
                }
                // Check for @RequestBody annotation
                else if (param.isAnnotationPresent(org.springframework.web.bind.annotation.RequestBody.class)) {
                    // RequestBody parameters are handled as request body, not as parameters
                    continue;
                }
                // Default to query parameter for other types
                else {
                    openApiParam.setName(param.getName());
                    openApiParam.setIn("query");
                    openApiParam.setRequired(false);
                    openApiParam.setSchema(new StringSchema());
                }
                
                parameters.add(openApiParam);
            }
        } catch (Exception e) {
            System.err.println("Error extracting parameters from method " + method.getName() + ": " + e.getMessage());
        }
        
        return parameters;
    }
    
    private boolean isHttpParameter(Class<?> paramType) {
        return paramType == jakarta.servlet.http.HttpServletRequest.class ||
               paramType == jakarta.servlet.http.HttpServletResponse.class ||
               paramType == jakarta.servlet.http.HttpSession.class ||
               paramType == org.springframework.web.context.request.WebRequest.class ||
               paramType == org.springframework.ui.Model.class ||
               paramType == org.springframework.ui.ModelMap.class;
    }
}
