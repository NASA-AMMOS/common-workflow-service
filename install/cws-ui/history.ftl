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
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<script>

	// Global vars
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
	
	/* Not used now, but may be added in the future
	function downloadLog(data) {
		var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(data);
		
		var dlAnchorElem = document.getElementById('downloadAnchorElem');
		
		dlAnchorElem.setAttribute("href", dataStr);
		dlAnchorElem.setAttribute("download", "catalina.out");
		
		dlAnchorElem.click();
	}
	*/
	
	function renderSet(rows) {
	
		for (var i = 0; i < rows.length; i++) {
		
			$('#logData').append(rows[i]);
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
						
						// sort table by Time Stamp (ascending)
						$("#timeStampColumn").click();
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
			
			var status = data.state;
			
			if (data.state === "COMPLETED") {
				status = "Complete";
			}
			else if (data.state === "ACTIVE") {
				status = "Running";
			}
			
			$('#procStatus').html(status);
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
		var CSV = "";
		var headers = $("#logData th");

		headers.each(function(index) {
			CSV += $(this).text() + ",";
		});
		CSV.slice(0, CSV.length - 1);

		CSV += "\r\n";

		$("#logData > tbody > tr").each(function(i) {
			if (i > 0) {
				var cells = $(this).find("td");
				var rowContents = [];
				cells.each(function(j) {
					if (j < 3) {
						//these are the first 3 columns - sanitize and add
						rowContents.push($(this).text().replaceAll('"', '""'));
					} else if (j == 3) {
						//this is the message column - we need to check if this is a nested json
						var cellValue = $(this).text();
						if (cellValue.indexOf("(json)") !== -1) {
							//this is a nested json - we need to parse it and add each key/value pair to the csv
							if (cellValue.indexOf("_in =") !== -1) {
								cellValue = cellValue.substring(0, cellValue.indexOf("_in =")+3);
							} else {
								cellValue = cellValue.substring(0, cellValue.indexOf("_out =")+4);
							}
							//sanitize and push
							rowContents.push(cellValue.replaceAll('"', '""'));
						} else {
							//not a nested json - just sanitize and push
							rowContents.push(cellValue.replaceAll('"', '""'));
						}
					} else {
						//we will only get here if this is a nested json - these will be the cells in that nested json.
						//sanitize and push
						rowContents.push($(this).text().replaceAll('"', '""'));
					}
				});
				//now we have to push the row contents to the csv
				for (var k = 0; k < rowContents.length; k++) {
					if (k < 3) {
						CSV += '"' + rowContents[k] + '",';
					} else if (k == 3) {
						if (rowContents.length > 4) {
							CSV += '"' + rowContents[k] + ' ';
						} else {
							CSV += '"' + rowContents[k] + '"';
						}
					} else {
						if (k == rowContents.length - 1) {
							CSV += rowContents[k] + '"';
						} else {
							CSV += rowContents[k] + ' ';
						}
					}
				}
				CSV += "\r\n";
			}});
		var dataStr = "data:text/csv;charset=utf-8," + encodeURIComponent(CSV);
		var downloadAnchorElement = document.getElementById('downloadAnchorElement');
		downloadAnchorElement.setAttribute("href", dataStr);
		downloadAnchorElement.setAttribute("download", $("#procInstId").text() + ".csv");
		downloadAnchorElement.click();
	}

	function downloadLogJSON() {
		var logRows = [];
		var logHeaders = [];
		var headers = $("#logData th");

		var rows = $("#logData > tbody > tr").each(function(i) {
			cells = $(this).find("td");
			logRows[i] = {};
			var nestedJson = [];
			cells.each(function(j) {
				if (j < 4) {
					if(logHeaders[j] === undefined) {
						logHeaders[j] = $(headers[j]).text();
					}
					var cellValue = $(this).text();
					if(cellValue.indexOf("(json)") !== -1) {
							if (cellValue.indexOf("_in =") !== -1) {
								cellValue = cellValue.substring(0, cellValue.indexOf("_in =")+3);
							} else {
								cellValue = cellValue.substring(0, cellValue.indexOf("_out =")+4);
							}
						}
					logRows[i][logHeaders[j]] = cellValue;
				} else {
					nestedJson.push($(this).text());
				}
			});
			if (nestedJson.length > 0) {
				var nestedJsonObject = {};
				for(var k = 0; k < nestedJson.length; k+= 2) {
					nestedJsonObject[nestedJson[k]] = nestedJson[k+1];
				}
			}
			logRows[i]["json"] = nestedJsonObject;
		});

		logRows.shift();

		var logInfo = {
			"process_definition": $("#procDefKey").text(),
			"process_instance": $("#procInstId").text(),
			"start_time": $("#procStartTime").text(),
			"end_time": $("#procEndTime").text(),
			"duration": $("#procDuration").text(),
			"status": $("#procStatus").text()
		}

		var jsonObj = {
			"process_info": logInfo,
			"log": logRows	
		};
		
		var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(jsonObj));
		var downloadAnchorElement = document.getElementById('downloadAnchorElement');
		downloadAnchorElement.setAttribute("href", dataStr);
		downloadAnchorElement.setAttribute("download", $("#procInstId").text() + ".json");
		downloadAnchorElement.click();
	}
	
	$( document ).ready(function() {

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
						<td style="font-weight:bold;">Status</td><td id="procStatus">Unknown</td>
					</tr>
				</table>
		</div>

		<div class="row">
			<div class="text-center">
				<button class="btn btn-primary" role="button" onclick="downloadLogJSON()">Download Log (JSON)</button>
				<button class="btn btn-primary" role="button" onclick="downloadLogCSV()">Download Log (CSV)</button>
			</div>
			
		</div>
		
		<div class="row">
			<div class="ajax-spinner"></div>
		</div>
		<div class="row">
			<div class="col-md-12 main">
				<div id="log-div">
					<table id="logData" class="table table-striped table-bordered sortable">
						<tr>
							<th id="timeStampColumn" class="sort" style="width: 185px">Time Stamp</th>
							<th class="sort">Type</th>
							<th class="sort">Source</th>
							<th class="sort">Details</th>
						</tr>
					</table>
				</div>
			</div>
		</div>
		<a id="downloadAnchorElement" style="display:none"></a>
	</div>

<script type="text/javascript" src="/${base}/js/cws.js"></script>
</body>
</html>
