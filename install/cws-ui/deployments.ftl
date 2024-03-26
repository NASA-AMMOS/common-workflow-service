<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Deployments</title>

	<!-- JAVASCRIPT LINKS -->
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/popper.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<script src="/${base}/js/bootstrap-toggle.min.js"></script>
	<script src="/${base}/js/popper.min.js"></script>
	<script src="/${base}/js/DataTables/datatables.js"></script>
	<script src="/${base}/js/cws.js"></script>
	<script src="/${base}/js/adaptation-workers-modal.js"></script>
	<!-- CSS LINKS -->
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="/${base}/js/bootstrap-toggle.min.js"></script>
	<link rel="stylesheet" href="/${base}/js/DataTables/datatables.css"/>
	<!-- Custom styles for this template -->
	<link href="/${base}/js/DataTables/datatables.css" rel="stylesheet">
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<link href="/${base}/css/deployments.css" rel="stylesheet">
	<link href="/${base}/css/microtip.css" rel="stylesheet">

	<script>
		//STATE PERSISTANCE CONSTS
		var username = document.cookie.substring(document.cookie.indexOf("cwsUsername=") + 12);
		if (username.indexOf(";") > 0) {
			username = username.substring(0, username.indexOf(";"));
		}
		const lastNumHoursVar = "CWS_DASH_DEPLOY_LAST_NUM_HOURS-" + username;
		const refreshRateVar = "CWS_DASH_DEPLOY_REFRESH_RATE-" + username;
		const hideSuspendedProcVar = "CWS_DASH_DEPLOY_HIDE_SUS-" + username;

		//GLOBAL VARIABLES

		var statsVal = {};
		var statsTotalVal = {};
		var procDefArray = [];
		var refreshing = false;
		var pageRefId = 0;
		var refreshRate = 5000;
		var lastNumHours = 24;
		var deleteProcDefName = "";
		var idling = false;
		var idleTimer = 0;
		var idleInterval = 600000 // 10 minutes (10 * 60 * 1000)

		//GET PROCESS DEFINITIONS AS AN ARRAY (USES FREEMARKER SYNTAX)
		<#list procDefs as x>
		statsVal.${x.key} = {
			pending: '...',
			disabled: '...',
			active: '...',
			completed: '...',
			error: '...',
			fts: '...',
			incident: '...'
		};
		var procDef = {
			"key": "${x.key}",
			"name": "${x.name}",
			"version": "${x.version}",
			"suspended": "${x.suspended?c}",
			"id": "${x.id}"
		};
		procDefArray.push(procDef);
		</#list>

		// REFRESH THE TEXTUAL STATS SUMMARY
		function refreshStatUI(name, statsCounts) {
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

		//HANDLER FUNCTION FOR DELETING A PROCESS DEFINITION
		function handleDeleteProcDef(proc_def_key) {

			deleteProcDefName = proc_def_key;

			var html = "<b>" + deleteProcDefName + "</b>";

			$('#delete-proc-name').html(html);
			$('#fail-delete-proc-name').html(html);

			$("#deleting-message-container").css("display", "none");
			$("#delete-proc-def").prop('disabled', false);
			$("#delete-proc-def-modal").modal('show');
		}

		//HANDLER FUNCTION FOR DELETING A PROCESS DEFINITION (ERROR STATE)
		function getDeleteErrorMessage(message) {
			if (message.indexOf("(Accepting new)") > 0) {
				return "You must disable this process definition on all workers before deleting.";
			} else if (message.indexOf("(Not found)") > 0) {
				return "Process definition was not found.  Maybe it was already deleted.  Try refreshing the page...";
			} else if (message.indexOf("(Running)") > 0) {
				return "Before deleting this process definition, you must wait for it to finish running or stop it, then disable it on all workers.";
			} else {
				return "An unknown error occured.";
			}
		}

		//FUNCTION FOR DELETING A PROCESS DEFINITION, MAKES AJAX CALL TO REST SERVICE
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
					} else {

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

		//DELETES LOGS OF A PROC DEF IN ELASTICSEARCH - MAKES AJAX CALL TO REST SERVICE
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

		//GRABS LATEST STATS FROM REST SERVICE AND UPDATES THE STATS
		function refreshStats() {

			if (refreshing) return;

			refreshing = true;
			//grab the value here so we don't have to do it multiple times
			var statsCookieValue = parseInt(localStorage.getItem(lastNumHoursVar));
			if (statsCookieValue == -1) {
				statsCookieValue = null;
			}

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
						if (statsVal[key] == undefined) {
							return true;
						}

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

		//DOCUMENT.READY STARTS HERE
		$(document).ready(function () {
			// DISPLAY MESSAGE AT TOP OF PAGE (IF THERE IS ONE)
			if ($("#statusMessageDiv:contains('ERROR:')").length >= 1) {
				$("#statusMessageDiv").css("color", "red");
			} else {
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

			//DATATABLE INITIALIZATION FOR PROCESS DEFINITION TABLE
			$("#process-table").DataTable({
				//SET OPTIONS FOR DATATABLE HERE... ALL OPTIONS CAN BE FOUND HERE: https://datatables.net/reference/option/
				//SET HOW DATA IS DISPLAYED IN EACH COLUMN (IN ORDER) (https://datatables.net/reference/option/columns)
				columns: [
					//ICONS COLUMN
					{
						data: {suspended: "suspended", id: "id", key: "key"},
						render: function (data, type) {
							if (type !== 'display') {
								return data.id;
							} else {

								var returnVal = `<div class="proc-name-btns">`;
								if (data.suspended == "true") {
									returnVal += `<a id="btn-suspend-` + data.key + `" data-proc-id="` + data.key + `" onClick="resumeProcDef('` + data.id + `', '` + data.key + `')" aria-label="Resume" data-microtip-position="top-right" role="tooltip">`
											+ `<img height="16" width="16" src="/${base}/images/play.svg" style="cursor: pointer; float: right; color: green;" id="suspend-`
											+ data.key + `" />`
											+ `</a>`;
								} else {
									returnVal += `<a id="btn-suspend-` + data.key + `" data-proc-id="` + data.key + `" onClick="suspendProcDef('` + data.id + `', '` + data.key + `')" aria-label="Suspend" data-microtip-position="top-right" role="tooltip">`
											+ `<img height="16" width="16" src="/${base}/images/pin_pause.svg" style="cursor: pointer; float: right; color: #d9534f;" id="suspend-`
											+ data.key + `" /></a>`;
								}

								returnVal += `<a href="/${base}/modeler?procDefKey=` + data.key + `" target="_blank" aria-label="Edit" data-microtip-position="top-right" role="tooltip">`
										+ `<span style="float: right;" id="edit-` + data.key + `"><img height="16" width="16" src="/${base}/images/pen.svg" /></span></a>`
										+ `<a data-proc-key="` + data.key + `" onClick="handleDeleteProcDef('` + data.key + `')" aria-label="Delete" data-microtip-position="top-right" role="tooltip">`
										+ `<img height="16" width="16" src="/${base}/images/trash.svg" style="cursor: pointer; float: right; color: #d9534f;" id="delete-`
										+ data.key + `" /></a>`;

								returnVal += `</div>`;
								return returnVal;
							}
						}
					},
					//NAME COLUMN
					{
						data: {name: "name", id: "id", key: "key"},
						render: function (data, type) {
							if (type !== 'display') {
								return data.name;
							} else {
								var html = `<div class-"proc-name-name"><a style="cursor: pointer;" onClick='parent.location="/camunda/app/cockpit/default/#/process-definition/` + data.id + `/runtime"'/>` + data.name + `</a></div>`;
								return html;
							}
						}
					},
					//KEY COLUMN
					{
						data: "key",
						render: function (data, type) {
							if (type !== 'display') {
								return data;
							} else {
								if (data === null || data === undefined || data === "null") {
									return "ERROR";
								} else {
									return data;
								}
							}
						}
					},
					//VERSION COLUMN
					{
						data: "version",
						render: function (data, type) {
							if (type !== 'display') {
								return data;
							} else {
								if (data === null || data === undefined || data === "null") {
									return "ERROR";
								} else {
									return data;
								}
							}
						}
					},
					//WORKERS BUTTON COLUMN
					{
						data: "key",
						render: function (data, type) {
							if (type !== 'display') {
								return "";
							} else {
								var html = `<button type="button" id="pv-` + data + `" class="btn btn-default worker-view-btn"`
										+ `data-proc-key="` + data + `">view</button>`;
								return html;
							}
						}
					},
					//STATUS COLUMN
					{
						data: {suspended: "suspended", key: "key"},
						render: function (data, type) {
							if (type !== 'display') {
								if (data.suspended === "true") {
									return "Suspended";
								} else {
									return "Active";
								}
							} else {
								var status = "";
								var html = "";
								if (data.suspended == "true") {
									html = `<div class="status-div-text" id="status-txt-` + data.key + `"><i style="color: dimgray;">Suspended</i></div>`;
								} else {
									html = `<div class="status-div-text" id="status-txt-` + data.key + `">Active</div></div>`;
								}
								return html;
							}
						}
					},
					//INSTANCE STATISTICS COLUMN
					{
						data: "key",
						render: function (data, type) {
							if (type !== 'display') {
								return "";
							} else {
								var html = `<div id="stat-txt-` + data + `" class="stat-txt"></div>`
										+ `<div id="stat-bar-` + data + `" class="progress" data-pdk="` + data + `">`
										+ `<div class="progress-bar progress-bar-danger bar-error"`
										+ `data-toggle="tooltip" title="0 Errors">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar progress-bar-warning bar-pending"`
										+ `data-toggle="tooltip" title="0 Pending">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar progress-bar-disabled bar-disabled"`
										+ `data-toggle="tooltip" title="0 Disabled">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar progress-bar-info bar-active"`
										+ `data-toggle="tooltip" title="0 Active">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar progress-bar-success bar-completed"`
										+ `data-toggle="tooltip" title="0 Completed">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar bar-failedToStart" data-toggle="tooltip"`
										+ `title="0 Failed to Start">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<div class="progress-bar bar-incident" data-toggle="tooltip"`
										+ `title="0 Incidents">`
										+ `<span class="sr-only"></span>`
										+ `</div>`
										+ `<span class="sr-only">No Instance Statistics...</span>`
										+ `</div>`;
								return html;
							}
						}
					}
				],
				//SETS EACH ROW ID TO BE THE "KEY" DATA VALUE (https://datatables.net/reference/option/rowId)
				rowId: "key",
				//DISABLES ORDERING ON BUTTON, WORKER, AND INSTANCE STATISTICS COLUMNS (https://datatables.net/reference/option/columnDefs)
				columnDefs: [
					{orderable: false, targets: [0, 6, 4]}
				],
				//SETS DEFAULT ORDERING TO BE THE "NAME" COLUMN, ASCENDING (https://datatables.net/reference/option/order)
				order: [[1, "asc"]],
				//DISABLES PAGINATION (ONE LONG TABLE) (https://datatables.net/reference/option/paging)
				"paging": false,
				//SETS WHAT ELEMENTS ARE CREATED BY DATATABLE AND WHERE ELEMENTS ARE PUT (https://datatables.net/reference/option/dom)
				dom: "<'above-table-div form-inline'<'above-table-buttons form-group mb-2'>f>"
					+ "t"
					+ "<'below-table-div'i>",
			});

			//OUR DATA COMES FROM FREEMARKER TEMPLATE - ADD THAT ARRAY TO THE DATATABLE
			$("#process-table").DataTable().rows.add(procDefArray);
			//REDRAW THE TABLE TO REFLECT THE NEW DATA
			$("#process-table").DataTable().draw();

			//ADD DOWNLOAD BUTTON & HIDE SUSPENDED CHECKBOX TO DIVS CREATED BY DATATABLE (DOM OPTION)
			$('<button id="download-btn" class="btn btn-primary" onclick="downloadJSON()"><img height="16" width="16" src="/${base}/images/download.svg" style="margin-right: 3px;" />Download</button>').appendTo(".above-table-buttons");
			$('<div class="form-check form-check-inline"><input class="form-check-input" name="hide-suspended" id="hide-sus-btn" type="checkbox" style="align-self: center;"><label class="form-check-label" for="hide-sus-btn">Hide All Suspended Processes</label></div>').appendTo(".above-table-buttons");

			//HANDLES MODAL POPUP FOR WORKER BUTTON
			$(".worker-view-btn").on("click", function () {
				dataProcKey = $(this).attr("data-proc-key");
				listWorkersInModal(dataProcKey);
			});
			//HANDLES HIDE SUSPENDED PROC DEF CHECKBOX BEHAVIOR
			$("#hide-sus-btn").on("click", function () {
				if ($(this).prop("checked")) {
					$("#process-table").DataTable().column(5).search("Active", false, true).draw();
					localStorage.setItem(hideSuspendedProcVar, "1");
					refreshStats();
				} else {
					$("#process-table").DataTable().column(5).search("").draw();
					localStorage.setItem(hideSuspendedProcVar, "0");
					refreshStats();
				}
				$("#process-table").DataTable().rows().every(function () {
					$("#process-table").DataTable().rows().every(function (rowIdx, tableLoop, rowLoop) {
						var status = this.data()["suspended"];
						var procDefKey = this.data()["key"];
						var procDefId = this.data()["id"];
						if (status == "false") {
							$("#suspend-" + procDefKey).attr("src", "/${base}/images/pin_pause.svg");
							$("#suspend-" + procDefKey).css("color", "#d9534f");
							$("#btn-suspend-" + procDefKey).attr("onclick", "suspendProcDef('" + procDefId + "', '" + procDefKey + "')");
							$("#status-txt-" + procDefKey).html("Active");
							$("#" + procDefKey).removeClass("disabled");
							$("#pv-" + procDefKey).removeClass("disabled");
						} else {
							$("#suspend-" + procDefKey).attr("src", "/${base}/images/play.svg");
							$("#suspend-" + procDefKey).css("color", "green");
							$("#btn-suspend-" + procDefKey).attr("onclick", "resumeProcDef('" + procDefId + "', '" + procDefKey + "')");
							$("#status-txt-" + procDefKey).html("Suspended");
							$("#" + procDefKey).addClass("disabled");
							$("#pv-" + procDefKey).addClass("disabled");
						}
					});
				});
			});

			//INIT STATE OF HIDE SUSPENDED PROC DEF CHECKBOX
			if (parseInt(localStorage.getItem(hideSuspendedProcVar)) == 0) {
				$("#hide-sus-btn").prop("checked", false);
				$("#process-table").DataTable().column(5).search("").draw();
			} else {
				$("#hide-sus-btn").prop("checked", true);
				$("#process-table").DataTable().column(5).search("Active", false, true).draw();
			}
			//PULL LATEST STATS
			refreshStats();

			//INIT STATE OF REFRESH RATE
			if (parseInt(localStorage.getItem(refreshRateVar)) !== 0) {
				pageRefId = setInterval(pageRefresh, parseInt(localStorage.getItem(refreshRateVar)));
			}
			idleTimer = setInterval(idleMode, idleInterval);

			//HANDLES HIDING INACTIVITY MODAL
			$("#resume-refresh").on("click", function () {
				$("#page-ref-modal").modal('hide');
				idling = false;
			});

			//HANDLES DELETE PROC DEF BUTTON
			$("#delete-proc-def").on("click", function () {
				$("#delete-proc-def").prop('disabled', true);
				$("#deleting-message-container").css("display", "flex");
				deleteProcDef(deleteProcDefName);
			});

			$("#open-file-div").on("click", function () {
				$("#file-input").trigger("click");
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

			$(".bar-error").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=fail&cache=false";
				} else {
					window.location = "/${base}/processes?status=fail&cache=false";
				}
			});
			$(".bar-completed").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=complete,resolved&cache=false";
				} else {
					window.location = "/${base}/processes?status=complete,resolved&cache=false";
				}
			});
			$(".bar-pending").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=pending&cache=false";
				} else {
					window.location = "/${base}/processes?status=pending&cache=false";
				}
			});
			$(".bar-disabled").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=disabled&cache=false";
				} else {
					window.location = "/${base}/processes?status=disabled&cache=false";
				}
			});
			$(".bar-active").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=running&cache=false";
				} else {
					window.location = "/${base}/processes?status=running&cache=false";
				}
			});
			$(".bar-failedToStart").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=failedToStart&cache=false";
				} else {
					window.location = "/${base}/processes?status=failedToStart&cache=false";
				}
			});
			$(".bar-incident").on("click", function () {
				id = $(this).parent().attr("data-pdk");
				if (id) {
					window.location = "/${base}/processes?procDefKey=" + id + "&status=incident&cache=false";
				} else {
					window.location = "/${base}/processes?status=incident&cache=false";
				}
			});

			adjustWorkersButton();
		});
		//DOCUMENT.READY ENDS HERE

		//THIS FUNCTION GETS CALLED BY AUTO REFRESH EVERY X SECONDS
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

		//suspend procDef given procDefId
		function suspendProcDef(procDefId, procDefKey) {
			console.log("Suspending procDefId: " + procDefId);
			var result;
			//make a post request to suspend the procDef
			$.ajax({
				url: "/${base}/rest/deployments/suspend/" + encodeURIComponent(procDefId),
				type: "POST",
				success: function (data) {
					console.log("successfully suspended");
					//change the glyphicon to play & make green
					$("#suspend-" + procDefKey).attr("src", "/${base}/images/play.svg");
					$("#suspend-" + procDefKey).css("color", "green");
					$("#btn-suspend-" + procDefKey).attr("onclick", "resumeProcDef('" + procDefId + "', '" + procDefKey + "')");
					$("#status-txt-" + procDefKey).html("Suspended");
					$("#" + procDefKey).addClass("disabled");
					$("#pv-" + procDefKey).addClass("disabled");
				},
				error: function (data) {
					console.log("error suspending");
				}
			})

		}

		//resume procDef given procDefId
		function resumeProcDef(procDefId, procDefKey) {
			console.log("Resuming procDefId: " + procDefId);
			var result;
			//make a post request to suspend the procDef
			$.ajax({
				url: "/${base}/rest/deployments/activate/" + encodeURIComponent(procDefId),
				type: "POST",
				success: function (data) {
					console.log("successfully activated");
					//change the glyphicon to pause & make color #d9534f
					$("#suspend-" + procDefKey).attr("src", "/${base}/images/pin_pause.svg");
					$("#suspend-" + procDefKey).css("color", "#d9534f");
					$("#btn-suspend-" + procDefKey).attr("onclick", "suspendProcDef('" + procDefId + "', '" + procDefKey + "')");
					$("#status-txt-" + procDefKey).html("Active");
					$("#" + procDefKey).removeClass("disabled");
					$("#pv-" + procDefKey).removeClass("disabled");
				},
				error: function (data) {
					console.log("error activating");
				}
			})

		}

	</script>

	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]>
	<script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->
</head>

<body>


<#include "navbar.ftl">

<div class="container-fluid" style="padding-left: 0; margin-top: 7px;">
	<div class="row">
		<div class="col main">
			<#include "sidebar.ftl">
				<div class="main-content">
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
							   name="file" id="file-input" onchange="form.submit()"/>
						<!-- <input class="btn btn-primary" type="submit" id="deployProcDefBtn" name="deployProcDefBtn" value="Deploy File as Process Definition">
                            -->
					</form>
				</div>
			</div>
			<br>

			<div class="row">
				<div class="col-md-4">
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
							<option value="-1">Show stats for All Time</option>
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

			<table id="process-table" class="table table-striped sortable" style="width: 100%;">
				<thead>
				<tr>
					<th style="width: 30px"></th>
					<th>Name</th>
					<th>Key</th>
					<th>Version</th>
					<th>Workers</th>
					<th>Status</th>
					<th style="width:500px">Instance Statistics</th>
				</tr>
				</thead>
				<tbody>
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
				<br/>
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
				<br/>
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
					<input type="checkbox" id="all-workers"/>&nbsp;
					<label for="all-workers" id='selAll-label'>Select All Workers</label>
					<#include "adaptation-workers-modal.ftl">
				</div>
				<hr/>
				<div id="workers-div"></div>
				<hr/>
				<span style="color:#666; font-size:95%">
              <strong>*It's recommended that this worker always be selected.</strong>
              <p>This is because all manual tasks (i.e. User Tasks and manual process starts via the TaskList app)
              are initiated via this worker.</p>
            </span>
				<hr/>
				<span>Note: Grayed out workers are currently down.</span>
			</div>

			<div class="modal-footer">
				<button id="done-workers-btn" type="button" class="btn btn-primary">Done</button>
			</div>
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

<script src="/${base}/js/bootstrap.min.js"></script>

<script type="text/javascript">
	var dataProcKey;
	var hideall = false;
	var id;

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
			} else {
				$("#all-workers").prop('checked', false);
			}
			$("#workers-modal").modal('show');
		});
	}

	//
	// CLICK ACTION FOR
	// "Select All Workers" checkbox in modal
	//
	$("#all-workers").on("click", function () {
		if ($(this).prop("checked")) {
			$(".worker-checkbox").each(function () {
				if (!$(this).prop("checked"))
					$(this).trigger("click");
			});
		} else {
			$(".worker-checkbox").each(function () {
				if ($(this).prop("checked"))
					$(this).trigger("click");
			});
		}
	});

	$("#refresh-rate").on('change', function () {
		refreshRate = parseInt($(this).val()) * 1000;
		localStorage.setItem(refreshRateVar, refreshRate.toString());
		clearInterval(pageRefId);
		if (refreshRate === 0)
			return;
		refreshStats();
		pageRefId = setInterval(pageRefresh, parseInt(localStorage.getItem(refreshRateVar)));
	});

	$("#stats-last-num-hours").on('change', function () {
		lastNumHours = parseInt($(this).val());
		localStorage.setItem(lastNumHoursVar, lastNumHours.toString());
		refreshStats();
	});

	$("#hide-sus-btn").click(function () {
		if ($(this).prop("checked")) {
			$("#process-table tr.disabled").hide(100);
			hideall = true;
		} else {
			$("#process-table tr.disabled").show(100);
			hideall = true;
		}
	});
	$("#hide-sus-btn").click(); // check by default

	function listWorkersInModal(dataProcKey) {
		$.get("/${base}/rest/worker/" + dataProcKey + "/getWorkersForProc", function (data) {
			$("#workers-div").html('');
			//Returned JSON is an array of objects
			var listWorkers = JSON.parse(data);
			//-------Add potential extra adaptation info to each worker-----
			addAdaptationWorkersInfo(dataProcKey, listWorkers);
			//-------------
			//$.each(JSON.parse(data), function(i) {
			for (i in listWorkers) {
				var div = "<div>" +
						"<input type='checkbox' id='" + listWorkers[i].id + "-box' " + " class='worker-checkbox' " +
						((listWorkers[i].status == 'down') ? " disabled='disabled'" : '') +
						" onClick=\'enableDisable(\"" + listWorkers[i].id.toString() + "\");\' " + (listWorkers[i].accepting_new ? "checked" : "") + "/>" +
						"<label for='" + listWorkers[i].id + "-box'" + ((listWorkers[i].status == 'down') ? " class='w-down'" : '') + ">" + listWorkers[i].name +
						(listWorkers[i].cws_install_type == 'console_only' ? '*' : '') + "</label>" +
						"<span id='" + listWorkers[i].id + "-msg'></span>" +
						"</div>";
				$("#workers-div").append(div);
			}


			//check the select/deselect checkbox if all workers are selected
			if ($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length) {
				$("#all-workers").prop('checked', true);
			} else {
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
		} else {
			$(".worker-checkbox").each(function () {
				if ($(this).prop("checked"))
					$(this).click();
			});
		}
	});

	// Done button closes the modal (as does clicking outside or pressing esc)
	$("#done-workers-btn").on("click", function () {
		$("#workers-modal").modal('hide');
	});

	// When the modal is closed (by any means), update the main-list buttons to reflect the worker status
	$('#workers-modal').on('hidden.bs.modal', function () {
		adjustWorkersButton();
	});

	function enableDisable(wid) {
		var enabledFlag = $("#" + wid + "-box").prop("checked");
		var postUrl = "/${base}/rest/worker/" + wid + "/" + dataProcKey + "/updateWorkerProcDefEnabled/" + enabledFlag;
		$.post(postUrl, function (data) {

			if (data == "success") {
				if (enabledFlag == true) {
					$("#" + wid + "-msg").html('<font color="green">enabled</font>');
				} else {
					$("#" + wid + "-msg").html('<font color="green">disabled</font>');
				}
				$("#" + wid + "-msg font").fadeOut(2000, "linear");
			} else {
				$("#" + wid + "-msg").html('<font color="red">update failed</font>');
			}

			//check the select/deselect checkbox if all workers are selected
			if ($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length) {
				$("#all-workers").prop('checked', true);
			} else
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
				} else {
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
			this.data().get
			var modelName = this.data()["name"];
			console.log(this.data());
			var modelId = this.data()["key"];
			//modelId = modelId.substring(modelId.indexOf("id=\"") + 4);
			//modelId = modelId.substring(0, modelId.indexOf("\""));

			var version = this.data()["version"];

			if (this.data["suspended"] == "true") {
				status = "Suspended";
			} else {
				status = "Active";
			}

			var hasAssignedWorkers = !$("#pv-" + modelId).hasClass("btn-danger");

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
		$.fn.dataTable.fileSave(
				new Blob([JSON.stringify(jsonFile)]),
				'deployments_export.json'
		);
	}


</script>

</body>

</html>