<html>
<head>
	<meta charset="utf-8">
	<title>CWS - History</title>

	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/docs.min.js"></script>
	<script src="/${base}/js/popper.min.js"></script>
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
	<link href="/${base}/css/history.css" rel="stylesheet">
	<script>

	//STATE PERSISTANCE VARS
	var username = document.cookie.substring(document.cookie.indexOf("cwsUsername=") + 12);
	if (username.indexOf(";") > 0) {
		username = username.substring(0, username.indexOf(";"));
	}
	var downloadFileTypeVar = "CWS_DASH_HISTORY_DOWNLOAD_FILE_TYPE-" + username;
	var datatableStateVar = "CWS_DASH_HISTORY_DATATABLE_STATE-" + username;
	var hideLogLinesVar = "CWS_DASH_HISTORY_HIDE_LOG_LINES-" + username;

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
		if ($(this).attr("data-downloadValue") !== undefined && $(this).attr("data-downloadValue") !== false && $(this).attr("data-downloadValue") !== null) {
			var downloadValue = $(this).attr('data-downloadValue');
			var downloadName = $(this).attr('data-downloadName');
			downloadFile(downloadValue, downloadName);
			$(this).attr('aria-label', 'Downloaded!');
			setTimeout(function () {
				$('.copy').attr('aria-label', 'Download');
			}, 2000);
			return;
		}
		var copyValue = $(this).attr('data-copyValue');
		var isImage = $(this).attr('data-isImage');
		copyInput(copyValue, isImage);
		$(this).attr('aria-label', 'Copied!');
		setTimeout(function () {
			$('.copy').attr('aria-label', 'Copy');
		}, 2000);
		console.log("fire");
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
			$('#procInstId').html(`<div class="procInstId-cell">` + data.procInstId + `</div>`);
			var momentStart;
			var momentEnd;
			if (data.startTime !== null && data.startTime !== undefined && data.startTime !== "") {
				momentStart = moment(data.startTime);
				$('#procStartTime').html(momentStart.format('MMM D, YYYY, h:mm:ss A'));
			} else {
				$('#procStartTime').html("");
			}
			if (data.endTime !== null && data.startTime !== undefined && data.startTime !== "") {
				momentEnd = moment(data.endTime);
				$('#procEndTime').html(momentEnd.format('MMM D, YYYY, h:mm:ss A'));
			} else {
				$('#procEndTime').html("");
			}
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
		console.log($("#procInstId").text());
        var mainJSON = getInstanceJSON($('#procInstId').text(), "${base}");
        $.fn.dataTable.fileSave(
            new Blob([JSON.stringify(mainJSON)]),
            'history-' + $("#procInstId").text() + '.json'
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
			dom: "<'above-table-div'<'above-table-buttons'><'above-table-filler'><'above-table-filter'f>>"
                        + "t"
		});

		$('<div class="dropdown" style="display:inline;">'
			+ '<button id="downloadButton" class="btn btn-primary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">&nbsp;Download &nbsp;'
			+ '<span class="caret"></span>'
			+ '</button>'
			+ '<ul id="action-list" class="dropdown-menu">'
			+ '<li id="action_download_json" class="enabled" role="presentation"><a id="json-bttn" role="menuitem" href="#" class="dropdown-item">Download as JSON</a></li>'
			+ '<li id="action_download_csv" class="enabled" role="presentation"><a id="csv-bttn" role="menuitem" href="#" class="dropdown-item">Download as CSV</a></li>'
  			+ '</ul>'
  			+ '</div>').appendTo(".above-table-buttons");

		$('<div id="hide-log-lines" style="display: inline;">'
			+ `<input id="showall" type="radio" value="showall" name="log-line-control">`
			+ `<label style="margin-left: 8px;" for="showall">Show All</label>`
			+ `<input id="logonly" style="margin-left: 10px;" type="radio" value="logonly" name="log-line-control">`
			+ `<label style="margin-left: 8px;" for="hide">CmdLine Log Lines Only</label>`
			+ `<input id="nolog" style="margin-left: 10px;" type="radio" value="nolog" name="log-line-control" checked>`
			+ `<label style="margin-left: 8px;" for="hide">Exclude CmdLine Log Lines</label>`
			+ '</div>').appendTo(".above-table-buttons");

		if (localStorage.getItem(hideLogLinesVar) === "showall") {
			$("#showall").attr("checked", true);
		} else if (localStorage.getItem(hideLogLinesVar) === "logonly") {
			$("#logonly").attr("checked", true);
		} else if (localStorage.getItem(hideLogLinesVar) === "nolog") {
			$("#nolog").attr("checked", true);
		} else {
			$("#nolog").attr("checked", true);
		}

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

		//if the checkbox is checked, hide the corresponding log lines
		if ($("#showall").is(":checked")) {
			$("#logData").DataTable().column(3).search("").draw();
		} else if ($("#logonly").is(":checked")) {
			$("#logData").DataTable().column(3).search("LINE: ", true, false).draw();
		} else if ($("#nolog").is(":checked")) {
			$("#logData").DataTable().column(3).search("^(?!LINE: )", true, false).draw();
		} else {
			$("#logData").DataTable().column(3).search("").draw();
		}

		$("#showall").change(function() {
			if(this.checked) {
				$("#logData").DataTable().column(3).search("").draw();
				localStorage.setItem(hideLogLinesVar, "showall");
			}
		});
		$("#logonly").change(function() {
			if(this.checked) {
				$("#logData").DataTable().column(3).search("LINE: ", true, false).draw();
				localStorage.setItem(hideLogLinesVar, "logonly");
			}
		});
		$("#nolog").change(function() {
			if(this.checked) {
				$("#logData").DataTable().column(3).search("^(?!LINE: )", true, false).draw();
				localStorage.setItem(hideLogLinesVar, "nolog");
			}
		});
	}); //END OF DOCUMENT.READY

	function downloadFile(data, name) {
		var decodedData = atob(data);
		$.fn.dataTable.fileSave(
			new Blob([decodedData]), name
		);
	}

	function checkforImageURL(potentialURL) {
		if (potentialURL === undefined || potentialURL === null || potentialURL === "") {
			return false;
		} else if (potentialURL.startsWith("www.") || potentialURL.startsWith("http://") || potentialURL.startsWith("https://") || potentialURL.startsWith("s3://")) {
			if (potentialURL.endsWith(".png") || potentialURL.endsWith(".jpg") || potentialURL.endsWith(".jpeg") || potentialURL.endsWith(".gif")) {
				return true;
			}
		}
		try {
			new URL(potentialURL);
			return true;
		}
		catch (e) {
			return false;
		}
	}

	function setInputVariableTable(data) {
		if (jQuery.isEmptyObject(data)) {
			$("#inputVariables").html("None");
		} else {
			var output = "";
			var timeStart = $("#procStartTime").html();
			for (const [key, value] of Object.entries(data).reverse()) {
				var temp = "";
				var varTimeSet = key.substring(key.indexOf("[")+1, key.indexOf("]"));
				if (moment(varTimeSet).diff(timeStart, "seconds") > 1) {
					continue;
				}
				var tempVal = value;
				var tempKey = key.substring(key.indexOf("]")+1);
				if (key.includes("(file, image")) {
					tempKey = tempKey.replace("file, ", "");
					temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ `<img class="grow historyLimitSize" src='` + tempVal + `'></div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
						+ `<img src="images/copy.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
				} else if (key.includes("{")) {
					var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
					tempKey = tempKey.substring(tempKey.indexOf("]") + 1, tempKey.indexOf(" {"));
					temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ `<i>` + fileName + `</i></div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick=''>`
						+ `<img src="images/download.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
				} else if (checkforImageURL(tempVal)) {
					tempKey = tempKey.replace("string", "url");
					temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2">`
						+ `<b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ `<img class="grow historyLimitSize" src="` + tempVal + `"></div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
						+ `<img src="images/copy.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
				} else if (checkForURL(tempVal)) {
					tempKey = tempKey.replace("string", "url");
					temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2">`
						+ `<b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ `<a href="` + tempVal + `">` + tempVal + `</a></div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
						+ `<img src="images/copy.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
				} else {
					if (key.includes("(string)")) {
						tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
					}
					temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2">`
						+ `<b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ tempVal + `</div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
						+ `<img src="images/copy.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
				}
				output = output + temp;
			}
		}
		$("#inputVariables").html(output);
	}

	function setOutputVariableTable(data) {
		if (jQuery.isEmptyObject(data)) {
			$("#outputVariables").html("None");
		} else {
			var output = "";
			if (Object.keys(data).includes("output_display_order (object)")) {
				//we have an order array
				var orderTruncated = data["output_display_order (object)"].substring(1, data["output_display_order (object)"].length - 1).split(", ");
				var fullKeys = Object.keys(data);
				var fullKeysInOrder = [];
				for (var i = 0; i < Object.keys(orderTruncated).length; i++) {
					var result =fullKeys.findIndex(element => element.includes(orderTruncated[i]));
					if (result > -1) {
						fullKeysInOrder.push(fullKeys[result]);
					}
				}
				//the orderTruncated array now contains the full keys in the order they should be displayed
				for (key in fullKeysInOrder) {
					var temp = "";
					var tempVal = data[fullKeysInOrder[key]];
					var tempKey = fullKeysInOrder[key].substring(7);
					if (tempKey.includes("(file, image")) {
						tempKey = tempKey.replace("file, ", "");	
						temp = `<div class="proc-var-flex-main">`
						+ `<div class="proc-var-flex-main-sub-1">`
						+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
						+ `<div class="proc-var-flex-main-sub-3">`
						+ `<img class="grow historyLimitSize" src='` + tempVal + `'></div></div>`
						+ `<div class="proc-var-flex-btn">`
						+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
						+ `<img src="images/copy.svg" class="copy-icon clipboard">`
						+ `</span></div></div>`;
					} else if (tempKey.includes("{")) {
						var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
						tempKey = tempKey.substring(tempKey.indexOf("]")+1, tempKey.indexOf(" {"));
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<i>` + fileName + `</i></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick=''>`
							+ `<img src="images/download.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkforImageURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<img class="grow historyLimitSize" src="` + tempVal + `"></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkForURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<a href="` + tempVal + `">` + tempVal + `</a></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else {
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ tempVal + `</div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;

				}
				output = output + temp;
			}
				//now we need to add any keys that were not in the fullKeysInOrder array
				//first, determine which keys were not in the fullKeysInOrder array
				var keysNotInOrder = fullKeys.filter(x => !fullKeysInOrder.includes(x));
				//now add the keys that were not in the fullKeysInOrder array
				for (key in keysNotInOrder) {
					if (keysNotInOrder[key] == "output_display_order (object)") {
						continue;
					}
					var temp = "";
					var tempVal = data[keysNotInOrder[key]];
					var tempKey = keysNotInOrder[key].substring(7);
					if (tempKey.includes("(file, image")) {
						tempKey = tempKey.replace("file, ", "");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<img class="grow historyLimitSize" src='` + tempVal + `'></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (tempKey.includes("{")) {
						var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
						tempKey = tempKey.substring(tempKey.indexOf("]")+1, tempKey.indexOf(" {"));
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<i>` + fileName + `</i></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick=''>`
							+ `<img src="images/download.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkforImageURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<img class="grow historyLimitSize" src="` + tempVal + `"></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkForURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<a href="` + tempVal + `">` + tempVal + `</a></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else {
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ tempVal + `</div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					}
					output = output + temp;
				}
		} else {
				//behavior for if variable "output_display_order" is not set
				for (const [key, value] of Object.entries(data).reverse()) {
					var temp = "";
					var tempVal = value;
					var tempKey = key.substring(7);
					if (tempKey.includes("(file, image")) {
						tempKey = tempKey.replace("file, ", "");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<img class="grow historyLimitSize" src='` + tempVal + `'></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (tempKey.includes("{")) {
						var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
						tempKey = tempKey.substring(tempKey.indexOf("]")+1, tempKey.indexOf(" {"));
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2"><b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<i>` + fileName + `</i></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick=''>`
							+ `<img src="images/download.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkforImageURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<img class="grow historyLimitSize" src="` + tempVal + `"></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else if (checkForURL(tempVal)) {
						tempKey = tempKey.replace("string", "url");
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ `<a href="` + tempVal + `">` + tempVal + `</a></div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					} else {
						temp = `<div class="proc-var-flex-main">`
							+ `<div class="proc-var-flex-main-sub-1">`
							+ `<div class="proc-var-flex-main-sub-2">`
							+ `<b>` + tempKey + `: </b></div>`
							+ `<div class="proc-var-flex-main-sub-3">`
							+ tempVal + `</div></div>`
							+ `<div class="proc-var-flex-btn">`
							+ `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick=''>`
							+ `<img src="images/copy.svg" class="copy-icon clipboard">`
							+ `</span></div></div>`;
					}
					output = output + temp;
				}
			}
		$("#outputVariables").html(output);
	}
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
        async: false,
        success: function(data) {
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
        async: false,
        success: function(data) {
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
	    }
    }).fail(function(xhr, err) {
        console.error("Error getting instance JSON: " + xhr.responseText);
    });
    logLines.sort(function(a, b) {
        var aDate = moment(a[0].trim());
        var bDate = moment(b[0].trim());
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

    return `<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: space-between; align-items: flex-start; gap: 0px;"><div>` + first + '<details><summary>Show All</summary>' + rest + `</details></div><div><span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + msg + `" onClick=''><img src="images/copy.svg" class="copy-icon clipboard"></span><div>`
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

</head>

<body>
	<#include "navbar.ftl">

	<div class="container-fluid" style="max-width: 100%; margin: 25px auto; padding: 0 20px;">
		
		<h2 class="sub-header">History</h2>
		<div class="row">
			<table align="center" class="table" style="width: 50%; font-size: 14px; margin-top: 15px;">
				<thead>
					<tr>
						<th colspan="2" style="text-align: center;"><h6>Process Details</h6></th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;">
							<td style="font-weight:bold; max-width: 50%; min-width: 200px;">Process Definition</td><td id="procDefKey" style="flex-grow: 4;">Unknown</td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">Process Instance ID</td><td id="procInstId" style="flex-grow: 4;">Unknown</td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">Start Time</td><td id="procStartTime" style="flex-grow: 4;">N/A</td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">End Time</td><td id="procEndTime" style="flex-grow: 4;">N/A</td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">Duration</td><td id="procDuration" style="flex-grow: 4;">N/A</td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">Input Variables</td><td id="inputVariables" style="flex-grow: 4;"></td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight: bold;">Output Variables<br><i style="font-weight: normal;">All output variables start with "output_"</i></td><td id="outputVariables" style="flex-grow: 4;"></td>
						</div>
					</tr>
					<tr>
						<div style="display: flex; flex-direction: row; flex-wrap: nowrap; justify-content: flex-start; gap: 0px;"></div>
							<td style="font-weight:bold;">Status</td><td id="procStatus" style="flex-grow: 4;"></td>
						</div>
					</tr>
				</tbody>
			</table>
		</div>
      <div id="resolveButtonDiv" class="row" style="text-align: center; display: none; gap: 10px;">
        <div style="display: inline-flex; gap: 10px;">
          <button id="resolveButton" class="btn btn-primary btn-sm" type="button" onclick="markAsResolved($('#procInstId').text())">Mark as Resolved</button>
          <button id="retryIncidentButton" class="btn btn-primary btm-sm" type="button" onclick="retryIncident($('#procInstId').text())">Retry Incident</button>
        </div>
      </div>
		</div>
	
		<div class="row">
			<div class="ajax-spinner"></div>
		</div>
		<div class="row">
			<div class="col main">
				<div id="log-div" style="width: 100%;">
					<table id="logData" class="table table-striped table-bordered sortable" style="margin: 25px 20px; width: 98%;">
						<thead>
							<tr>
								<th id="timeStampColumn" class="col-1" scope="col">Time Stamp</th>
								<th class="col-1" scope="col">Type</th>
								<th class="col-1" scope="col">Source</th>
								<th class="col-3" scope="col">Details</th>
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
