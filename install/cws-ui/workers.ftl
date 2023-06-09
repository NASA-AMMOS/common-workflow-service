<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Workers</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<script src="/${base}/js/adaptation-workers.js"></script>
	<script src="/${base}/js/cws.js"></script>
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<script>

		//STATE PERSISTANCE CONSTS
		const username = "username"; //temporary, hardcoded value for now
		const hideDownWorkersVar = "CWS_DASH_WORKERS_HIDE_DOWN" + username;
	
		var numProcDefs = ${procDefs?size};
		
		var refreshCount = 0;
		var refreshRate = 1000;
	
		function refreshStats() {
		
			$.ajax({ 
				url: "/${base}/rest/stats/workerNumRunningProcs",
				success: function( data ) {

					var updated = [];
					
					$.each(data, function (key, val) {
						var div = $("#" + key + "_numRunningProcs");

						if (div) {
							div.html(val + ' running');
							updated.push(key + "_numRunningProcs");
						}
					});
					
					$("table[id=workers-table] tr td:nth-child(3) div").each(function () {
					
						if (updated.indexOf(this.id) === -1) {
							$(this).html('Idle');
						}
					});
				},
				error: function(){
					clearInterval(pageRefId);
					$("#ajax-error-modal").modal('show');
				}
			});
		}
		
		
		//
		//  CALLED WHEN USER CHECKS/UN-CHECKS A PROCESS CHECKBOX
		//
		function updateProcDefEnabled(worker, proc, enabledFlag) {
			
			$.post( "/${base}/rest/worker/"+worker+"/"+proc+"/updateWorkerProcDefEnabled/"+enabledFlag, function( data ) {
				
				if (data == "success") {
					$( "#" + worker + "_" + proc + "_updateStatus" ).show();
					if (enabledFlag == true) {
						$( "#" + worker + "_" + proc + "_updateStatus" ).html( '<font color="green">enabled</font>' );
						$( "#" + worker + "_" + proc + "_limit" ).show();
					}
					else {
						$( "#" + worker + "_" + proc + "_updateStatus" ).html( '<font color="green">disabled</font>' );
						$( "#" + worker + "_" + proc + "_limit" ).hide();
					}
					$( "#" + worker + "_" + proc + "_updateStatus" ).fadeOut(3000, "linear");
				}
				else {
					$( "#" + worker + "_" + proc + "_updateStatus" ).show();
					$( "#" + worker + "_" + proc + "_updateStatus" ).html( '<font color="red">update failed</font>' );
				}
				
			});
			
			updateEnabledCounts();
		}
	
		//
		//  CALLED WHEN USER UPDATES A PROCESS LIMIT TEXTBOX
		//
		function updateProcLimit(worker, proc, newLimit) {
			$.post( "/${base}/rest/worker/"+worker+"/"+proc+"/updateWorkerProcDefLimit/"+newLimit, function( data ) {
				
				if (data == "success") {
					$( "#" + worker + "_" + proc + "_updateStatus" ).show();
					$( "#" + worker + "_" + proc + "_updateStatus" ).html( '<font color="green">limit updated</font>' );
					$( "#" + worker + "_" + proc + "_updateStatus" ).fadeOut(3000, "linear");
				}
				else {
					$( "#" + worker + "_" + proc + "_updateStatus" ).show();
					$( "#" + worker + "_" + proc + "_updateStatus" ).html( '<font color="red">update failed</font>' );
				}
				
			});
			
		}
		
		//
		//  CALLED WHEN USER UPDATES A WORKER'S EXEC THREADS COUNT
		//
		function updateExecThreads(worker, numThreads) {
			$.post( "/${base}/rest/worker/"+worker+"/updateNumJobExecThreads/"+numThreads, function( data ) {
				
				if (data == "success") {
					$("#" + worker + "_numRunningProcs").data('maxthreads', numThreads);
					refreshStats();
					
					$( "#" + worker + "_updateConfigStatus" ).show();
					$( "#" + worker + "_updateConfigStatus" ).html( '<font color="green">updated</font>' );
					$( "#" + worker + "_updateConfigStatus" ).fadeOut(3000, "linear");
				}
				else {
					$( "#" + worker + "_updateConfigStatus" ).show();
					$( "#" + worker + "_updateConfigStatus" ).html( '<font color="red">' + data + '</font>' );
				}
			});
		}
		
		//
		//  CALLED WHEN USER CLICKS ON PROC DEFS '+' ICON
		//
		function expandProcDefs(workerId) {
			$("#"+workerId+"_plus_procDefs").hide();
			$("#"+workerId+"_minus_procDefs").show();
			$("#"+workerId+"_procDefsTable").show();
		}
		
		//
		//  CALLED WHEN USER CLICKS ON PROC DEFS '-' ICON
		//
		function collapseProcDefs(workerId) {
			$("#"+workerId+"_plus_procDefs").show();
			$("#"+workerId+"_minus_procDefs").hide();
			$("#"+workerId+"_procDefsTable").hide();
		}
		
		//
		//  CALLED WHEN USER CLICKS ON CONFIGURATION '+' ICON
		//
		function expandConfig(workerId) {
			$("#"+workerId+"_plus_config").hide();
			$("#"+workerId+"_minus_config").show();
			$("#"+workerId+"_configTable").show();
			$("#"+workerId+"_configOverview").html("hide details");
		}
		
		//
		//  CALLED WHEN USER CLICKS ON CONFIGURATION '-' ICON
		//
		function collapseConfig(workerId) {
			$("#"+workerId+"_plus_config").show();
			$("#"+workerId+"_minus_config").hide();
			$("#"+workerId+"_configTable").hide();
			$("#"+workerId+"_configOverview").html("show details");
		}
		
		//
		// FUNCTION THAT REFRESHES UI "N / M enabled" COUNTS
		//
		function updateEnabledCounts() {
			<#list workers as w>
				var enabledCount = 0;
				<#list w.procInstanceLimits?keys as procDefKey>
					if ($("#${w.id}_${procDefKey}_enabled").is(':checked')) {
						enabledCount = enabledCount + 1;
					}
				</#list>
				$("#${w.id}_procDefsOverview").html(enabledCount + " / ${procDefs?size}  process definitions enabled");
			</#list>
		}
		
		$( document ).ready(function() {
			// 
			if ($("#statusMessageDiv:contains('ERROR:')").length >= 1) {
				$("#statusMessageDiv").css( "color", "red" );
			}
			else {
				$("#statusMessageDiv").css( "color", "green" );
				if ($('#statusMessageDiv').html().length > 9) {
					$('#statusMessageDiv').fadeOut(5000, "linear");
				}
			}
			
			<#list workers as w>
				$("#${w.id}_minus_procDefs").hide();
				$("#${w.id}_minus_config").hide();
			</#list>
			
			<!--Call any potential adaptation specific updates -->
			adaptationWorkersReady();
			
			refreshStats();
			pageRefId  = setInterval(pageRefresh, refreshRate);
		});
	
		function pageRefresh(){
			if (refreshCount < 300) {
				refreshStats();
				refreshCount++;
				refreshRate = refreshRate + 25;
			}
			else{
				$("#page-ref-modal").modal('show');
				clearInterval(pageRefId);
			}
		}
		
		$( window ).load(function() {
			console.log( "window loaded" );
		});
	</script>
	
	<!-- Just for debugging purposes. Don''t actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->
	
	<style type="text/css">
	#down-div {
		height: 50px;
		float: right;
	}
	</style>
</head>

<body>

<#include "navbar.ftl">

<div class="container-fluid">
	<div class="row">
	
		<#include "sidebar.ftl">

		<div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">

			<span id="statusMessageDiv"><h2>${msg}</h2></span>
			
			<h2 class="sub-header">${workersTitle}</h2>
			<div id="down-div">
				<label for="hide-down">Hide All Down Workers</label>
				<input name="hide-down" id="hide-down-btn" type="checkbox">
			</div>
			<h3 class="sub-header">Workers</h3>
			<div class="table-responsive">
				<table id="workers-table" class="table table-striped table-bordered" border="1">
					<thead>
						<tr>
							<th width="20%">Name</th>
							<th width="5%">Status</th>
							<th width="10%"># Active</th>
							<th width="35%">Configuration</th>
							<th width="30%">Process Definitions</th>
						</tr>
					</thead>
					<tbody>
						<#list workers as w>
							<#if w.cwsInstallType != "console_only">
							<tr <#if w.status == "down">class="danger"</#if> >
								<td>${w.name}<#if w.cwsWorkerType == "run_models_only"><sup>M</sup></#if><#if w.cwsWorkerType == "run_external_tasks_only"><sup>E</sup></#if></td>
								<td>${w.status}</td>
								<td><div id="${w.id}_numRunningProcs" data-maxThreads="${w.jobExecutorMaxPoolSize}"></div></td>
								<td>
									<image id="${w.id}_plus_config"  src="/${base}/images/plus2.png" onclick="expandConfig('${w.id}');" />
									<image id="${w.id}_minus_config" src="/${base}/images/minus2.png" onclick="collapseConfig('${w.id}');" />
									<span id="${w.id}_configOverview">show details</span>
									<div id="${w.id}_updateConfigStatus"></div>
									<table id="${w.id}_configTable" class="table table-striped table-bordered" style="display:none" >
										<thead>
											<tr>
												<th width="50%">Setting</th>
												<th width="50%">Value</th>
											</tr>
										</thead>
										<tr>
											<td>
												CWS Installation Type:
											</td>
											<td id="${w.id}_install_type">${w.cwsInstallType}</td>
										</tr>
										<tr>
											<td>
												CWS Worker Type:
											</td>
											<td id="${w.id}_worker_type">${w.cwsWorkerType}</td>
										</tr>
										<tr>
											<td>
												Worker ID:
											</td>
											<td id="${w.id}_id"><small>${w.id}</small></td>
										</tr>
										<tr>
											<td>
												# Executor Threads:
											</td>
											<td>
												 <input type="text" id="${w.id}_execThreads" value="${w.jobExecutorMaxPoolSize}" maxlength="5" size="5" onblur="updateExecThreads('${w.id}', this.value);" />
											</td>
										</tr>
									</table>
								</td>
								<td>
									<image id="${w.id}_plus_procDefs"  src="/${base}/images/plus2.png" onclick="expandProcDefs('${w.id}');" />
									<image id="${w.id}_minus_procDefs" src="/${base}/images/minus2.png" onclick="collapseProcDefs('${w.id}');" />
									<span id="${w.id}_procDefsOverview">${w.enabledCount}  / ${procDefs?size}  process definitions enabled</span>
									<table id="${w.id}_procDefsTable" class="table table-striped table-bordered" style="display:none">
										<thead>
											<tr>
												<th width="70%">Process</th>
												<th width="30%">Limit</th>
											</tr>
										</thead>
										<tbody>
											<#list w.procInstanceLimits?keys as procDefKey>
												<tr>
													<td>
														<input type="checkbox" id="${w.id}_${procDefKey}_enabled" value="on" <#if w.procInstanceLimits[procDefKey]??>checked="checked"</#if> onchange="updateProcDefEnabled('${w.id}', '${procDefKey}', this.checked);" /> ${procDefKey} <div id="${w.id}_${procDefKey}_updateStatus"></div>
													</td>
													<td>
														<#setting number_format="computer">
														<#if w.procInstanceLimits[procDefKey]??>
															<input type="text" id="${w.id}_${procDefKey}_limit" value="${w.procInstanceLimits[procDefKey]}" maxlength="10" size="10" onblur="updateProcLimit('${w.id}', '${procDefKey}', this.value);" />
														<#else>
															<input type="text" id="${w.id}_${procDefKey}_limit" value="1" maxlength="10" size="10" onblur="updateProcLimit('${w.id}', '${procDefKey}', this.value);" style="display:none;" />
														</#if>
													</td>
												</tr>
											</#list>
										</tbody>
									</table>
								</td>
							</tr>
							</#if>
						</#list> <!-- list of workers -->
					</tbody>
				</table>
			</div>
			
			<h3 class="sub-header">External Workers</h3>
			<div class="table-responsive">
				<table id="ext-workers-table" class="table table-striped table-bordered" border="1">
					<thead>
						<tr>
							<th width="10%">Name</th>
							<th width="10%">Hostname</th>
							<th width="15%">Active Topics</th>
							<th width="15%">Current Topic</th>
							<th width="35%">Current Command</th>
							<th width="15%">Current Working Dir</th>
						</tr>
					</thead>
					<tbody>
						<#list externalWorkers as w>
							<tr>
								<td>${w.name}</td>
								<td>${w.hostname}</td>
								<td><#if w.activeTopics??>${w.activeTopics}<#else>None</#if></td>
								<td><#if w.currentTopic??>${w.currentTopic}<#else>None</#if></td>
								<td><#if w.currentCommand??>${w.currentCommand}<#else>None</#if></td>
								<td><#if w.currentWorkingDir??>${w.currentWorkingDir}<#else>None</#if></td>
							</tr>
						</#list> <!-- list of externalWorkers -->
					</tbody>
				</table>
			</div>
			
			<h3 class="sub-header">AMQ Clients</h3>
			<div class="table-responsive">
				<table class="table table-striped table-bordered">
					<thead>
						<tr>
							<th>Remote Address</th>
							<th>Active</th>
							<th>Connected</th>
						</tr>
					</thead>
					<tbody>
						<#list amqClients as x>
							<tr>
								<td>${x.remoteAddress}</td>
								<td>${x.active?c}</td>
								<td>${x.connected?c}</td>
							</tr> 
						</#list>
					</tbody>
				</table>
			</div>
			<div style="color:#808285;">
				<hr>
				<p><sup>M</sup> &ndash; Run Models Only</p>
				<p><sup>E</sup> &ndash; Run External Tasks Only</p>
			</div>
		</div>
	</div>
</div>

<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="/${base}/js/bootstrap.min.js"></script>
<script src="/${base}/js/docs.min.js"></script>

<script type="text/javascript">
	$("#hide-down-btn").click(function() {
		if ($(this).prop("checked")) {
			$('#workers-table tbody tr').filter(function () {
		        return $.trim($(this).find('td').eq(1).text()) === "down"
		    }).hide();
			localStorage.setItem(hideDownWorkersVar, "1");
		}
		else {
			$('#workers-table tbody tr').filter(function () {
		        return $.trim($(this).find('td').eq(1).text()) === "down"
		    }).show();
			localStorage.setItem(hideDownWorkersVar, "0");
		}
	});

	if(localStorage.getItem(hideDownWorkersVar) === "1") {
		$("#hide-down-btn").prop("checked", true);
		$('#workers-table tbody tr').filter(function () {
	        return $.trim($(this).find('td').eq(1).text()) === "down"
	    }).hide();
	} else {
		$("#hide-down-btn").prop("checked", false);
		$('#workers-table tbody tr').filter(function () {
	        return $.trim($(this).find('td').eq(1).text()) === "down"
	    }).show();
	}
</script>
</body>
</html>
	