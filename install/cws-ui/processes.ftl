<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Processes</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css" />
	<script src="/${base}/js/moment.js"></script>
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css" />
	<script src="/${base}/js/moment.js"></script>
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
	<script src="/${base}/js/DataTablesDateFilter.js"></script>
	<!-- Custom js adaptation script; override this file from your adaptation project -->
	<script src="/${base}/js/adaptation-process-actions.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
	<style>
		.dataTables_wrapper .filter .dataTables_filter{float:right; padding-top: 15px; display: inline;}
		.dataTables_wrapper .length .dataTables_length{float:left; display: inline; padding-top: 15px; padding-left: 15px; padding-right: 15px;}
		.dataTables_wrapper .buttons .dt-buttons{float:left; display: inline; padding-top: 15px; padding-left: 15px; padding-right: 15px;}
		.dataTables_wrapper .action-button {padding-top: 15px; padding-right: 15px;}
		.dataTables_wrapper .download-button {padding-top: 15px; padding-left: -15px; padding-right: 15px;}
		.dataTables_wrapper .dtsb-titleRow {display: none;}
		.dataTables_wrapper .dtsb-group {padding-bottom: -15px !important; padding-top: 8px;}
	</style>
	<style type="text/css">
		#processes-table {
			font-size: 90%;
		}
		summary::before {
			margin-right: 1ch;
			display: inline-block;
			content: '▶️';
			transition: 0.2s;
		}

		details[open] summary::before {
			transform: rotate(90deg);
		}
	</style>

	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->

	<style type="text/css">
		#pd-select{
			width:90%;
		}
		#status-select{
			border:1px solid #ddd;
			border-radius: 3px;
			padding: 10px;
		}
		#status-select label{cursor: pointer; padding-left: 5px;}
		#datepicker-div input{
			/*width:40%;*/
			margin-bottom: 1em;
			float:left;
		}
		.tr-fail{color:#D9534F;}
		.tr-complete{color:black;}
		.tr-running{color:#5BC0DE;}
		.tr-pending{color:#F0AD4E;}
		.tr-incident{color:#C347ED;} /*#F142F4*/;}
			
		#hide-subprocs-div {
			margin: 20px 0px;
		}
			
		#display-subprocs-div {
			margin: 30px 0px;
		}
		
		#super-proc-inst-id {
			background: #ededed;;
			padding: 5px;
			border-radius: 8px;
			margin-left: 8px;
			padding: 5px;
		}
	</style>
</head>

<body>


<#include "navbar.ftl">

<div class="container-fluid">
	<div class="row">
		<#include "sidebar.ftl">
		<div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">

			<span id="statusMessageDiv">
				<h2>${msg}</h2>
			</span>

			<h2 class="sub-header">Processes</h2>

			<div id="load-more-div">
				<b style="color: red">By default, the 5000 most recent processes are loaded.</b>
				<button id="load-more-btn" class="btn btn-primary">Load More</button>
				<button id="load-less-btn" class="btn btn-primary">Load Less</button>
				<button id="load-all-btn" class="btn btn-primary">Load All</button>
			</div>

  			<div id="hide-subprocs-div">
				<label for="hide-subprocs">Hide Subprocesses</label>
				<input name="hide-subprocs" id="hide-subprocs-btn" type="checkbox">
			</div>
			<div id="display-subprocs-div">
				<h3>Displaying Subprocesses for Process Instance ID: <span id="super-proc-inst-id">34374-349083748</span></h3>
			</div>
  			<div id="action_msg"></div>
			<span id="showing_num_procs"></span><span id="out_of_procs"></span>

			<div id="proc-log">
				<div class="ajax-spinner" id="ajax-spinner"></div>
				<table id="processes-table" class="table table-striped table-bordered sortable">
					<thead>
					<tr>
                        <th>Select</th>
						<th></th>
						<th>Definition Key</th>
						<th>Proc Inst ID</td>
						<th>Status</th>
						<th>Schedule Queued Time</th>
						<th>Started on Worker</th>
						<th>Process Start</th>
						<th>Process End</th>
						<th style="word-wrap: break-word; max-width: 150px;">Input Variables</th>
					</tr>
					</thead>
					<tbody>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">

	//STATE PERSISTANCE CONSTS
	const username = "username"; //temporary, hardcoded value for now
	const rowsToLoadVar = "CWS_DASH_PROC_ROWS-" + username;

	var params = {};
	var rows;
	var loadRows = 5000;
	var rowsTotal = 0;

	$( document ).ready(function() {
		
		//get our current url
		var currentUrl = window.location.href;

		if (localStorage.getItem(rowsToLoadVar) != null) {
			loadRows = parseInt(localStorage.getItem(rowsToLoadVar));
		}

		if (localStorage.getItem(rowsToLoadVar) != null) {
			loadRows = parseInt(localStorage.getItem(rowsToLoadVar));
		}
		
		displayMessage();

		//get our params from the url
		params = getQueryString();

		if (!params) {
			$("#hide-subprocs-btn").prop('checked', false);
			$("#display-subprocs-div").css('display', 'none');
		}
		else if (!params.superProcInstId) {
			$("#hide-subprocs-btn").prop('checked', false);
			$("#display-subprocs-div").css('display', 'none');
		}
		else if (params.superProcInstId.toLowerCase() === 'null') {
			$("#hide-subprocs-btn").prop('checked', true);
			$("#display-subprocs-div").css('display', 'none');
		}
		else {
			$("#hide-subprocs-div").css('display', 'none');
			
			$("#super-proc-inst-id").html(params.superProcInstId);
		}

		$.fn.dataTable.moment( 'MMM D, YYYY, h:mm:ss A' );

		$("#processes-table").DataTable({
			searchDelay: 250,
			select: {
				style: 'multi+shift',
				selector: 'td:first-child'
			},
			columnDefs: [
				{
					orderable: false,
					className: 'select-checkbox',
					targets: 0
				},
				{
					orderable: false,
					searchable: false,
					targets: 1
				},
				{
					orderable: false,
					searchable: false,
					targets: 2
				}
        	],
			stateSave: true,
			dom: "Q<'row'<'col-sm-auto buttons'B>><'row'<'col-sm-1 action-button'><'col-sm-1 download-button'><'col-sm-5 length'l><'col-sm-5 filter'f>>" + "tip",
			buttons: [
				{
					text: "Select all on page",
					action: function () {
						$("#processes-table").DataTable().rows( {page: "current"}).select();
						updateActionList();
					}
				},
				{
					text: "Unselect all on page",
					action: function () {
						$("#processes-table").DataTable().rows( {page: "current"}).deselect();
						updateActionList();
					}
				},
				{
					text: "Select all",
					action: function () {
						$("#processes-table").DataTable().rows({filter: "applied"}).select();
						updateActionList();
					}
				},
				{
					text: "Unselect all",
					action: function () {
						$("#processes-table").DataTable().rows().deselect();
						updateActionList();
					}
				}
			],
			searchBuilder: {
				columns: [3,4,5,6,7,8,9,10],
			},
			language: {
				searchBuilder: {
					title: {
						0: 'Filters',
						_: 'Filters (%d active)'
					},
				}
        	}
		});

		renderRows(loadRows);

		var table = $("#processes-table").DataTable();
		table.on( 'select', function ( e, dt, type, indexes ) {
			updateActionList();
		} );

		table.on( 'deselect', function ( e, dt, type, indexes ) {
			updateActionList();
		} );

		$('<button id="menu3" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Actions &nbsp;' 
			+ '<span class="caret"></span>'
			+ '</button>'
			+ '<ul id="action-list" class="dropdown-menu test" role="menu" aria-labelledby="menu3">'
			+ `<li id="action_open_selected_new_tabs" class="disabled" role="presentation"><a id="action_open_selected_new_tabs_atag" role="menuitem">Open selected rows in new tabs (must not be pending)</a></li>`
			+ `<li id="action_copy_all_selected_history_links" class="disabled" role="presentation"><a id="action_copy_all_selected_history_links_atag" role="menuitem">Copy all selected history links (must not be pending)</a></li>`
			+ `<li id="action_download_selected_json" class="disabled" role="presentation"><a id="action_download_selected_json_atag" role="menuitem">Download logs of selected processes (JSON) (all rows selected must not be pending)</a></li>`
			+ `<li id="action_download_selected_csv" class="disabled" role="presentation"><a id="action_download_selected_csv_atag" role="menuitem">Download logs of selected processes (CSV) (all rows selected must not be pending)</a></li>`
			+ `<li id="action_disable" class="disabled" role="presentation"><a id="action_disable_atag" role="menuitem">Disable selected rows (all rows selected must be 'pending')</a></li>`
    		+ `<li id="action_enable" class="disabled" role="presentation"><a id="action_enable_atag" role="menuitem">Enable selected rows (all rows selected must be 'disabled')</a></li>`
    		+ `<li id="action_retry_incident" class="disabled" role="presentation"><a id="action_retry_incident_atag" role="menuitem">Retry all selected incident rows (all rows selected must be 'incident')</a></li>`
    		+ `<li id="action_retry_failed_to_start" class="disabled" role="presentation"><a id="action_retry_failed_to_start_atag" role="menuitem">Retry all selected failed to start rows (all rows selected must be 'failedToStart')</a></li>`
    		+ `<li id="action_mark_as_resolved" class="disabled" role="presentation"><a id="action_mark_as_resolved_atag" role="menuitem">Mark all selected failed rows as resolved (all rows selected must be 'fail')</a></li>`
  			+ `<#include "adaptation-process-actions.ftl">`
  			+ `</ul>`).appendTo(".action-button");

		$(`<div class="dropdown" style="display:inline;">`
			+ `<button id="downloadButton" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Download &nbsp;`
			+ `<span class="caret"></span>`
			+ `</button>`
			+ `<ul id="action-list" class="dropdown-menu" role="menu" aria-labelledby="menu3">`
			+ `<li id="action_download_json" class="enabled" role="presentation"><a id="json-bttn" role="menuitem" href="javascript:downloadListJSON();">Download as JSON</a></li>`
			+ `</ul>`
			+ `</div>)`).appendTo(".download-button");
	});

	$("#load-more-btn").click(function() {
		loadRows += 5000;
		localStorage.setItem(rowsToLoadVar, loadRows);
		renderRows(loadRows);
	});

	$("#load-less-btn").click(function() {
		loadRows -= 5000;
		if (loadRows < 5000) {
			loadRows = 5000;
		}
		localStorage.setItem(rowsToLoadVar, loadRows);
		renderRows(loadRows);
	});

	$("#load-all-btn").click(function() {
		loadRows = -1;
		localStorage.setItem(rowsToLoadVar, loadRows);
		renderRows(loadRows);
	});

	function getFilterQString(changeHideSubs) {
		var params = {};
		
		if (changeHideSubs) {
			if ($("#hide-subprocs-btn").prop("checked")) {
				params.superProcInstId = "null";
			}
		}
		else {
			var qs = getQueryString();
			
			if (qs && qs.superProcInstId) {
				params.superProcInstId = qs.superProcInstId;
			}
		}
		
		var qstring = "?";

		if(params != null){
			for(p in params){
				qstring += encodeURI(p)+"="+encodeURI(params[p])+"&";
			}
		}
		qstring = qstring.substring(0,qstring.length-1);
		return qstring;
	}

	function updateLocation(changeHideSubs) {
		window.location="/${base}/processes" + getFilterQString(changeHideSubs);
	}
	
	$("#hide-subprocs-btn").click(function(){
		updateLocation(true);
	});
	
	// ---------------------------------
	// DISPLAY STATUS MESSAGE (IF ANY)
	//
	function displayMessage() {
		
		if ($("#statusMessageDiv:contains('ERROR:')").length >= 1) {
			$("#statusMessageDiv").css( "color", "red" );
		}
		else {
			$("#statusMessageDiv").css( "color", "green" );
			if ($('#statusMessageDiv').html().length > 9) {
				$('#statusMessageDiv').fadeOut(5000, "linear");
			}
		}
	}
	


	function viewHistory(procInstId) {
	
		if (procInstId !== '') {
			window.location = "/${base}/history?procInstId=" + procInstId;
		} else {
			return false;
		}
	}
	
	function viewSubProcs(procInstId) {
	
		if (procInstId !== '') {
			window.location = "/${base}/processes?superProcInstId=" + procInstId;
		} else {
			return false;
		}
	}
	
	// ----------------------------------------------------
	// Get the process instances, and render them as rows
	//
	function renderRows(rowsToLoad) {

		$("#proc-log div.ajax-spinner").show();

		qstr = document.location.search;
		//we only care about the superProcInstId part of query string
		if (qstr.indexOf("superProcInstId") > -1) {
			//get everything after ? and before first &
			qstr = qstr.substring(qstr.indexOf("?"));
			if (qstr.indexOf("&") > -1) {
				qstr = qstr.substring(0, qstr.indexOf("&"));
			}
		} else {
			qstr = "";
		}
		//console.log("/${base}/rest/processes/getInstances"+qstr);
		params = getQueryString();
		var numProcs = 0;
		var requestProc = 0;

		//get the number of instances
		$.get("/${base}/rest/processes/getInstancesSize"+qstr,
			function(res) {
				numProcs = res;
				rowsTotal = res;
				if (numProcs < 5000) {
					$("#load-more-div").hide();
				} else {
					$("#load-more-div").show();
				}
				if (rowsToLoad === -1) {
					requestProc = numProcs;
				} else {
					requestProc = rowsToLoad;
				}
				//
				// GET THE PROCESS INSTANCE, 
				// AND RENDER THEM...
				//
				if (qstr === "") {
					qstr = "?";
				}
				$.get("/${base}/rest/processes/getInstancesCamunda"+qstr+"&page="+requestProc,
					function(res) {
						var table = $("#processes-table").DataTable()
						
						table.clear();
						for (i in res) {
							var procInstId = (res[i].procInstId == undefined ? '' : res[i].procInstId);
							var incidentUrl = "/camunda/app/cockpit/default/#/process-instance/" + procInstId + "/runtime?tab=incidents-tab";
							if (res[i].startedByWorker !== undefined) {
								var workerIP = "<br><b>Worker IP: </b>" + res[i].startedByWorker.split("_").slice(0, -2).join(".");
							} else {
								var workerIP = "<br><b>Worker IP: </b>";
							}
							var procStartTime = (res[i].procStartTime == undefined ? '' : res[i].procStartTime);
							var procEndTime = (res[i].procEndTime == undefined ? '' : res[i].procEndTime);
							if (procStartTime !== '' && procEndTime !== '') {
								var start = moment(procStartTime);
								var end = moment(procEndTime);
								var procDuration = "<br><i>(~" + moment.duration(end.diff(start)).humanize() + ")</i>";
							} else {
								var procDuration = '';
							}
							table.row.add(
							$("<tr id=\""+i+"\" class=\"tr-"+ res[i].status +"\" procInstId=\"" + procInstId + "\">"+
								"<td status=\"" + res[i].status + "\" uuid=\"" + res[i].uuid + "\" procInstId=\"" + res[i].procInstId + "\"></td>" +
								"<td><a onclick=\"viewHistory('" + procInstId + "')\" href=\"/${base}/history?procInstId=" + procInstId + "\" class=\"btn btn-default btn-sm\">History</a>" +
								"<a style=\"margin-top: 5px;\" onclick=\"viewSubProcs('" + procInstId + "')\" href=\"/${base}/processes?superProcInstId=" + procInstId + "\" class=\"btn btn-default btn-sm\">Subprocs</a></td>" +
								"<td>"+ res[i].procDefKey +"</td>"+
								"<td>"+ (res[i].status == 'incident' ? ("<a href=\""+ incidentUrl +"\" target=\"blank_\">" + procInstId + "</a>") : procInstId) + "</td>" +
								"<td>"+ res[i].status +"</td>"+
								"<td>"+ (res[i].createdTimestamp == undefined ? '' : res[i].createdTimestamp) + "</td>"+
								"<td>"+ (res[i].startedByWorker == undefined ? '' : res[i].startedByWorker + workerIP) + "</td>"+
								"<td>"+ procStartTime + "</td>"+
								"<td>"+ procEndTime + procDuration + "</td>"+
								"<td>"+ (res[i].inputVariables == undefined ? '' : res[i].inputVariables) + "</td>"+
							"</tr>")
							);
						}
						table.draw();
						
						$("#proc-log div.ajax-spinner").hide();
					});
			});

	}

	// ---------------------------------------------------------------
	// Updates the list of active items in the Actions drop-down list
	//
	function updateActionList() {
		console.log("updateActionList called");

		var table = $("#processes-table").DataTable();
		
		var selectedRows = table.rows( { selected: true } );

		var numSelected = selectedRows.count();
		var numDisabledSelected = 0;
		var numPendingSelected = 0;
		var numIncidentSelected = 0;
		var numFailedToStartSelected = 0;
		var numFailedSelected = 0;
		var numComplete = 0;

		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			switch (data[6]) {
				case 'disabled':
					numDisabledSelected++;
					break;
				case 'pending':
					numPendingSelected++;
					break;
				case 'incident':
					numIncidentSelected++;
					break;
				case 'failed_to_start':
					numFailedToStartSelected++;
					break;
				case 'failed':
					numFailedSelected++;
					break;
				case 'complete':
					numComplete++;
					break;
			}
		} );

		if (numSelected > 0) {
			var disabled      = numDisabledSelected      == numSelected;
			var pending       = numPendingSelected       == numSelected;
			var incident      = numIncidentSelected      == numSelected;
			var failedToStart = numFailedToStartSelected == numSelected;
			var failed        = numFailedSelected        == numSelected;
		}

		// Disable everything
		$("#action_disable").addClass("disabled");
		$("#action_disable").removeClass("enabled");
		$("#action_enable").addClass("disabled");
		$("#action_enable").removeClass("enabled");
		$("#action_retry_incident").addClass("disabled");
		$("#action_retry_incident").removeClass("enabled");
		$("#action_retry_failed_to_start").addClass("disabled");
		$("#action_retry_failed_to_start").removeClass("enabled");
		$("#action_mark_as_resolved").addClass("disabled");
		$("#action_mark_as_resolved").removeClass("enabled");
		$("#action_open_selected_new_tabs").addClass("disabled");
		$("#action_open_selected_new_tabs").removeClass("enabled");
		$("#action_copy_all_selected_history_links").addClass("disabled");
		$("#action_copy_all_selected_history_links").removeClass("enabled");
		$("#action_download_selected_json").addClass("disabled");
		$("#action_download_selected_json").removeClass("enabled");
		$("#action_download_selected_csv").addClass("disabled");
		$("#action_download_selected_csv").removeClass("enabled");

		// Remove hrefs from the anchor tags
		$("#action_disable_atag").removeAttr("href");
		$("#action_enable_atag").removeAttr("href");
		$("#action_retry_incident_atag").removeAttr("href");
		$("#action_retry_failed_to_start_atag").removeAttr("href");
		$("#action_mark_as_resolved_atag").removeAttr("href");
		$("#action_open_selected_new_tabs_atag").removeAttr("href");
		$("#action_copy_all_selected_history_links_atag").removeAttr("href");
		$("#action_download_selected_json_atag").removeAttr("href");
		$("#action_download_selected_csv_atag").removeAttr("href");

		// Enable the right one

		// only disabled rows are selected
		if (disabled) {
			$("#action_enable").removeClass("disabled");
			$("#action_enable_atag").attr("href", "javascript:action_enable_rows();");
		}
		// only pending rows are selected
		else if (pending) {
			$("#action_disable").removeClass("disabled");
			$("#action_disable_atag").attr("href", "javascript:action_disable_rows();");
		}
		// only incident rows are selected
		else if (incident) {
			$("#action_retry_incident").removeClass("disabled");
			$("#action_retry_incident_atag").attr("href", "javascript:action_retry_incident_rows()");
		}
		// only failedToStart rows are selected
		else if (failedToStart) {
			$("#action_retry_failed_to_start").removeClass("disabled");
			$("#action_retry_failed_to_start_atag").attr("href", "javascript:action_retry_failed_to_start();");
		}
		// only failed rows are selected
		else if (failed) {
			$("#action_mark_as_resolved").removeClass("disabled");
			$("#action_mark_as_resolved_atag").attr("href", "javascript:action_mark_as_resolved();");
		}

		if ((numSelected > 0 && numPendingSelected === 0)) {
			$("#action_open_selected_new_tabs").removeClass("disabled");
			$("#action_open_selected_new_tabs_atag").attr("href", "javascript:action_open_selected_new_tabs();");
			$("#action_copy_all_selected_history_links").removeClass("disabled");
			$("#action_copy_all_selected_history_links_atag").attr("href", "javascript:action_copy_all_selected_history_links();");
			$("#action_download_selected_json").removeClass("disabled");
			$("#action_download_selected_json_atag").attr("href", "javascript:downloadSelectedJSON();");
			$("#action_download_selected_csv").removeClass("disabled");
			$("#action_download_selected_csv_atag").attr("href", "javascript:downloadSelectedCSV();");
		}
		
		// Execute adaptation actions if any
		updateAdaptationActionList();
	}

	function action_open_selected_new_tabs() {
		var table = $("#processes-table").DataTable();
		var selectedRows = table.rows( { selected: true } );
		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			window.open("/${base}/history?procInstId=" + data[5], "_blank");
		} );
	}

	function action_copy_all_selected_history_links() {
		var table = $("#processes-table").DataTable();
		const protocol = window.location.protocol;
		const host = window.location.host;
		var selectedRows = table.rows( { selected: true } );
		var links = "";
		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			links += protocol + "://" + host + "/${base}/history?procInstId=" + data[5] + "\n";
		} );
		navigator.clipboard.writeText(links);
	}


	// -------------------------------------------------------------------------------
	// Function fired when user clicks on "Enable Selected Rows..." in drop-down list
	// 
	function action_enable_rows() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/makeDisabledRowsPending",
			dataType: "json",
			Accept : "application/json",
			contentType: "application/json", 
			data: JSON.stringify(getSelectedRowUuids())
		})
		.done(function(msg) {
			$("#action_msg").html(msg.message);
			renderRows();
			resetCheckboxes();
		})
		.fail(function(xhr, err) { 
			$("#action_msg").html(xhr.responseTextmsg.message);
		});
	}


	// --------------------------------------------------------------------------------
	// Function fired when user clicks on "Disable Selected Rows..." in drop-down list
	// 
	function action_disable_rows() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/makePendingRowsDisabled",
			Accept : "application/json",
			contentType: "application/json", 
			dataType: "json",
			data: JSON.stringify(getSelectedRowUuids())
		})
		.done(function(msg) {
			$("#action_msg").html(msg.message);
			renderRows();
			resetCheckboxes();
		})
		.fail(function(xhr, err) { 
			$("#action_msg").html(xhr.responseTextmsg.message);
		});
	}


	// --------------------------------------------------------------------------------
	// Function fired when user clicks on "Retry Selected Incident Rows..." in drop-down list
	//
	function action_retry_incident_rows() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/retryIncidentRows",
			Accept : "application/json",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(getSelectedRowUuids())
		})
		.done(function(msg) {
			$("#action_msg").html(msg.message);
			location.reload();
		})
		.fail(function(xhr, err) {
			$("#action_msg").html(xhr.responseTextmsg.message);
		});
	}

	// --------------------------------------------------------------------------------
	// Function fired when user clicks on "Retry Selected Failed to Start Rows..." in drop-down list
	//
	function action_retry_failed_to_start() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/retryFailedToStart",
			Accept : "application/json",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(getSelectedRowUuids())
		})
		.done(function(msg) {
			$("#action_msg").html(msg.message);
			location.reload();
		})
		.fail(function(xhr, err) {
			$("#action_msg").html(xhr.responseTextmsg.message);
		});
	}

	// --------------------------------------------------------------------------------
	// Function fired when user clicks on "Mark Selected Failed Rows As Resolved..." in drop-down list
	//
	function action_mark_as_resolved() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/processes/markResolved",
			Accept : "application/json",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(getSelectedRowUuids())
		})
		.done(function(msg) {
			$("#action_msg").html(msg.message);
			location.reload();
		})
		.fail(function(xhr, err) {
			$("#action_msg").html(xhr.responseTextmsg.message);
		});
	}

	// ------------------------------------
	// Clear checked selections (resolves timing issues after an action is chosen)
	//
	function resetCheckboxes() {
		$("input[status]:checked").attr("checked", false)
		updateActionList();
	}
	
	// ------------------------------------
	// Get array of selected rows (uuids)
	//
	function getSelectedRowUuids() {
		var selectedRowUuids = [];
		//
		// For each selected row...
		//
		var table = $('#processes-table').DataTable();
		var selectedRows = table.rows( { selected: true } );

		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var html = this.node();
			var status = $(html).children("td:first").attr("status");
			var uuid = $(html).children("td:first").attr("uuid");
			var procInstId = $(html).children("td:first").attr("procInstId");
			var data = this.data();
			switch (status) {
				case 'disabled':
					selectedRowUuids.push(uuid);
					break;
				case 'pending':
					selectedRowUuids.push(uuid);
					break;
				case 'incident':
					selectedRowUuids.push(procInstId);
					break;
				case 'failedToStart':
					selectedRowUuids.push(uuid);
					break;
				case 'fail':
					selectedRowUuids.push(procInstId);
					break;
			}
		});
		return selectedRowUuids;
	}

	function downloadSelectedJSON() {
		var mainJSON = {};
		//get selected rows
		var table = $('#processes-table').DataTable();
		var selectedRows = table.rows( { selected: true } );
		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			var procInstId = data[5];
			var json = getInstanceJSON(procInstId);
			mainJSON[procInstId] = json;
		});
		$.fn.dataTable.fileSave(
			new Blob( [ JSON.stringify(mainJSON) ] ),
			'processes-' + moment().format('MMM-DD-YYYY-hh-mm-a') + '.json'
		);
	}

	function downloadSelectedCSV() {
		var mainCSV = `"process_definition","process_instance","time stamp","type","source","details"\r\n`;
		//get selected rows
		var table = $('#processes-table').DataTable();
		var selectedRows = table.rows( { selected: true } );
		selectedRows.every( function ( rowIdx, tableLoop, rowLoop ) {
			var data = this.data();
			var procInstId = data[5];
			var csv = getInstanceCSV(procInstId);
			mainCSV += csv;
		});
		$.fn.dataTable.fileSave(
			new Blob( [ mainCSV ] ),
			'processes-' + moment().format('MMM-DD-YYYY-hh-mm-a') + '.csv'
		);
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

	$("#json-bttn").click(function(e) {
		e.preventDefault();
		downloadListJSON();
	});

	function downloadListJSON() {
		var dt = $('#processes-table').DataTable();
		//number of rows
		var numRows = dt.rows({selected:true}).count();
		var jsonFile = {};
		var processes = {};
		jsonFile["num_rows_loaded"] = rowsTotal;

		if (numRows === 0) {
			dt.rows({search:'applied'}).every( function ( rowIdx, tableLoop, rowLoop ) {
				var data = this.data();
				var thisProcJSON = {};
				var startedOnWorker = "";
				var workerIP = "";
				var duration = "";
				var process_end = "";
				var inputVars = "";

				if (data[8] !== "") {
					startedOnWorker = data[6].substring(0, startedOnWorker.indexOf("<br><b>"));
					workerIP = data[6].substring(workerIP.indexOf("</b>") + 4, workerIP.length);
				} else {
					startedOnWorker = data[6];
				}

				if (data[8] !== "") {
					duration = data[8];
					//get everything after <br><i> but before </i>
					duration = duration.substring(duration.indexOf("<br><i>") + 7, duration.indexOf("</i>"));
					process_end = data[8].substring(0, data[8].indexOf("<br><i>")-1);
				}

				if (data[9] !== "") {
					inputVars = data[9].replaceAll("<br>", ", ");
				}



				thisProcJSON["definition_key"] = data[2];
				thisProcJSON["process_instance_id"] = data[3];
				thisProcJSON["status"] = data[4];
				thisProcJSON["schedule_queued_time"] = data[5];
				thisProcJSON["started_on_worker"] = startedOnWorker;
				thisProcJSON["worker_ip"] = workerIP;
				thisProcJSON["process_start"] = data[7];
				thisProcJSON["process_end"] = process_end;
				thisProcJSON["duration"] = duration;
				thisProcJSON["input_variables"] = inputVars;

				processes[data[3]] = thisProcJSON;
			} );
		} else {
			dt.rows({selected: true, search:'applied'}).every( function ( rowIdx, tableLoop, rowLoop ) {
				var data = this.data();
				var thisProcJSON = {};
				var startedOnWorker = "";
				var workerIP = "";
				var duration = "";
				var process_end = "";
				var inputVars = "";

				if (data[8] !== "") {
					startedOnWorker = data[6].substring(0, startedOnWorker.indexOf("<br><b>"));
					workerIP = data[6].substring(workerIP.indexOf("</b>") + 4, workerIP.length);
				} else {
					startedOnWorker = data[6];
				}

				if (data[8] !== "") {
					duration = data[8];
					//get everything after <br><i> but before </i>
					duration = duration.substring(duration.indexOf("<br><i>") + 7, duration.indexOf("</i>"));
					process_end = data[8].substring(0, data[8].indexOf("<br><i>")-1);
					
				}

				if (data[9] !== "") {
					inputVars = data[9].replaceAll("<br>", ", ");
				}

				thisProcJSON["definition_key"] = data[2];
				thisProcJSON["process_instance_id"] = data[3];
				thisProcJSON["status"] = data[4];
				thisProcJSON["schedule_queued_time"] = data[5];
				thisProcJSON["started_on_worker"] = startedOnWorker;
				thisProcJSON["worker_ip"] = workerIP;
				thisProcJSON["process_start"] = data[7];
				thisProcJSON["process_end"] = process_end;
				thisProcJSON["duration"] = duration;
				thisProcJSON["input_variables"] = inputVars;

				processes[data[3]] = thisProcJSON;
			} );
		}
		jsonFile["processes"] = processes;
		console.log(jsonFile);
		$.fn.dataTable.fileSave(
			new Blob( [ JSON.stringify(jsonFile) ] ),
			'processes_export.json'
		);
	}
</script>
<script src="/${base}/js/cws.js"></script>

</body>
</html>