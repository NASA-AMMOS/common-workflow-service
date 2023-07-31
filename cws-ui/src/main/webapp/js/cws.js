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

//parses query string. returns null if empty
function parseQueryString(qstring){
    /*
    * PARSE THE QUERY STRING
    */
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

//Thank you to sobyte.net for the following function:
function base64ToBlob(base64Data, mime) {
    mime = mime || "image/png";
    var sliceSize = 512;
    var byteChars = atob(base64Data);
    var byteArrays = [];
    for (var offset = 0; offset < byteChars.length; offset += sliceSize) {
        var slice = byteChars.slice(offset, offset + sliceSize);
        let byteNumbers = new Array(slice.length);
        for (var i = 0; i < slice.length; i++) {
            byteNumbers[i] = slice.charCodeAt(i);
        }
        var byteArray = new Uint8Array(byteNumbers);
        byteArrays.push(byteArray);
    }
    return new Blob(byteArrays, {type: mime});
}

function copyInput(varValue, isImage) {
    if (isImage == "true") {
        var sanitizedB64 = varValue;
        //if it exists, replace everything between data: and "base64, " with nothing
        if (sanitizedB64.indexOf("data:") !== -1) {
            sanitizedB64 = sanitizedB64.replace(/data:.*base64, /g, "");
        }
        const item = new ClipboardItem({
            "image/png": base64ToBlob(sanitizedB64, "image/png")
        });
        navigator.clipboard.write([item]);
    }
    navigator.clipboard.writeText(varValue);
}

function checkForURL(potentialURL) {
    if (potentialURL === undefined || potentialURL === null || potentialURL === "") {
        return false;
    } else if (potentialURL.startsWith("www.") || potentialURL.startsWith("http://") || potentialURL.startsWith("https://") || potentialURL.startsWith("s3://")) {
        return true;
    }
    try {
        new URL(potentialURL);
        return true;
    }
    catch (e) {
        return false;
    }
}

function isEqual (a, b) {
    for (const key in a) {
        if (b[key] !== undefined) {
            if (a[key] !== b[key]) {
                return false;
            }
        } else {
            return false;
        }
    }
    return true;
}

function autocomplete(inp, arr) {
    var currentFocus;
    inp.addEventListener("input", function(e) {
        var a, b, i, val = this.value;
        closeAllLists();
        if (!val || val.length < 2) { return false;}
        currentFocus = -1;
        a = document.createElement("DIV");
        a.setAttribute("id", this.id + "autocomplete-list");
        a.setAttribute("class", "autocomplete-items autocomplete-items-cws");
        this.parentNode.appendChild(a);
        for (i = 0; i < arr.length; i++) {
            if (arr[i].substr(0, val.length).toUpperCase() === val.toUpperCase()) {
                b = document.createElement("DIV");
                b.innerHTML = "<strong>" + arr[i].substr(0, val.length) + "</strong>";
                b.innerHTML += arr[i].substr(val.length);
                b.innerHTML += "<input type='hidden' value='" + arr[i] + "'>";
                b.addEventListener("click", function(e) {
                    inp.value = this.getElementsByTagName("input")[0].value;
                    closeAllLists();
                });
                a.appendChild(b);
            }
        }
    });
    inp.addEventListener("keydown", function(e) {
        var x = document.getElementById(this.id + "autocomplete-list");
        if (x) x = x.getElementsByTagName("div");
        if (e.keyCode === 40) {
            currentFocus++;
            addActive(x);
        } else if (e.keyCode === 38) {
            currentFocus--;
            addActive(x);
        } else if (e.keyCode === 13) {
            e.preventDefault();
            if (currentFocus > -1) {
                if (x) x[currentFocus].click();
            }
        }
    });
}

function addActive(x) {
    if (!x) return false;
    removeActive(x);
    if (currentFocus >= x.length) currentFocus = 0;
    if (currentFocus < 0) currentFocus = (x.length - 1);
    x[currentFocus].classList.add("autocomplete-active");
}

function removeActive(x) {
    for (var i = 0; i < x.length; i++) {
        x[i].classList.remove("autocomplete-active");
    }
}

function closeAllLists(elmnt, inp) {
    var x = document.getElementsByClassName("autocomplete-items-cws");
    for (var i = 0; i < x.length; i++) {
        if (elmnt !== x[i]) {
            x[i].parentNode.removeChild(x[i]);
        }
    }
}