<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Processes</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
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
		.dataTables_wrapper .filter .dataTables_filter{float:right; margin-top: 15px; display: inline; margin-right: 15px;}
		.dataTables_wrapper .length .dataTables_length{float:left; display: inline; margin-top: 15px;}
		.dataTables_wrapper .buttons .dt-buttons{float:left; display: inline; margin-top: 15px; margin-left: 15px; margin-right: 15px;}
		.dataTables_wrapper .action-button {margin-top: 15px; margin-left: -15px; margin-right: 15px;}
		.dataTables_wrapper .dtsb-titleRow {display: none;}
		.dataTables_wrapper .dtsb-group {margin-bottom: -15px !important; margin-top: 8px;}
	</style>
	<style type="text/css">
		#processes-table {
			font-size: 90%;
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
						<th></th>
						<th>Initiator</th>
						<th>Definition Key</th>
						<th>Proc Inst ID</td>
						<th>Status</th>
						<th>Schedule Queued Time</th>
						<th>Started on Worker</th>
						<th>Process Start</th>
						<th>Process End</th>
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
	const qstringVar = "CWS_DASH_PROC_QSTRING-" + username;
	const rowsToLoadVar = "CWS_DASH_PROC_ROWS-" + username;

	var params = {};
	var rows;
	var loadRows = 5000;

	$( document ).ready(function() {
		//get our current url
		var currentUrl = window.location.href;
		//get our local storage url
		var localStorageUrl = localStorage.getItem(qstringVar);
		//check if a cookie has been stored (indicating we can restore state)
		if(localStorageUrl != null) {
			//remove everything before ?
			currentUrl = currentUrl.substring(currentUrl.indexOf("?"));
			//compare against what is in local storage
			if (currentUrl != localStorageUrl) {
				//check if we are viewing subprocs (if we are, we don't want to restore state)
				var subprocs = currentUrl.indexOf("superProcInstId") + 16;
				if(subprocs === "null") {
					//if they are different, go to the one in local storage (essentially restoring from last time used)
					window.location="/${base}/processes" + localStorageUrl;
				}
			}
		}

		if (localStorage.getItem(rowsToLoadVar) != null) {
			loadRows = parseInt(localStorage.getItem(rowsToLoadVar));
		}
		
		displayMessage();

		renderRows(loadRows);

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
			dom: "Q<'row'<'col-sm-auto buttons'B><'col-sm-1 action-button'><'col-sm-4 length'l><'col-sm-auto filter'f>>" + "tip",
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
						$("#processes-table").DataTable().rows().select();
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

		var table = $("#processes-table").DataTable();
		table.on( 'select', function ( e, dt, type, indexes ) {
			updateActionList();
		} );

		$('<button id="menu3" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Actions &nbsp;' 
			+ '<span class="caret"></span>'
			+ '</button>'
			+ '<ul id="action-list" class="dropdown-menu test" role="menu" aria-labelledby="menu3">'
    		+ `<li id="action_disable" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_disable_rows();");">Disable selected rows (all rows selected must be 'pending')</a></li>`
    		+ `<li id="action_enable" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_enable_rows();">Enable selected rows (all rows selected must be 'disabled')</a></li>`
    		+ `<li id="action_retry_incident" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_retry_incident_rows();">Retry all selected incident rows (all rows selected must be 'incident')</a></li>`
    		+ `<li id="action_retry_failed_to_start" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_retry_failed_to_start();">Retry all selected failed to start rows (all rows selected must be 'failedToStart')</a></li>`
    		+ `<li id="action_mark_as_resolved" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_mark_as_resolved();">Mark all selected failed rows as resolved (all rows selected must be 'fail')</a></li>`
  			+ `<#include "adaptation-process-actions.ftl">`
  			+ `</ul>`).appendTo(".action-button");
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
		//console.log("/${base}/rest/processes/getInstances"+qstr);
		params = getQueryString();
		var numProcs = 0;
		var requestProc = 0;

		//get the number of instances
		$.get("/${base}/rest/processes/getInstancesSize"+qstr,
			function(res) {
				numProcs = res;
				console.warn("numProcs: " + numProcs);
				console.log("numProcs: " + numProcs);
				if (rowsToLoad === -1) {
					requestProc = numProcs;
				} else {
					requestProc = rowsToLoad;
				}
				//
				// GET THE PROCESS INSTANCE, 
				// AND RENDER THEM...
				//
				console.log("requestProc: " + requestProc);
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
							table.row.add(
							$("<tr id=\""+i+"\" class=\"tr-"+ res[i].status +"\" procInstId=\"" + procInstId + "\">"+
								"<td status=\"" + res[i].status + "\" uuid=\"" + res[i].uuid + "\" procInstId=\"" + res[i].procInstId + "\"></td>" +
								"<td><button onclick=\"viewHistory('" + procInstId + "')\" class=\"btn btn-default btn-sm\">History</button></td>" +
								"<td><button onclick=\"viewSubProcs('" + procInstId + "')\" class=\"btn btn-default btn-sm\">Subprocs</button></td>" +
								"<td id=\"row-" + i + "initiationKey\">"+ (res[i].initiationKey == undefined ? '' : res[i].initiationKey) + "</td>" +
								"<td>"+ res[i].procDefKey +"</td>"+
								"<td>"+ (res[i].status == 'incident' ? ("<a href=\""+ incidentUrl +"\" target=\"blank_\">" + procInstId + "</a>") : procInstId) + "</td>" +
								"<td>"+ res[i].status +"</td>"+
								"<td>"+ (res[i].createdTimestamp == undefined ? '' : res[i].createdTimestamp) + "</td>"+
								"<td>"+ (res[i].startedByWorker == undefined ? '' : res[i].startedByWorker) + "</td>"+
								"<td>"+ (res[i].procStartTime == undefined ? '' : res[i].procStartTime) + "</td>"+
								"<td>"+ (res[i].procEndTime == undefined ? '' : res[i].procEndTime) + "</td>"+
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

		// Enable the right one

		// only disabled rows are selected
		if (disabled) {
			$("#action_enable").removeClass("disabled");
		}
		// only pending rows are selected
		else if (pending) {
			$("#action_disable").removeClass("disabled");
		}
		// only incident rows are selected
		else if (incident) {
			$("#action_retry_incident").removeClass("disabled");
		}
		// only failedToStart rows are selected
		else if (failedToStart) {
			$("#action_retry_failed_to_start").removeClass("disabled");
		}
		// only failed rows are selected
		else if (failed) {
			$("#action_mark_as_resolved").removeClass("disabled");
		}
		
		// Execute adaptation actions if any
		updateAdaptationActionList();
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
		console.log(selectedRows.count());

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
</script>
<script src="/${base}/js/cws.js"></script>

</body>
</html>