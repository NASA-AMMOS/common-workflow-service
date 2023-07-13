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

function getInstanceJSON(procInstId) {
    var outputJSON = {};
    var logLinesJSON = {};
    var logLines = [];
    var scrollId = "";
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
        var proc_info = {
            "process_definition": data.procDefKey,
            "process_instance": data.procInstId,
            "start_time": data.startTime,
            "end_time": data.endTime,
            "duration": convertMillis(data.duration),
            "status": status
        };
        outputJSON["process_info"] = proc_info;
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

    var i = 0;
    logLines.forEach(function(row) {
        var data = row;
        var tmpDetails = data[3];
        var details = "";
        var lineJson = {};
        var nestedJson = {};
        //go through data[0] and if there is a space at the end, remove it
        if (data[0].charAt(data[0].length - 1) == " ") {
            data[0] = data[0].substring(0, data[0].length - 1);
        }
        if (data[3].indexOf("Setting (json)") === -1) {
            //check if data[3] starts with "<table><tr>". If it does, remove it.
            if (data[3].startsWith("<table><tr>")) {
                tmpDetails = data[3].substring(11);
            }
            details = data[3];
            lineJson = {
                "time-stamp": data[0],
                "type": data[1],
                "source": data[2],
                "details": details
            };
        } else {
            var fixedDetails = "";
            if (data[3].startsWith("<table><tr>")) {
                data[3] = data[3].substring(11);
            }
            //we need to first separate the string from the rest of the HTML
            if (data[3].indexOf("_in =") !== -1) {
                details = data[3].substring(0, data[3].indexOf("_in =")+3);
                tmpDetails = data[3].substring(data[3].indexOf("_in =")+3);
            } else {
                details = data[3].substring(0, data[3].indexOf("_out =")+4);
                tmpDetails = data[3].substring(data[3].indexOf("_out =")+4);
            }
            //now we need to go through and get details from json string
            //note: key is always after <tr><td ...> and value is the following td
            while (tmpDetails.indexOf("<tr><td") !== -1) {
                tmpDetails = tmpDetails.substring(tmpDetails.indexOf("<tr><td")+8);
                tmpDetails = tmpDetails.substring(tmpDetails.indexOf(">")+1);
                var key = tmpDetails.substring(0, tmpDetails.indexOf("</td>"));
                tmpDetails = tmpDetails.substring(tmpDetails.indexOf("<td>")+4);
                var value = tmpDetails.substring(0, tmpDetails.indexOf("</td>"));
                nestedJson[key] = value;
            }
            //check/clean nested json object
            if (nestedJson["stdout"] !== undefined) {
                //replace all break points with new line
                nestedJson["stdout"] = nestedJson["stdout"].replaceAll(/<br>/g, "\n");
                //find and remove everything between <summary>  and  </summary>
                nestedJson["stdout"] = nestedJson["stdout"].replace(/<summary>.*<\/summary>/g, "");
            }
            lineJson = {
                "time-stamp": data[0],
                "type": data[1],
                "source": data[2],
                "details": details,
                "json": nestedJson
            };
        }
        //check/clean details
        if (lineJson["details"] !== "") {
            //replace all break points with new line
            details = details.replaceAll('<br>', "\n");
            details = details.replaceAll('<br/>', "\n");
            details = details.replaceAll("<p>", "");
            details = details.replaceAll("</p>", "");
            lineJson["details"] = details;
        }
        logLinesJSON[i] = lineJson;
        i++;
    } );
    outputJSON["logs"] = logLinesJSON;
    return outputJSON;
};

function outputMessage(msg) {

    if (msg.startsWith("Setting (json) ")) {

        var i2 = msg.indexOf("= ")

        if (i2 != -1) {
            var cmd = msg.substring(0, i2 + 1)
            var jsonObj = JSON.parse(msg.substring(i2 + 2))
            var output = '<table><tr>' + cmd + '<br/><br/><table id=\"logDataNest\" class=\"table table-striped table-bordered\">'

            Object.keys(jsonObj).forEach(function(key) {
                var value = jsonObj[key];
                output += makeRow(key, value, cmd)
            });

            output += '</table>'

            return output
        }
    }

    return msg.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
}

function makeRow(key, value, cmd) {

    var style = 'width: 210px;'

    if (cmd.endsWith('_out =')) {
        style = 'width: 120px;'
    }

    if (key == 'stdout' || key == 'stderr') {
        return '<tr><td style="' + style + ';font-weight:bold;">' + key + '</td><td>' + formatMsg(value) + '</td></tr>'
    }
    return '<tr><td style="' + style + ';font-weight:bold;">' + key + '</td><td>' + value + '</td></tr>'
}

function formatMsg(msg) {

    var index = 0, count = 0, maxCount = 30

    for ( ; count < maxCount && i2 != -1; count++) {

        var i2 = msg.indexOf('\n', index)

        if (i2 != -1) {
            index = i2 + 1
        }
    }

    if (count < maxCount - 1 || index > msg.length / 2) {
        return msg.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
    }

    var first = msg.substring(0, index).replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
    var rest = msg.substring(index).replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")

    return first + '<details><summary>Show All</summary>' + rest + '</details>'
}

function convertMillis(millis) {

     var x = millis / 1000
    var seconds = Math.floor(x % 60)
    x /= 60
    var minutes = Math.floor(x)
    
    if (minutes === 0)
        return millis / 1000 + " sec";

    return minutes + " min " + seconds + " sec"
}

