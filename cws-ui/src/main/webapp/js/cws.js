//parses query string. returns null if empty
function getQueryString(){
    /*
    * PARSE THE QUERY STRING
    */
    var qstring = document.location.search;
    qstring = qstring.substring(1);

    var keyValPair = qstring.split('&');

    if(keyValPair.length == 1 && keyValPair[0] == ""){
        return null;
    }

    var params = {};
    for(entry in keyValPair){
        var key = keyValPair[entry].split("=")[0];
        var val = keyValPair[entry].split("=")[1];
        params[key] = val;
    }
    
    return params;
}