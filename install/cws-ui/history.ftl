<html>
<head>
	<meta charset="utf-8">
	<title>CWS - History</title>

	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/docs.min.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<script src="/${base}/js/moment.js"></script>
	<script type="text/javascript" src="/${base}/js/cws.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css" />
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<link href="/${base}/css/microtip.css" rel="stylesheet">
	<style>
		.dataTables_wrapper .filter .dataTables_filter{float:right; padding-top: 15px; display: inline; margin-right: 15px;}
		.dataTables_wrapper .download-button {padding-top: 15px;}
		.dataTables_wrapper .button {float:left; display: inline; margin-top: 15px; margin-right: 15px;}
		.dataTables_wrapper {margin-left: 5px; margin-right: -10px;}
		summary {
			width: 100px;
		}
		.historyLimitSize {
			max-height: 150px;
		}
		.thumbnail {
			margin-top: 5px !important;
			margin-bottom: 0px !important;
		}
	</style>
	<script>

	//STATE PERSISTANCE VARS
	var username = "username";
	var downloadFileTypeVar = "CWS_DASH_HISTORY_DOWNLOAD_FILE_TYPE-" + username;
	var datatableStateVar = "CWS_DASH_HISTORY_DATATABLE_STATE-" + username + "_";

	// Global vars
	var isDataTablesInit = 0;
	var params;
	var FETCH_COUNT = 20;		// 20 seems to make for fastest load times
	
	var baseEsReq = {
		"from": 0,
		"size": FETCH_COUNT,
		"query": { 
			"bool": {
				"must" :[]
			}
		},
		"sort": { "@timestamp": { "order": "asc" } }
	};
	
	function renderSet(rows) {

		var table = $("#logData").DataTable();
	
		for (var i = 0; i < rows.length; i++) {
			table.row.add($(rows[i]));
		}
	}

	$(document).on('click', '.copy', function (e) {
			e.preventDefault();
			var copyValue = $(this).attr('data-copyValue');
			copyInput(copyValue);
			$(this).attr('aria-label', 'Copied!');
			setTimeout(function () {
				$('.copy').attr('aria-label', 'Copy');
			}, 2000);
		});
		
	function getMoreLogData(scrollId) {

		$.ajax({
			type: "POST",
			url: "/${base}/rest/logs/get/scroll",
			data: "scrollId=" + scrollId,
			success: function(data) {
				if (data.hits) {
					
					if (data.hits.hits.length > 0) {
					
						const tableRows = buildLogRows(data);

						renderSet(tableRows);

						// Call again until no more hits
						getMoreLogData(data._scroll_id);
					}
					else {

						// Done with all rendering row sets
						$(".ajax-spinner").hide();

						var table = $("#logData").DataTable();

						table.draw();
					
					}
					
				} // end if (data.hits)
			},
			error: function(e) {
				$(".ajax-spinner").hide();
				
				alert("Error retrieving history data.");
			}
		});
	}

	function buildLogRows(data) {
	
		let tableRows = [];
		
		if (data.hits) {

			for (const hit of data.hits.hits) {

				const source = hit._source;

				const row = "<tr><td>"+ source["@timestamp"] + "</td>"+
							"<td>Log</td>"+
							"<td>"+ source.actInstId.split(':')[0] + "</td>"+
							"<td><p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p></td></tr>";
				
				tableRows.push(row);
				
			} // end for each theLogData

		} // end if (data.hits)
		
		return tableRows;
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

		return first + '<details><summary> Show All</summary>' + rest + '</details>'
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

	function buildHistoryRows(data) {
	
		let tableRows = [];
		let inputVarRows = [];
		
		if (data.details) {
					
			for (const entry of data.details) {
				
				let date = entry["date"];
				
				if (entry["message"].startsWith("Ended ")) {
					date += " ";
				}
				
				const row = "<tr><td>"+ date + "</td>" +
							"<td>"+ entry["type"] + "</td>"+
							"<td>"+ entry["activity"] + "</td>"+
							"<td>"+ outputMessage(entry["message"]) + "</td></tr>";
						
				tableRows.push(row);
			}
	
			$('#procDefKey').html(data.procDefKey);
			$('#procInstId').html(data.procInstId);
			$('#procStartTime').html(data.startTime);
			$('#procEndTime').html(data.endTime);
			
			if (data.duration !== 0) {
				$('#procDuration').html(convertMillis(data.duration));
			}
			
			$.ajax({
			type: "GET",
			url: "/${base}/rest/history/getStatus/" + data.procInstId,
			success: function(data) {
				var status = data;
				switch (data) {
					case "pending":
						status = "Pending";
						$("#procStatus").css("color", "blue");
						break;
					case "disabled":
						status = "<b>Disabled</b>";
						$("#procStatus").css("color", "red");
						break;
					case "failedToSchedule":
						status = "<b>Failed to schedule</b>";
						$("#procStatus").css("color", "red");
						break;
					case "claimedByWorker":
						status = "Claimed by Worker";
						$("#procStatus").css("color", "blue");
						break;
					case "failedToStart":
						status = "<b>Failed to start</b>";
						$("#procStatus").css("color", "red");
						break;
					case "running":
						status = "Running";
						$("#procStatus").css("color", "blue");
						break;
					case "complete":
						status = "Complete";
						$("#procStatus").css("color", "green");
						break;
					case "resolved":
						status = "Resolved";
						$("#procStatus").css("color", "green");
						break;
					case "fail":
						status = "<b>Failed</b>";
						$("#procStatus").css("color", "red");
						break;
					case "incident":
						status = "<b>Incident</b>";
						$("#procStatus").css("color", "red");
						break;
					default:
						status = "<b>Unknown</b>";
						$("#procStatus").css("color", "red");
						break;
				}
				$('#procStatus').html(status);
				if ($("#procStatus").text() == "Failed") {
					$("#resolveButtonDiv").show();
					$("#resolveButton").show();
					$("#retryIncidentButton").hide();
				} else if ($("#procStatus").text() == "Incident") {
					$("#resolveButtonDiv").show();
					$("#retryIncidentButton").show();
					$("#resolveButton").hide();
				} else {
					$("#resolveButtonDiv").hide();
					$("#resolveButton").hide();
					$("#retryIncidentButton").hide();
				}
			},
			error: function(e) {
				$("procStatus").html("Error fetching status - please try again later.");
			}
			});
		}

		setInputVariableTable(data.inputVariables);
		setOutputVariableTable(data.outputVariables);
		
		return tableRows;
	}
	
	function processData(historyData, logData) {
	
		const historyRows = buildHistoryRows(historyData[0]);
		const logRows = buildLogRows(logData[0]);
		
		renderSet(historyRows.concat(logRows));
		
		// Get rest of log data (if exists)
		getMoreLogData(logData[0]._scroll_id);
	}
	
	function processFailed(historyError, logError) {
	
		$(".ajax-spinner").hide();
		
		console.log("Errors", historyError, logError);
		
		alert("Error retrieving history data.");
	}

	function downloadLogCSV() {
		var dt = $('#logData').DataTable();
		var data = dt.buttons.exportData();
		//number of rows
		var csvString = "";
		//get headers and put them as first row in CSV
		for (var i = 0; i < data.header.length; i++) {
			csvString = csvString + data.header[i] + ",";
		}
		csvString = csvString.substring(0, csvString.length - 1);
		csvString = csvString + "\r\n";

		dt.rows().every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
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
				lineString = data[0] + "," + data[1] + "," + data[2] + "," + details + "\r\n";
			} else {
				lineString = data[0] + "," + data[1] + "," + data[2] + ",";
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
			csvString = csvString + lineString;
		} );

		$.fn.dataTable.fileSave(
			new Blob( [ csvString ] ),
			$("#procInstId").text() + '.csv'
		);
	}

	function downloadLogJSON() {
		console.log($("#procInstId").html());
        var mainJSON = getInstanceJSON($('#procInstId').html(), "${base}");
        $.fn.dataTable.fileSave(
            new Blob([JSON.stringify(mainJSON)]),
            'history-' + $("#procInstId").html() + '.json'
        );
    }

	function retryIncident(procInstId) {
		var idArr = [procInstId];
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/retryIncidentRows",
			Accept : "application/json",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(idArr),
		})
		.done(function(msg) {
			console.log(msg);
			location.reload();
		})
		.fail(function(xhr, err) {
			console.err(msg);
		});
	}

	function markAsResolved(procInstId) {
		var idArr = [procInstId];
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/markResolved",
			Accept : "application/json",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(idArr),
		})
		.done(function(msg) {
			console.log(msg);
			location.reload();
		})
		.fail(function(xhr, err) {
			console.err(xhr.responseTextmsg.message);
		});
	}
	
	$( document ).ready(function() {

		$("#logData").DataTable({
			order: [[0, 'asc']],
			paging: false,
			dom: "<'row'<'col-sm-2 download-button'><'col-sm-10 filter'f>>" + "tip",
		});

		$('<div class="dropdown" style="display:inline;">'
			+ '<button id="downloadButton" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Download &nbsp;'
			+ '<span class="caret"></span>'
			+ '</button>'
			+ '<ul id="action-list" class="dropdown-menu" role="menu" aria-labelledby="menu3">'
			+ '<li id="action_download_json" class="enabled" role="presentation"><a id="json-bttn" role="menuitem" href="#">Download as JSON</a></li>'
			+ '<li id="action_download_csv" class="enabled" role="presentation"><a id="csv-bttn" role="menuitem" href="#">Download as CSV</a></li>'
  			+ '</ul>'
  			+ '</div>').appendTo(".download-button");

		// Get query string values
		params = getQueryString();

		if (params && params.procInstId) {
		
			let esReq = baseEsReq;
		
			$(".ajax-spinner").show();
			
			esReq.query.bool.must.push({"query_string":{"fields":["procInstId"],"query" : "\"" + decodeURIComponent(params.procInstId) + "\""}});
			
			// Get history and log data (first scroll) in parallel
			$.when( $.getJSON("/${base}/rest/history/" + params.procInstId), 
					$.getJSON("/${base}/rest/logs/get?source=" + encodeURIComponent(JSON.stringify(esReq))) ).then(processData, processFailed);
			
			// In case of unknown problems, just hide spinner
			setTimeout(function() {
				$(".ajax-spinner").hide();
			}, 180000);
		}

		$("#json-bttn").click(function(e) {
			e.preventDefault();
			downloadLogJSON();
		});

		$("#csv-bttn").click(function(e) {
			e.preventDefault();
			downloadLogCSV();
		});


	}); //END OF DOCUMENT.READY

	function setInputVariableTable(data) {
		if (jQuery.isEmptyObject(data)) {
			$("#inputVariables").html("None");
		} else {
			var output = "";
		var before = "";
		var after = "";
		var putAllAfter = 0;
		var count = 0;
		for (const [key, value] of Object.entries(data)) {
			var temp = "";
			var tempVal = value;
			var tempKey = key;
			if (tempKey === "workerId") {
				continue;
			}
			if (count > 3) {
				putAllAfter = 1;
			}
			if (key.includes("(file, image")) {
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b><img class="history-grow historyLimitSize" src="` + tempVal + `"></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			} else if (checkForURL(tempVal)) {
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b><a href="` + tempVal + `">` + tempVal + `</a></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			} else {
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b>` + tempVal + `</a></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			}
			if (tempKey === "startedOnWorkerId") {
				after = after + temp;
				putAllAfter = 1;
			} else if (putAllAfter === 0) {
				before = before + temp;
			} else {
				after = after + temp;
			}
			count++;
		}
		if (after.length == 0) {
			output = before;
		} else {
			output = before + "<details><summary><b> Show All</b></summary>" + after + "</details>";
		}
		}
		$("#inputVariables").html(output);
	}

	function setOutputVariableTable(data) {
		if (jQuery.isEmptyObject(data)) {
			$("#outputVariables").html("None");
		} else {
			var output = "";
		var before = "";
		var after = "";
		var putAllAfter = 0;
		var count = 0;
		for (const [key, value] of Object.entries(data)) {
			var temp = "";
			var tempVal = value;
			var tempKey = key.substring(7);
			if (tempKey === "workerId") {
				continue;
			}
			if (count > 3) {
				putAllAfter = 1;
			}
			if (key.includes("(file, image")) {
				tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b><img class="grow historyLimitSize" src="` + tempVal + `"></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			} else if (checkForURL(tempVal)) {
				tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b><a href="` + tempVal + `">` + tempVal + `</a></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			} else {
				tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
				temp = `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: center; gap: 10px;"><div style="flex-grow: 1; align-self: start"><b>` + tempKey + `: </b>` + tempVal + `</a></div><div style="align-self: start; margin-top: auto; margin-bottom: auto;"><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span></div></div><br>`;
			}
			if (tempKey === "startedOnWorkerId") {
				after = after + temp;
				putAllAfter = 1;
			} else if (putAllAfter === 0) {
				before = before + temp;
			} else {
				after = after + temp;
			}
			count++;
		}
		if (after.length == 0) {
			output = before;
		} else {
			output = before + "<details><summary><b> Show All</b></summary>" + after + "</details>";
		}
		}
		$("#outputVariables").html(output);
	}

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
            "status": status,
            "input_variables": data.inputVariables,
            "output_variables": data.outputVariables
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
	</script>
	

	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->

	<style type="text/css">

	#logData{
		font-size:95%;
	}
	#logData tr th{
		white-space: nowrap;
	}
	/* log level header*/
	#logData tr th:nth-child(5){
		padding-right: 25px;
	}
	#logData tr td{
		line-height: 1;
	}
	#logData tr td:nth-child(4){
		/*overflow: auto;*/
		word-wrap: break-word;
		word-break: break-all;
		font-family: courier,consolas;
		/*white-space: normal !important;*/
	}
	summary {
		display: list-item;
	}
	</style>

</head>

<body>
	<#include "navbar.ftl">

	<div class="container-fluid" style="margin-left: 20px;">
		
		<h2 class="sub-header">History</h2>
		<div class="row">
			<table align="center" class="table table-bordered " style="width: 50%; font-size:95%">
				<tr>
					<td style="font-weight:bold;">Process Definition</td><td id="procDefKey">Unknown</td>
				</tr>
				<tr>
					<td style="font-weight:bold;">Process Instance ID</td><td id="procInstId">Unknown</td>
				</tr>
				<tr>
					<td style="font-weight:bold;">Start Time</td><td id="procStartTime">N/A</td>
				</tr>
				<tr>
					<td style="font-weight:bold;">End Time</td><td id="procEndTime">N/A</td>
				</tr>
				<tr>
					<td style="font-weight:bold;">Duration</td><td id="procDuration">N/A</td>
				</tr>
				<tr>
					<td style="font-weight:bold;">Status</td><td id="procStatus"></td>
				</tr>
				<tr>
					<td style="font-weight:bold;">Input Variables</td><td id="inputVariables"></td>
				</tr>
			</table>
		</div>
		<div class="row">
			<table align="center" class="table table-bordered" style="width: 50%; font-size: 95%">
				<tr>
					<th>Output Variables</th>
				</tr>
				<tr>
					<td id="outputVariables"></td>
				</tr>
			</table>
		</div>
      <div id="resolveButtonDiv" class="row" style="text-align: center; display: none;">
        <button id="resolveButton" class="btn btn-primary" type="button" onclick="markAsResolved($('#procInstId').text())">Mark as Resolved</button>
		<button id="retryIncidentButton" class="btn btn-primary" type="button" onclick="retryIncident($('#procInstId').text())">Retry Incident</button>
      </div>
		</div>
	
		<div class="row">
			<div class="ajax-spinner"></div>
		</div>
		<div class="row">
			<div class="col-md-12 main">
				<div id="log-div">
					<table id="logData" class="table table-striped table-bordered sortable">
						<thead>
							<tr>
								<th id="timeStampColumn" style="width: 185px">Time Stamp</th>
								<th>Type</th>
								<th>Source</th>
								<th>Details</th>
							</tr>
						</thead>
					</table>
				</div>
			</div>
		</div>
		<a id="downloadAnchorElement" style="display:none"></a>
	</div>
</body>
</html>