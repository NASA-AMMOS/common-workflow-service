//
// Redirect Camunda login to automatically logout of CWS and display the CWS login page
//
define([], function() {
    window.addEventListener("hashchange", function(event) {
        if (event.newURL.includes("#/login")) {
            location.href = "/cws-ui/logout"; // Replace with your own URL to redirect to
        }
    });
});
