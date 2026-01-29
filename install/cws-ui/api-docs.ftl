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

                $.getJSON('/${base}/v3/api-docs', function(data) {
                    // The OpenAPI 3.x spec is already properly formatted
                    // No need to manipulate paths or security schemes
                    console.log("Loaded OpenAPI spec:", data);
                    
                    // Add custom authentication UI first
                    $('body').append(`
                        <div id="auth-wrapper" style="position: fixed; top: 10px; right: 10px; background: white; padding: 10px; border: 1px solid #ccc; border-radius: 5px; z-index: 9999; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                            <div style="font-weight: bold; margin-bottom: 5px;">CWS Authentication</div>
                            <input type="password" id="cwsTokenInput" placeholder="Enter your cwsToken" style="width: 200px; margin: 5px; padding: 5px;">
                            <br>
                            <button onclick="setCwsToken()" style="margin: 2px; padding: 5px 10px;">Set Token</button>
                            <button onclick="clearCwsToken()" style="margin: 2px; padding: 5px 10px;">Clear</button>
                        </div>
                    `);
                    
                    window.ui = SwaggerUIBundle({
                        spec: data,
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        docExpansion: 'none',
                        tryItOutEnabled: true,
                        requestInterceptor: function(request) {
                            // Add cwsToken as a cookie to all requests
                            if (window.cwsToken && typeof window.cwsToken === 'string') {
                                // Set the cwsToken cookie
                                document.cookie = "cwsToken=" + window.cwsToken + "; path=/";
                                // Also add it to the request headers for curl display
                                request.headers.cwsToken = window.cwsToken;
                            }
                            return request;
                        }
                    });
                    
                    window.setCwsToken = function() {
                        var tokenValue = $('#cwsTokenInput').val().trim();
                        if (tokenValue) {
                            window.cwsToken = tokenValue;
                            $('#auth-wrapper').hide();
                            console.log("CWS Token set:", window.cwsToken);
                        } else {
                            alert("Please enter a valid cwsToken");
                        }
                    };
                    
                    window.clearCwsToken = function() {
                        window.cwsToken = null;
                        $('#cwsTokenInput').val('');
                        $('#auth-wrapper').show();
                        console.log("CWS Token cleared");
                    };
                });
            };
        </script>
    </body>
</html>