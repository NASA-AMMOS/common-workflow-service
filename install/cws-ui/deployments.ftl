<html>

<head>
	<meta charset="utf-8">
	<title>CWS - Deployments</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="/${base}/js/bootstrap-toggle.min.js"></script>
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css" />
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<style>
		.dataTables_wrapper .filter .dataTables_filter {
			float: right;
			padding-top: 15px;
			display: inline;
			margin-right: 15px;
		}

		.dataTables_wrapper .mylength .dataTables_length {
			float: right
		}

		.dataTables_wrapper .download-button {
			padding-top: 15px;
		}

		.above-table-div {
			display: flex;
			flex-direction: row;
			flex-wrap: nowrap;
			justify-content: space-between;
			align-items: flex-end;
			gap: 10px;
			margin-bottom: 5px;
			margin-top: 15px;
		}

		.above-table-buttons {
			display: flex;
			flex-direction: row;
			flex-wrap: nowrap;
			justify-content: flex-end;
			align-items: flex-end;
			gap: 10px;
		}

		.btn-icon {
			margin-right: 5px;
		}

		.below-table-div {
			display: flex;
			flex-direction: row;
			flex-wrap: nowrap;
			justify-content: space-between;
			align-items: flex-start;
			gap: 10px;
			margin-bottom: 5px;
		}
	</style>
	<script>

		//STATE PERSISTANCE CONSTS
		const username = "username"; //temporary, hardcoded value for now
		const lastNumHoursVar = "CWS_DASH_DEPLOY_LAST_NUM_HOURS-" + username;
		const refreshRateVar = "CWS_DASH_DEPLOY_REFRESH_RATE-" + username;
		const hideSuspendedProcVar = "CWS_DASH_DEPLOY_HIDE_SUS-" + username;

		var statsVal = {};
		var statsTotalVal = {};

		var refreshing = false;
		var pageRefId = 0;
		var refreshRate = 5000;
		var lastNumHours = 24;
		var deleteProcDefName = "";
		var idling = false;
		var idleTimer = 0;
		var idleInterval = 600000 // 10 minutes (10 * 60 * 1000)

		<#list procDefs as x>
			statsVal.${x.key} = {pending:'...', disabled:'...', active:'...', completed:'...', error:'...', fts:'...', incident:'...'};
		</#list>


			function refreshStatUI(name, statsCounts) {

				// REFRESH THE TEXTUAL STATS SUMMARY
				//
				var statTotal =
					statsCounts.pending +
					statsCounts.disabled +
					statsCounts.active +
					statsCounts.completed +
					statsCounts.error +
					statsCounts.fts +
					statsCounts.incident;

				var instanceTextString = "";
				if (statsCounts.pending) {
					instanceTextString += "<b>pending</b>:&nbsp;" + statsCounts.pending + "&nbsp;&nbsp;";
				}
				if (statsCounts.disabled) {
					instanceTextString += "<b>disabled</b>:&nbsp;" + statsCounts.disabled + "&nbsp;&nbsp;";
				}
				if (statsCounts.active) {
					instanceTextString += "<b>running</b>:&nbsp;" + statsCounts.active + "&nbsp;&nbsp;";
				}
				if (statsCounts.completed) {
					instanceTextString += "<b>completed</b>:&nbsp;" + statsCounts.completed + "&nbsp;&nbsp;";
				}
				if (statsCounts.error) {
					instanceTextString += " <b>failed</b>:&nbsp;" + statsCounts.error + "&nbsp;&nbsp;";
				}
				if (statsCounts.fts) {
					instanceTextString += "<b>failed-start</b>:&nbsp;" + statsCounts.fts + "&nbsp;&nbsp;";
				}
				if (statsCounts.incident) {
					instanceTextString += "<b>incidents</b>:&nbsp;" + statsCounts.incident + "&nbsp;&nbsp;";
				}


				if (statTotal > 0) {
					$("#stat-txt-" + name).html(instanceTextString);
				} else {
					$("#stat-txt-" + name).html(
						"No stats for this process"
					);
				}

				//calculate the percentage of each dimension
				var statsPercent = {};

				statsPercent.pending = statsCounts.pending / statTotal;
				//alert(statsCounts.disabled);
				statsPercent.disabled = statsCounts.disabled / statTotal;
				statsPercent.active = statsCounts.active / statTotal;
				statsPercent.completed = statsCounts.completed / statTotal;
				statsPercent.error = statsCounts.error / statTotal;
				statsPercent.fts = statsCounts.fts / statTotal;
				statsPercent.incident = statsCounts.incident / statTotal;

				//set the minimum percentage of each dimension to 1.5 if it's smaller than 1.5
				if (statsPercent.pending < 0.015 && statsPercent.pending > 0) {
					statsPercent.pending = 0.015;
				}
				if (statsPercent.disabled < 0.015 && statsPercent.disabled > 0) {
					statsPercent.disabled = 0.015;
				}
				if (statsPercent.active < 0.015 && statsPercent.active > 0) {
					statsPercent.active = 0.015;
				}
				if (statsPercent.completed < 0.015 && statsPercent.completed > 0) {
					statsPercent.completed = 0.015;
				}
				if (statsPercent.error < 0.015 && statsPercent.error > 0) {
					statsPercent.error = 0.015;
				}
				if (statsPercent.fts < 0.015 && statsPercent.fts > 0) {
					statsPercent.fts = 0.015;
				}
				if (statsPercent.incident < 0.015 && statsPercent.incident > 0) {
					statsPercent.incident = 0.015;
				}

				/**
				* because of the possible additional percentage, calculate the number of
				* each dimension corresponding to the adjusted percentage
				**/
				var statsTemp = {};

				statsTemp.pending = statsPercent.pending * statTotal;
				statsTemp.disabled = statsPercent.disabled * statTotal;
				statsTemp.error = statsPercent.error * statTotal;
				statsTemp.active = statsPercent.active * statTotal;
				statsTemp.completed = statsPercent.completed * statTotal;
				statsTemp.fts = statsPercent.fts * statTotal;
				statsTemp.incident = statsPercent.incident * statTotal;

				statTotal = statsTemp.pending + statsTemp.disabled + statsTemp.error + statsTemp.active +
					statsTemp.completed + statsTemp.fts + statsTemp.incident;

				/**
				* recalculate percentage distribution using the recalculated values
				**/
				statsPercent.pending = statsTemp.pending / statTotal * 100;
				statsPercent.disabled = statsTemp.disabled / statTotal * 100;
				statsPercent.active = statsTemp.active / statTotal * 100;
				statsPercent.completed = statsTemp.completed / statTotal * 100;
				statsPercent.error = statsTemp.error / statTotal * 100;
				statsPercent.fts = statsTemp.fts / statTotal * 100;
				statsPercent.incident = statsTemp.incident / statTotal * 100;

				//set the width of each bar
				$("#stat-bar-" + name + " div.bar-pending").css('width', statsPercent.pending + '%');
				$("#stat-bar-" + name + " div.bar-disabled").css('width', statsPercent.disabled + '%');
				$("#stat-bar-" + name + " div.bar-active").css('width', statsPercent.active + '%');
				$("#stat-bar-" + name + " div.bar-completed").css('width', statsPercent.completed + '%');
				$("#stat-bar-" + name + " div.bar-error").css('width', statsPercent.error + '%');
				$("#stat-bar-" + name + " div.bar-failedToStart").css('width', statsPercent.fts + '%');
				$("#stat-bar-" + name + " div.bar-incident").css('width', statsPercent.incident + '%');
				//set the tooltip text of each bar
				$("#stat-bar-" + name + " div.bar-pending").attr('data-original-title', statsCounts.pending + " Pending");
				$("#stat-bar-" + name + " div.bar-disabled").attr('data-original-title', statsCounts.disabled + " Disabled");
				$("#stat-bar-" + name + " div.bar-active").attr('data-original-title', statsCounts.active + " Running");
				$("#stat-bar-" + name + " div.bar-completed").attr('data-original-title', statsCounts.completed + " Completed");
				$("#stat-bar-" + name + " div.bar-error").attr('data-original-title', statsCounts.error + " Failed");
				$("#stat-bar-" + name + " div.bar-failedToStart").attr('data-original-title', statsCounts.fts + " Failed to Start");
				$("#stat-bar-" + name + " div.bar-incident").attr('data-original-title', statsCounts.incident + " Incidents");
			}

		function handleDeleteProcDef(proc_def_key) {

			deleteProcDefName = proc_def_key;

			var html = "<b>" + deleteProcDefName + "</b>";

			$('#delete-proc-name').html(html);
			$('#fail-delete-proc-name').html(html);

			$("#deleting-message-container").css("display", "none");
			$("#delete-proc-def").prop('disabled', false);
			$("#delete-proc-def-modal").modal('show');
		}

		function getDeleteErrorMessage(message) {

			if (message.indexOf("(Accepting new)") > 0) {
				return "You must disable this process definition on all workers before deleting.";
			}
			else if (message.indexOf("(Not found)") > 0) {
				return "Process definition was not found.  Maybe it was already deleted.  Try refreshing the page...";
			}
			else if (message.indexOf("(Running)") > 0) {
				return "Before deleting this process definition, you must wait for it to finish running or stop it, then disable it on all workers.";
			}

			return "An unknown error occured.";
		}


		function deleteProcDef(proc_def_key) {
			$.ajax({
				url: "/${base}/rest/processes/processDefinition/" + proc_def_key + "/undeploy",
				success: function (data) {

					if (data.status === "SUCCESS") {

						var tableRow = $("td").filter(function () {
							return $(this).text() === proc_def_key;
						}).closest("tr");

						if (tableRow) {
							tableRow.remove();
						}

						deleteProcDefEsLogs(proc_def_key);
					}
					else {

						var deleteError = getDeleteErrorMessage(data.message);

						$("#delete-error-message").html(deleteError);
						$("#delete-proc-def-modal").modal('hide');
						$("#delete-error-modal").modal('show');
					}
				},
				error: function () {
					$("#delete-proc-def-modal").modal('hide');
					$("#ajax-error-modal").modal('show');
				}
			});
		}

		function deleteProcDefEsLogs(proc_def_key) {
			$.ajax({
				type: "DELETE",
				url: "/${base}/rest/logs/delete/" + proc_def_key,
				success: function (data) {

					if (data.status !== "SUCCESS") {

						var deleteError = "This process definition was deleted, but could not delete its log data.";

						$("#delete-error-message").html(deleteError);
						$("#delete-error-modal").modal('show');
					}

					$("#delete-proc-def-modal").modal('hide');
				},
				error: function (msg) {
					$("#delete-proc-def-modal").modal('hide');
					$("#ajax-error-modal").modal('show');
				}
			});
		}

		function refreshStats() {

			if (refreshing) return;

			refreshing = true;
			//grab the value here so we don't have to do it multiple times
			var statsCookieValue = parseInt(localStorage.getItem(lastNumHoursVar));

			$.ajax({
				url: "/${base}/rest/stats/processInstanceStatsJSON",
				data: statsCookieValue ? "lastNumHours=" + statsCookieValue : "",
				success: function (data) {

					statsTotalVal.pending = 0;
					statsTotalVal.disabled = 0;
					statsTotalVal.active = 0;
					statsTotalVal.completed = 0;
					statsTotalVal.error = 0;
					statsTotalVal.fts = 0;
					statsTotalVal.incident = 0;

					var updatedKeys = [];

					$.each(data, function (key, val) {
						//
						// SKIP AN INVALID KEY
						// THIS IS NECESSARY, IF SOMEHOW AN INVALID PROC GOT SCHEDULED OR PUT IN A DATABASE ROW
						//
						if (statsVal[key] == undefined) { return true; }

						// UPDATE THE STATS VALUE ARRAYS
						//
						statsVal[key].pending = parseInt(val.pending) | 0;
						statsVal[key].disabled = parseInt(val.disabled) | 0;
						statsVal[key].active = parseInt(val.running) | 0;
						statsVal[key].completed = (parseInt(val.complete) | 0) + (parseInt(val.resolved) | 0);
						statsVal[key].error = parseInt(val.fail) | 0;
						statsVal[key].fts = parseInt(val.failedToStart) | 0;
						statsVal[key].incident = parseInt(val.incident) | 0;

						// UPDATE THE TOTAL STATS VALUE
						statsTotalVal.pending += statsVal[key].pending;
						statsTotalVal.disabled += statsVal[key].disabled;
						statsTotalVal.active += statsVal[key].active;
						statsTotalVal.completed += statsVal[key].completed;
						statsTotalVal.error += statsVal[key].error;
						statsTotalVal.fts += statsVal[key].fts;
						statsTotalVal.incident += statsVal[key].incident;

						refreshStatUI(key, statsVal[key]);

						updatedKeys.push(key);
					});

					// Clear all keys that are not in the output
					$.each(statsVal, function (key, val) {

						if (updatedKeys.indexOf(key) === -1) {

							statsVal[key].pending = 0;
							statsVal[key].disabled = 0;
							statsVal[key].active = 0;
							statsVal[key].completed = 0;
							statsVal[key].error = 0;
							statsVal[key].fts = 0;
							statsVal[key].incident = 0;

							refreshStatUI(key, statsVal[key]);
						}
					});

					refreshStatUI('cws-reserved-total', statsTotalVal);

					refreshing = false;
				},
				error: function () {
					clearInterval(pageRefId);
					$("#ajax-error-modal").modal('show');

					refreshing = false;
				}
			});
		}
		$(document).ready(function () {
			// DISPLAY MESSAGE AT TOP OF PAGE
			//
			if ($("#statusMessageDiv:contains('ERROR:')").length >= 1) {
				$("#statusMessageDiv").css("color", "red");
			}
			else {
				$("#statusMessageDiv").css("color", "green");
				if ($('#statusMessageDiv').html().length > 9) {
					$('#statusMessageDiv').fadeOut(refreshRate, "linear");
				}
			}

			// State persistance for refresh rate and show stats for last x hours
			if (localStorage.getItem(refreshRateVar) !== null) {
				$("#refresh-rate").val(localStorage.getItem(refreshRateVar) / 1000);
			} else {
				localStorage.setItem(refreshRateVar, 5000);
				$("#refresh-rate").val("5");
			}
			if (localStorage.getItem(lastNumHoursVar) !== null) {
				$("#stats-last-num-hours").val(localStorage.getItem(lastNumHoursVar));
			} else {
				localStorage.setItem(lastNumHoursVar, 24);
				$("#stats-last-num-hours").val(24);
			}

			$("#process-table").DataTable({
				columnDefs: [
					{ orderable: false, targets: 5 },
					{ orderable: false, targets: 3 }
				],
				"paging": false,
				//filter is top left, length is top right, info is bottom left, pagination is bottom right
				dom: "<'above-table-div'<'above-table-buttons'>f>"
					+ "t"
					+ "<'below-table-div'i>",
				//dom: "<'row'<'col-sm-2 download-button'><'col-sm-10 filter'f>>" + "tip",
			});

			$('<button id="download-btn" class="btn btn-primary" onclick="downloadJSON()"><i class="glyphicon glyphicon-save btn-icon"></i>Download (JSON)</button>').appendTo(".above-table-buttons");

			refreshStats();
			pageRefId = setInterval(pageRefresh, parseInt(localStorage.getItem(refreshRateVar)));
			idleTimer = setInterval(idleMode, idleInterval);

			$("#resume-refresh").click(function () {
				$("#page-ref-modal").modal('hide');
				idling = false;
			});

			$("#delete-proc-def").click(function () {

				$("#delete-proc-def").prop('disabled', true);
				$("#deleting-message-container").css("display", "flex");

				deleteProcDef(deleteProcDefName);
			});

			$("#open-file-div").click(function () {
				$("#file-input").click();
			});
			$("#file-input").on('change', function () {
				//console.log($("#file-input").val());
				$("#open-file-div label").text($("#file-input").val().replace("C:\\fakepath\\", ""));
			});

			// When entering a div tag, reset idle timer to prevent idle mode
			$("div").mouseenter(function (e) {
				clearInterval(idleTimer);
				idleTimer = setInterval(idleMode, idleInterval);
			});

			adjustWorkersButton();
		});

		function pageRefresh() {
			if (!idling) {
				refreshStats();
			}
		}

		// Stop refreshing the page
		function idleMode() {
			if (!idling) {
				idling = true;
				$("#page-ref-modal").modal('show');
			}
		}

		$(function () {
			$('[data-toggle="tooltip"]').tooltip()
		})

	</script>

	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->

	<style type="text/css">
		.status-div {
			padding: 0px 6px 6px 6px;
		}

		#suspend-div {
			height: 50px;
			float: right;
		}

		.bar-failedToStart {
			background-color: #D8860B;
		}

		.bar-incident {
			background-color: #C347ED;
			/*#F142F4;*/
		}

		#workers-div {
			overflow: auto;
			max-height: 500px;
			margin-left: 20px;
		}

		.stat-txt {
			font-size: 0.7em;
			font-family: monospace;
		}

		.progress-bar-warning {
			background-color: #E7B814;
		}

		.progress-bar-disabled {
			background-color: #CCCCCC;
		}

		.progress-bar-info {
			background-color: #4363CF;
		}

		#selAll-label {
			cursor: pointer;
		}

		/*	#workers-div div{
		margin: 10px;
		float:left; 
		width:155px;
	}
	#workers-div div input{
		margin: 0 5px;
		/*transform:scale(1.2);*/
		}

		#workers-div div label {
			margin: 0 5px;
			cursor: pointer;
		}

		*/ #deploy-table td {
			padding: 10px;
		}

		#process-table {
			margin-top: 2rem;
		}

		#process-table tr td {
			vertical-align: middle;
			padding: 4px 8px;
			min-width: 70px;
		}

		#process-table tr td:nth-child(1) {
			min-width: 200px;
			overflow: hidden;
			text-wrap: none
		}

		#process-table tr td:nth-child(3) {
			text-align: center;
			width: 70px;
		}

		#process-table tr td:nth-child(4) {
			text-align: center;
			width: 70px;
		}

		#process-table tr td:nth-child(5) {
			text-align: center;
			width: 70px;
		}

		.progress {
			margin: 0px;
		}

		.w-down {
			color: #bbb;
		}

		#deleting-message-container {
			margin-top: 30px;
			display: none;
			justify-content: center;
			align-items: center;

		}

		.loader {
			border: 10px solid #f3f3f3;
			border-radius: 50%;
			border-top: 10px solid #3498db;
			width: 40px;
			height: 40px;
			-webkit-animation: spin 1s linear infinite;
			/* Safari */
			animation: spin 1s linear infinite;
		}

		/* Safari */
		@-webkit-keyframes spin {
			0% {
				-webkit-transform: rotate(0deg);
			}

			100% {
				-webkit-transform: rotate(360deg);
			}
		}

		@keyframes spin {
			0% {
				transform: rotate(0deg);
			}

			100% {
				transform: rotate(360deg);
			}
		}
	</style>
</head>

<body>


	<#include "navbar.ftl">

		<div class="container-fluid">
			<div class="row">

				<#include "sidebar.ftl">

					<div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
						<span id="statusMessageDiv">${msg}</span>

						<div class="row">
							<div class="col-md-6">
								<h2 class="sub-header">Deployed Process Definitions</h2>
							</div>
							<div class="col-md-6">
								<form id="bpmn-form" action="/${base}/rest/deployments/deployProcessDefinition"
									method="post" enctype="multipart/form-data" class="form">
									<div id="open-file-div">
										<img src="/${base}/images/open-file-icon.png" alt="Open File" width="32">
										<label>Browse for BPMN file...</label>
									</div>
									<input class="btn btn-default" style="clear:both" type="file" accept=".bpmn"
										name="file" id="file-input" onchange="form.submit()" />
									<!-- <input class="btn btn-primary" type="submit" id="deployProcDefBtn" name="deployProcDefBtn" value="Deploy File as Process Definition">
										-->
								</form>
							</div>
						</div>
						<br>

						<div class="row">
							<div class="col-md-4">
								<div>
									<label for="hide-sus-btn">
										<input name="hide-suspended" id="hide-sus-btn" type="checkbox">
										Hide All Suspended Processes</label>
								</div>
								<div>
									<select class="form-control" id="refresh-rate">
										<option value="5">5 second refresh rate</option>
										<option value="3">3 second refresh rate</option>
										<option value="1">1 second refresh rate</option>
										<option value="0">Stop auto-refresh</option>
									</select>
								</div>
								<br>
								<div>
									<select class="form-control" id="stats-last-num-hours">
										<option value="1">Show stats for last Hour</option>
										<option value="3">Show stats for last 3 Hours</option>
										<option value="6">Show stats for last 6 Hours</option>
										<option value="12">Show stats for last 12 Hours</option>
										<option value="24" selected="selected">Show stats for last Day</option>
										<option value="72">Show stats for last 3 Days</option>
										<option value="168">Show stats for last 1 Week</option>
										<option value="336">Show stats for last 2 Weeks</option>
										<option value="null">Show stats for All Time</option>
									</select>
								</div>
							</div>

							<div class="status-div col-md-7 col-md-offset-1">
								<label>Process status summary:</label>
								<div id="stat-txt-cws-reserved-total" class="stat-txt">-</div>
								<div id="stat-bar-cws-reserved-total" class="progress">
									<div class="progress-bar progress-bar-danger bar-error" data-toggle="tooltip"
										title="0 Errors">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar progress-bar-warning bar-pending" data-toggle="tooltip"
										title="0 Pending">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar progress-bar-disabled bar-disabled" data-toggle="tooltip"
										title="0 Disabled">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar progress-bar-info bar-active" data-toggle="tooltip"
										title="0 Active">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar progress-bar-success bar-completed" data-toggle="tooltip"
										title="0 Completed">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar bar-failedToStart" data-toggle="tooltip"
										title="0 Failed to Start">
										<span class="sr-only"></span>
									</div>

									<div class="progress-bar bar-incident" data-toggle="tooltip" title="0 Incidents">
										<span class="sr-only"></span>
									</div>

									<span class="sr-only">No Summary Statistics...</span>
								</div>
							</div>
						</div>

						<table id="process-table" class="table table-striped sortable">
							<thead>
								<tr>
									<th>Name</span></th>
									<th>Key</span></th>
									<th>Version&nbsp;</span></th>
									<th>Workers</span></th>
									<th>Status&nbsp;</span></th>
									<!-- <th># Pending</span></th>
							<th># Active</span></th>
							<th># Completed</span></th>
							<th class="sort"># Error</span></th> -->
									<th style="width:500px">Instance Statistics</th>
								</tr>
							</thead>
							<tbody>
								<#list procDefs as pd>
									<tr class="<#if pd.suspended>disabled</#if>">
										<td><a style="cursor: pointer;"
												onClick='parent.location="/camunda/app/cockpit/default/#/process-definition/${pd.id}/runtime"' />${pd.name!""}</a>
											<a data-proc-key="${pd.key}"
												onClick="handleDeleteProcDef(this.getAttribute('data-proc-key'))"><span
													style="cursor:pointer;float:right;color:#d9534f;padding-left:7"
													id="delete-${pd.key}" class="glyphicon glyphicon-remove-sign"></a>
											<a href="/${base}/modeler?procDefKey=${pd.key}" target="_blank"></span><span
													style="float:right;" id="edit-${pd.key}"
													class="glyphicon glyphicon-pencil"></span></a>
										</td>
										<td>${pd.key!"ERROR"}</td>
										<td>${pd.version!"ERROR"}</td>
										<td><button id="pv-${pd.key}" class="btn btn-default worker-view-btn"
												data-proc-key="${pd.key}">view</button></td>
										<td>
											<#if pd.suspended>suspended<#else>active</#if>
											<img id="spinner_${pd.key}" src="/${base}/images/spinner.20.gif"
												style="display:none;" />
										</td>
										<!-- <td><div id="numPending_${pd.key}">...</div></td>
								<td><div id="numRunning_${pd.key}">...</div></td>
								<td><div id="numCompleted_${pd.key}">...</div></td>
								<td><div id="numError_${pd.key}">...</div></td> -->
										<td>
											<div id="stat-txt-${pd.key}" class="stat-txt"></div>
											<div id="stat-bar-${pd.key}" class="progress" data-pdk="${pd.key}">
												<div class="progress-bar progress-bar-danger bar-error"
													data-toggle="tooltip" title="0 Errors">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar progress-bar-warning bar-pending"
													data-toggle="tooltip" title="0 Pending">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar progress-bar-disabled bar-disabled"
													data-toggle="tooltip" title="0 Disabled">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar progress-bar-info bar-active"
													data-toggle="tooltip" title="0 Active">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar progress-bar-success bar-completed"
													data-toggle="tooltip" title="0 Completed">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar bar-failedToStart" data-toggle="tooltip"
													title="0 Failed to Start">
													<span class="sr-only"></span>
												</div>

												<div class="progress-bar bar-incident" data-toggle="tooltip"
													title="0 Incidents">
													<span class="sr-only"></span>
												</div>

												<span class="sr-only">No Instance Statistics...</span>
											</div>
										</td>
									</tr>
								</#list>
							</tbody>
						</table>

					</div>
			</div>
		</div>

		<div class="modal fade" id="page-ref-modal" role="dialog" data-backdrop="static" data-keyboard="false">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">WARNING!</h4>
					</div>

					<div class="modal-body">
						<p>Your browser has been idle for more than 10 minutes. Do you want to resume?</p>
					</div>

					<div class="modal-footer">
						<button id="resume-refresh" type="button" class="btn btn-primary">Resume</button>
					</div>
				</div> <!-- modal-content -->
			</div> <!-- modal-dialog -->
		</div> <!-- .modal .fade -->


		<div class="modal fade" id="delete-proc-def-modal" role="dialog" data-backdrop="static" data-keyboard="false">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Confirm Delete</h4>
					</div>

					<div class="modal-body">
						<p>Are you sure you want to delete process definition '<span id="delete-proc-name"></span>'?</p>
						<br />
						<p>Warning: All data for this process definition will be removed (run history, logs, pending
							runs, etc...)</p>
						<div id="deleting-message-container">
							<div class="loader"></div>
							<div style="margin-left: 14px">Deleting... Please wait.</div>
						</div>
					</div>
					<div class="modal-footer">
						<button id="delete-proc-def" type="button" class="btn btn-primary">Yes</button>
						<button type="button" class="btn btn-default" data-dismiss="modal">No</button>
					</div>
				</div> <!-- modal-content -->
			</div> <!-- modal-dialog -->
		</div> <!-- .modal .fade -->

		<div class="modal fade" id="delete-error-modal" role="dialog" data-backdrop="static" data-keyboard="false">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Delete Failed</h4>
					</div>

					<div class="modal-body">
						<p>There was an error deleting process definition '<span id="fail-delete-proc-name"></span>'.
						</p>
						<br />
						<div id="delete-error-message">Error</div>
					</div>

					<div class="modal-footer">
						<button type="button" class="btn btn-primary" data-dismiss="modal">Dismiss</button>
					</div>
				</div> <!-- modal-content -->
			</div> <!-- modal-dialog -->
		</div> <!-- .modal .fade -->

		<div class="modal fade" id="workers-modal" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">Select one or more workers to enable the process.</h4>
					</div>

					<div class="modal-body">
						<div>
							<input type="checkbox" id="all-workers" />&nbsp;
							<label for="all-workers" id='selAll-label'>Select All Workers</label>
						</div>
						<hr />
						<div id="workers-div"></div>
						<hr />
						<span style="color:#666; font-size:95%">
							<strong>*It's recommended that this worker always be selected.</strong>
							<p>This is because all manual tasks (i.e. User Tasks and manual process starts via the
								TaskList app)
								are initiated via this worker.</p>
						</span>
						<hr />
						<span>Note: Grayed out workers are currently down.</span>
					</div>

					<div class="modal-footer">
						<button id="done-workers-btn" type="button" class="btn btn-primary">Done</button>
					</div>
				</div> <!-- modal-content -->
			</div> <!-- modal-dialog -->
		</div> <!-- .modal .fade -->

		<div class="modal fade" id="ajax-error-modal" role="dialog" data-backdrop="static" data-keyboard="false">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">AJAX ERROR!</h4>
					</div>

					<div class="modal-body">
						<p>There was an error loading the status of processes. Please make sure that CWS is up and
							running.</p>
					</div>

					<div class="modal-footer">
						<button type="button" class="btn btn-primary" data-dismiss="modal">Dismiss</button>
					</div>
				</div> <!-- modal-content -->
			</div> <!-- modal-dialog -->
		</div> <!-- .modal .fade -->

		<!-- Bootstrap core JavaScript
================================================== -->
		<!-- Placed at the end of the document so the pages load faster -->
		<script src="/${base}/js/bootstrap.min.js"></script>
		<script type="text/javascript" src="/${base}/js/cws.js"></script>
		<script type="text/javascript">
			var dataProcKey;
			var hideall = false;
			var id;
			$(".bar-error").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=fail";
				}
				else {
					window.location = "/${base}/processes?status=fail";
				}
			});
			$(".bar-completed").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=complete,resolved";
				}
				else {
					window.location = "/${base}/processes?status=complete,resolved";
				}
			});
			$(".bar-pending").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=pending";
				}
				else {
					window.location = "/${base}/processes?status=pending";
				}
			});
			$(".bar-disabled").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=disabled";
				}
				else {
					window.location = "/${base}/processes?status=disabled";
				}
			});
			$(".bar-active").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=running";
				}
				else {
					window.location = "/${base}/processes?status=running";
				}
			});
			$(".bar-failedToStart").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=failedToStart";
				}
				else {
					window.location = "/${base}/processes?status=failedToStart";
				}
			});
			$(".bar-incident").click(function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=incident";
				}
				else {
					window.location = "/${base}/processes?status=incident";
				}
			});

			$("#hide-sus-btn").click(function () {
				if ($(this).prop("checked")) {
					$("#process-table tr.disabled").hide(100);
					localStorage.setItem(hideSuspendedProcVar, "1");
					hideall = true;
				}
				else {
					$("#process-table tr.disabled").show(100);
					localStorage.setItem(hideSuspendedProcVar, "0");
					hideall = true;
				}
			});

			if (parseInt(localStorage.getItem(hideSuspendedProcVar)) == 0) {
				$("#hide-sus-btn").prop("checked", false);
				$("#process-table tr.disabled").show(100);
				hideall == true;
			}
			else {
				$("#hide-sus-btn").prop("checked", true);
				$("#process-table tr.disabled").hide(100);
			}

			function listWorkersInModal(dataProcKey) {
				$.get("/${base}/rest/worker/" + dataProcKey + "/getWorkersForProc", function (data) {
					$("#workers-div").html('');
					//Returned JSON is an array of objects
					var listWorkers = JSON.parse(data);
					//$.each(JSON.parse(data), function(i) {
					for (i in listWorkers) {
						var div = "<div>" +
							"<input type='checkbox' id='" + listWorkers[i].id + "-box' " + " class='worker-checkbox' " +
							((listWorkers[i].status == 'down') ? " disabled='disabled'" : '') +
							" onClick=\'enableDisable(\"" + listWorkers[i].id.toString() + "\");\' " + (listWorkers[i].accepting_new ? "checked" : "") + "/>" +
							"<label for='" + listWorkers[i].id + "-box'" + ((listWorkers[i].status == 'down') ? " class='w-down'" : '') + ">" + listWorkers[i].name +
							(listWorkers[i].cws_install_type == 'console_only' ? '*' : '') + "</label>" +
							"<span id='" + listWorkers[i].id + "-msg'>" +
							"</div>";
						$("#workers-div").append(div);
					}


					//check the select/deselect checkbox if all workers are selected
					if ($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length) {
						$("#all-workers").prop('checked', true);
					}
					else {
						$("#all-workers").prop('checked', false);
					}

					$("#workers-modal").modal('show');
				});
			}

			$(".worker-view-btn").click(function () {
				dataProcKey = $(this).attr("data-proc-key");
				listWorkersInModal(dataProcKey);
			});

			//
			// CLICK ACTION FOR
			// "Select All Workers" checkbox in modal
			//
			$("#all-workers").click(function () {
				if ($(this).prop("checked")) {
					$(".worker-checkbox").each(function () {
						if (!$(this).prop("checked"))
							$(this).click();
					});
				}
				else {
					$(".worker-checkbox").each(function () {
						if ($(this).prop("checked"))
							$(this).click();
					});
				}
			});

			$("#refresh-rate").on('change', function () {
				refreshRate = parseInt($(this).val()) * 1000;
				localStorage.setItem(refreshRateVar, refreshRate.toString());
				clearInterval(pageRefId);
				if (refreshRate == 0)
					return;
				refreshStats();
				pageRefId = setInterval(pageRefresh, parseInt(localStorage.getItem(refreshRateVar)));
			});

			$("#stats-last-num-hours").on('change', function () {
				lastNumHours = parseInt($(this).val()) | null;
				localStorage.setItem(lastNumHoursVar, lastNumHours.toString());
				refreshStats();
			});


			// Done button closes the modal (as does clicking outside or pressing esc)
			$("#done-workers-btn").click(function () {
				$("#workers-modal").modal('hide');
			});

			// When the modal is closed (by any means), update the main-list buttons to reflect the worker status
			$('#workers-modal').on('hidden.bs.modal', function () {
				adjustWorkersButton();
			})

			function enableDisable(wid) {
				var enabledFlag = $("#" + wid + "-box").prop("checked");
				var postUrl = "/${base}/rest/worker/" + wid + "/" + dataProcKey + "/updateWorkerProcDefEnabled/" + enabledFlag;
				$.post(postUrl, function (data) {

					if (data == "success") {
						if (enabledFlag == true) {
							$("#" + wid + "-msg").html('<font color="green">enabled</font>');
						}
						else {
							$("#" + wid + "-msg").html('<font color="green">disabled</font>');
						}
						$("#" + wid + "-msg font").fadeOut(2000, "linear");
					}
					else {
						$("#" + wid + "-msg").html('<font color="red">update failed</font>');
					}

					//check the select/deselect checkbox if all workers are selected
					if ($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length) {
						$("#all-workers").prop('checked', true);
					}
					else
						$("#all-workers").prop('checked', false);

				});
			}

			function adjustWorkersButton() {
				$.get("/${base}/rest/processes/getProcDefWorkerCount", function (data) {
					var rows = JSON.parse(data)
					for (i in rows) {
						if (rows[i].workers == 0) {
							$("#pv-" + rows[i].pdk).removeClass("btn-default").addClass("btn-danger");
							$("#pv-" + rows[i].pdk).text("enable");
						}
						else {
							$("#pv-" + rows[i].pdk).removeClass("btn-danger").addClass("btn-default");
							$("#pv-" + rows[i].pdk).text("view");
						}
					}
				});
			}

			function downloadJSON() {
				var dt = $('#process-table').DataTable();
				var data = dt.buttons.exportData();
				//number of rows
				var numRows = dt.rows().count();
				var jsonFile = {};
				var models = {};

				dt.rows().every(function (rowIdx, tableLoop, rowLoop) {
					var thisModelJson = {};
					var modelName = this.data()[0].replace(/<a.*?>/g, '');
					modelName = modelName.substring(0, modelName.indexOf("</a>"));

					var modelId = this.data()[1];
					//modelId = modelId.substring(modelId.indexOf("id=\"") + 4);
					//modelId = modelId.substring(0, modelId.indexOf("\""));

					var version = this.data()[2];

					var status = this.data()[4];
					//check if active is in str
					if (status.indexOf("active") > -1) {
						status = "active";
					}
					else {
						status = "inactive";
					}

					var hasAssignedWorkers = $("#pv-" + modelId).hasClass("btn-danger") ? "false" : "true";

					var statPending = 0;
					var statDisabled = 0;
					var statActive = 0;
					var statCompleted = 0;
					var statError = 0;
					var statFailedToStart = 0;
					var statIncident = 0;
					var stats = $("#stat-txt-" + modelId).html();

					if (stats !== "No stats for this process") {
						if (stats.indexOf("pending") > -1) {
							statPending = stats.substring(stats.indexOf("<b>pending</b>:&nbsp;") + 21);
							statPending = parseInt(statPending.substring(0, statPending.indexOf("&")));
						}
						if (stats.indexOf("disabled") > -1) {
							statDisabled = stats.substring(stats.indexOf("<b>disabled</b>:&nbsp;") + 22);
							statDisabled = parseInt(statDisabled.substring(0, statDisabled.indexOf("&")));
						}
						if (stats.indexOf("running") > -1) {
							statActive = stats.substring(stats.indexOf("<b>running</b>:&nbsp;") + 21);
							statActive = parseInt(statActive.substring(0, statActive.indexOf("&")));
						}
						if (stats.indexOf("completed") > -1) {
							statCompleted = stats.substring(stats.indexOf("<b>completed</b>:&nbsp;") + 23);
							statCompleted = parseInt(statCompleted.substring(0, statCompleted.indexOf("&")));
						}
						if (stats.indexOf("failed") > -1) {
							statError = stats.substring(stats.indexOf("<b>failed</b>:&nbsp;") + 20);
							statError = parseInt(statError.substring(0, statError.indexOf("&")));
						}
						if (stats.indexOf("failed-start") > -1) {
							statFailedToStart = stats.substring(stats.indexOf("<b>failed-start</b>:&nbsp;") + 28);
							statFailedToStart = parseInt(statFailedToStart.substring(0, statFailedToStart.indexOf("&")));
						}
						if (stats.indexOf("incident") > -1) {
							statIncident = stats.substring(stats.indexOf("<b>incidents</b>:&nbsp;") + 23);
							statIncident = parseInt(statIncident.substring(0, statIncident.indexOf("&")));
						}
					}

					thisModelJson["model-name"] = modelName;
					thisModelJson["model-id"] = modelId;
					thisModelJson["version"] = version;
					thisModelJson["status"] = status;
					thisModelJson["has-assigned-workers"] = hasAssignedWorkers;
					thisModelJson["stat-proc-pending"] = statPending;
					thisModelJson["stat-proc-disabled"] = statDisabled;
					thisModelJson["stat-proc-running"] = statActive;
					thisModelJson["stat-proc-completed"] = statCompleted;
					thisModelJson["stat-proc-error"] = statError;
					thisModelJson["stat-proc-failed-to-start"] = statFailedToStart;
					thisModelJson["stat-proc-incident"] = statIncident;

					models[modelId] = thisModelJson;

				});
				jsonFile["models"] = models;
				console.log(jsonFile);
				$.fn.dataTable.fileSave(
					new Blob([JSON.stringify(jsonFile)]),
					'deployments_export.json'
				);
			}

		</script>

</body>

</html>