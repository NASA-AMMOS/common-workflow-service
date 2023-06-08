/**
* TABLE SORT
**/

$("th.sort").click(function(){
    var table = $(this).parents('table').eq(0)
    var rows = table.find('tr:gt(0)').toArray().sort(comparer($(this).index()))
    this.asc = !this.asc
    if (!this.asc){
        rows = rows.reverse();
    }
    for (var i = 0; i < rows.length; i++){table.append(rows[i])}
})
function comparer(index) {
    return function(a, b) {
        var valA = getCellValue(a, index), valB = getCellValue(b, index)
        return $.isNumeric(valA) && $.isNumeric(valB) ? valA - valB : valA.localeCompare(valB)
    }
}
function getCellValue(row, index){ return $(row).children('td').eq(index).html() }


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

//function to check if a cookie exists and if it does, return the value
//if it doesn't, return an empty string
function getCookieValue(cookieName){
    let name = cookieName + "=";
    let decodedCookies = decodeURIComponent(document.cookie);
    let cookies = decodedCookies.split(';');
    for(let i = 0; i < cookies.length; i++){
        let cookie = cookies[i];
        while(cookie.charAt(0) == ' '){
            cookie = cookie.substring(1);
        }
        if(cookie.indexOf(name) == 0){
            return cookie.substring(name.length, cookie.length);
        }
    }
    return "";
}

//function to handle creating / updating a cookie
function updateCookie(cookieName, cookieValue, expireDays){
    let d = new Date();
    d.setTime(d.getTime() + expireDays*24*60*60*1000);
    let expireString = "expires=" + d.toUTCString();
    document.cookie = cookieName + "=" + cookieValue + ";" + expireString + ";path=/";
}