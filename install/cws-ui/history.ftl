<html>
<head>
	<meta charset="utf-8">
	<title>CWS - History</title>

	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/docs.min.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css" />
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
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

		return first + '<details><summary>Show All</summary>' + rest + '</details>'
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
			},
			error: function(e) {
				$("procStatus").html("Error fetching status - please try again later.");
			}
			});
		}
		
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
		console.log(data);
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
			console.log(data);
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
				console.log(lineString);
			}
			csvString = csvString + lineString;
		} );

		$.fn.dataTable.fileSave(
			new Blob( [ csvString ] ),
			$("#procInstId").text() + '.csv'
		);
	}

	function downloadLogJSON() {
		var dt = $('#logData').DataTable();
		var data = dt.buttons.exportData();
		//number of rows
		var numRows = dt.rows().count();
		var jsonFile = {};
		var logInfo = {
			"process_definition": $("#procDefKey").text(),
			"process_instance": $("#procInstId").text(),
			"start_time": $("#procStartTime").text(),
			"end_time": $("#procEndTime").text(),
			"duration": $("#procDuration").text(),
			"status": $("#procStatus").text()
		}
		jsonFile["process_info"] = logInfo;

		var logs = {};

		dt.rows().every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			var tmpDetails = data[3];
			var details = "";
			var lineJson = {};
			var nestedJson = {};
			if (data[3].indexOf("Setting (json)") === -1) {
				details = data[3];
				lineJson = {
					"time-stamp": data[0],
					"type": data[1],
					"source": data[2],
					"details": details
				};
			} else {
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
				details = details.replaceAll("<p>", "");
				details = details.replaceAll("</p>", "");
				lineJson["details"] = details;
			}
			logs[numRows-rowLoop-1] = lineJson;
		} );
		jsonFile["logs"] = logs;
		console.log(jsonFile);
		$.fn.dataTable.fileSave(
			new Blob( [ JSON.stringify(jsonFile) ] ),
			$("#procInstId").text() + '.json'
		);
	}
	
	$( document ).ready(function() {

		$("#logData").DataTable({
			order: [[0, 'desc']],
			paging: false
		});

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

	<div class="container-fluid">
		
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
				</table>
		</div>

		<div class="col-md-12">
				<div class="dropdown" style="display:inline;">
				<button id="downloadButton" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Download &nbsp;
					<span class="caret"></span>
				</button>
				<ul id="action-list" class="dropdown-menu" role="menu" aria-labelledby="menu3">
					<li id="action_download_json" class="enabled" role="presentation"><a id="json-bttn" role="menuitem" href="#">Download as JSON</a></li>
					<li id="action_download_csv" class="enabled" role="presentation"><a id="csv-bttn" role="menuitem" href="#">Download as CSV</a></li>
  				</ul>
  			</div>
		</div>
		<!--
			<div class="col-sm-12 main">
				<p>Select file type:</p>
				<div id="downloadRadios">
					<form class="fileType">
						<input type="radio" name="fileType" value="json" checked>
						<label for="json">JSON</label><br>
						<input type="radio" name="fileType" value="csv">
						<label for="csv">CSV</label><br>
					</form>
				</div>
				<button class="btn btn-primary" role="button" onclick="downloadLog()">Download Log</button>
			</div>
		-->
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

<script type="text/javascript" src="/${base}/js/cws.js"></script>
</body>
</html>