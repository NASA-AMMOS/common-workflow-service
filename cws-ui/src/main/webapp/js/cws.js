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

function getInstanceCSV(procInstId) {
    var outputCSV = "";
    var logLines = [];
    var scrollId = "";
    var proc_info = {};
    var baseEsReq = {
        "from": 0,
        "size": 20,
        "query": { 
            "bool": {
                "must" :[]
            }
        },
        "sort": { "@timestamp": { "order": "asc" } }
    };
    baseEsReq.query.bool.must.push({"query_string":{"fields":["procInstId"],"query" : "\"" + decodeURIComponent(procInstId) + "\""}});

    //get process history
    $.ajax({
        type: "GET",
        url: "/${base}/rest/history/" + procInstId,
        Accept : "application/json",
        contentType: "application/json",
        dataType: "json",
        async: false
    }).success(function(data) {
        var status = data.state;
        if (data.state === "COMPLETED") {
            status = "Complete";
        }
        else if (data.state === "ACTIVE") {
            status = "Running";
        }
        proc_info["process_definition"] = data.procDefKey;
        proc_info["process_instance"] = data.procInstId;
        proc_info["start_time"] = data.startTime;
        proc_info["end_time"] = data.endTime;
        proc_info["duration"] = convertMillis(data.duration);
        proc_info["status"] = status;
        for (const entry of data.details) {
            let date = entry["date"];
            if (entry["message"].startsWith("Ended ")) {
                date += " ";
            }
            const row = [date, entry["type"], entry["activity"], outputMessage(entry["message"])];
            logLines.push(row);
        }
    }).fail(function(xhr, err) {
        console.error("Error getting instance JSON: " + xhr.responseText);
    });

    $.ajax({
        type: "GET",
        url: "/${base}/rest/logs/get?source=" + encodeURIComponent(JSON.stringify(baseEsReq)),
        Accept : "application/json",
        contentType: "application/json",
        dataType: "json",
        async: false
    }).success(function(data) {
        var finished = false;
        scrollId = data._scroll_id;
        if (data.hits) {
            for (const hit of data.hits.hits) {
                const source = hit._source;
                const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                logLines.push(row);
                
            }
        }
        while (!finished) {
            $.ajax({
                type: "POST",
                url: "/${base}/rest/logs/get/scroll",
                data: "scrollId=" + scrollId,
                async: false,
                success: function(data) {
                    if (data.hits) {
                        
                        if (data.hits.hits.length > 0) {
                            for (const hit of data.hits.hits) {
                                const source = hit._source;
                                const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                                logLines.push(row);
                            }
                            scrollId = data._scroll_id;
                        }
                        else {
                            finished = true;
                        }
                    }
                },
                error: function(e) {
                    alert("Error retrieving history data.");
                }
            });
        }
    }).fail(function(xhr, err) {
        console.error("Error getting instance JSON: " + xhr.responseText);
    });
    logLines.sort(function(a, b) {
        var aTemp = a[0];
        //if there is a space in the last char, remove it
        if (aTemp.charAt(aTemp.length - 1) == " ") {
            aTemp = aTemp.substring(0, aTemp.length - 1);
        }
        var bTemp = b[0];
        //if there is a space in the last char, remove it
        if (bTemp.charAt(bTemp.length - 1) == " ") {
            bTemp = bTemp.substring(0, bTemp.length - 1);
        }
        var aDate = moment(aTemp);
        var bDate = moment(bTemp);
        if (aDate.isBefore(bDate)) return -1;
        if (bDate.isBefore(aDate)) return 1;
        return 0;
    });

    logLines.forEach(function(row) {
        var data = row;
        var details = data[3];
        var tmpDetails = "";
        var lineString = "";
        if (data[3].indexOf("Setting (json)") === -1) {
            details = details.replaceAll('<br>', "\n");
            details = details.replaceAll("<p>", "");
            details = details.replaceAll("</p>", "");
            details = details.replaceAll('"' , '""');
            details = details.replaceAll('\n' , ' ');
            //add first and last char as double quotes
            details = '"' + details + '"';
            lineString = proc_info["process_definition"] + "," + proc_info["process_instance"] + "," + data[0] + "," + data[1] + "," + data[2] + "," + details + "\r\n";
        } else {
            lineString = proc_info["process_definition"] + "," + proc_info["process_instance"] + "," + data[0] + "," + data[1] + "," + data[2] + ",";
            //remove last char
            if (data[3].indexOf("_in =") !== -1) {
                lineString += '"' + details.substring(0, details.indexOf("_in =")+3) + " ";
                details = details.substring(details.indexOf("_in =")+3);
            } else {
                lineString += '"' + details.substring(0, details.indexOf("_out =")+4) + " ";
                details = details.substring(details.indexOf("_out =")+4);
            }
            //now we need to go through and get details from json string
            //note: key is always after <tr><td ...> and value is the following td
            while (details.indexOf("<tr><td") !== -1) {
                details = details.substring(details.indexOf("<tr><td")+8);
                details = details.substring(details.indexOf(">")+1);
                var key = details.substring(0, details.indexOf("</td>"));
                details = details.substring(details.indexOf("<td>")+4);
                var value = details.substring(0, details.indexOf("</td>"));
                tmpDetails += key + ": " + value + "; ";
            }
            //check/clean tmpDetails
            if (tmpDetails !== "") {
                //replace all break points with new line
                tmpDetails = tmpDetails.replaceAll(/<br>/g, " ");
                //find and remove everything between <summary>  and  </summary>
                tmpDetails = tmpDetails.replace(/<summary>.*<\/summary>/g, "");
                //find and remove <details>  and  </details>
                tmpDetails = tmpDetails.replace(/<details>/g, "");
                tmpDetails = tmpDetails.replace(/<\/details>/g, "");
                //CSV quirk: replace all " with ""
                tmpDetails = tmpDetails.replaceAll('"' , '""');
            }
            //remove last char
            tmpDetails = tmpDetails.substring(0, tmpDetails.length-1);
            tmpDetails = tmpDetails + '"';
            lineString += tmpDetails + "\r\n";
        }
        lineString = lineString.replaceAll("<table><tr>", "");
        outputCSV = outputCSV + lineString;
    } );
    return outputCSV;
};

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