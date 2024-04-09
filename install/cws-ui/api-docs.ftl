<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="description" content="SwaggerUI" />
        <title>SwaggerUI</title>
        <link href="/${base}/css/swagger-ui.css" rel="stylesheet">
        <script src="/${base}/js/jquery.min.js"></script>
    </head>
    <body>
        <div id="swagger-ui"></div>
        <script src="/${base}/js/swagger-ui-bundle.js"></script>
        <script>
            window.onload = () => {

                $.getJSON('/${base}/v2/api-docs', function(data) {
                    //under the "paths" property, we have all the endpoints.
                    //loop through and change anywhere we find "/rest/cws-ui/api/" to "/cws-ui/rest/"
                    //also, add the securityDefinitions property
                    for (var path in data.paths) {
                        var newPath = path.replace("/rest/cws-ui/api/", "/cws-ui/rest/");
                        data.paths[newPath] = data.paths[path];
                        delete data.paths[path];
                    }
                    data["securityDefinitions"] = {
                        "cwsToken": {
                            "type": "apiKey",
                            "name": "cwsToken",
                            "in": "header"
                        }
                    };
                    data["security"] = [
                        {
                            "cwsToken": []
                        }
                    ];
                    delete data.tags;
                    window.ui = SwaggerUIBundle({
                        spec: data,
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        docExpansion: 'none',
                    });
                });
            };
        </script>
    </body>
</html>