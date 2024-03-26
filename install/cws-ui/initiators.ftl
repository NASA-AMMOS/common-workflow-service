<!DOCTYPE html>

<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Initiators</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/jquery.migrate.js"></script>
	<script src="/${base}/js/ace/ace.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<script src="/${base}/js/bootstrap.min.js"></script>

	<style type="text/css">
	#save-table td:nth-child(2){
		padding-right: 2em; 
		width:12%;
	}
	#workers-div {
		overflow: auto;
		max-height: 500px;
	}
	#selAll-label{
		cursor: pointer;
	}
	#workers-div div{
		margin: 10px;
		float:left; width:155px;
	}
	#workers-div div input{
		margin: 0 5px;
		/*transform:scale(1.2);*/
	}
	#workers-div div label{
		margin:0 5px;
		cursor: pointer;
	}
	</style>
	
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

<div class="container-fluid">
	<div class="row">
		<#include "sidebar.ftl">
		
		<div class="main-content">
			<span id="statusMessageDiv"><h2>${msg}</h2></span>

			<h2 class="sub-header">Initiators</h2>
			<div id="editorDiv">Code holder</div>
			

				<table class="table" id="save-table">
					<thead>
						<tr>
							<th></th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td><input id="saveXmlBtn" type="button" class="btn btn-primary" value="Save the XML file"/></td>
							<td>
								<label>Enable All</label>
								<div class="slide-switch" id="active-all">
									<input id="activate-all-inits" type="checkbox">
									<label for="activate-all-inits"><span>Power</span></label>
								</div>
							</td>
						</tr>
					</tbody>
				</table>
				<div id="beans-table">
					<div class="ajax-spinner"></div>
					<#include "initiators-table.ftl" />
				</div>
			
		</div>
	</div>
</div>

<div class="modal fade" id="saveMsg" role="dialog">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title">WARNING!</h4>
			</div>

            <div class="modal-body">
                <p>This will stop and replace any initiators which have been changed on this page. Unchanged initiators continue running unaffected.</p>
                <b>NOTE:<br/>&nbsp;&nbsp;If there is an error in the new configuration, existing initiators will remain unchanged. However, changes made earlier in the file (before the invalid initiator configuration) will be applied.</b>
            </div>

			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
				<button type="button" class="btn btn-primary" id="saveConfirmBtn">Confirm</button>
			</div>
		</div> <!-- modal-content -->
	</div> <!-- modal-dialog -->
</div> <!-- .modal .fade -->

<div class="modal fade" id="workers-modal" role="dialog" data-backdrop="static" data-keyboard="false">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
	        	<h4 class="modal-title">WARNING!</h4>
			</div>

			<div class="modal-body">
				<span>The process definition '<b id="proc-def-key">Undefined</b>' is not enabled for any workers.  In order to proceed, you must enable it for at least one worker.</span>
				<hr/>
				<div>
					<input type="checkbox" id="all-workers" />&nbsp;
					<label for="all-workers" id='selAll-label'>Select All Workers</label>
				</div>
				<hr/>
				<div id="workers-div"></div>
				<hr/>
				<span style="color:#666; font-size:95%">
					<strong>*It's recommended that this worker always be selected.</strong>
					<p>This is because all manual tasks (i.e. User Tasks and manual process starts via the TaskList app) 
					are initiated via this worker.</p>
				</span>
				<hr />
				<span>Note: Grayed out workers are currently down.</span>
			</div>

			<div class="modal-footer">
				<button id="cancel-workers-btn" type="button" class="btn btn-default">Cancel</button>
				<button id="done-workers-btn" type="button" class="btn btn-primary">Done</button>
			</div>
		</div> <!-- modal-content -->
	</div> <!-- modal-dialog -->
</div> <!-- .modal .fade -->

<div class="modal fade" id="xmlErrorMsg" role="dialog">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title">ERROR!</h4>
			</div>

			<div class="modal-body">
				<p>There was an error in the XML file. Please review the syntax and click on 'Save the XML file' again.</p>
				<div id="errorMsg">XXX</div>
			</div>

			<div class="modal-footer">
				<button type="button" class="btn btn-primary" data-dismiss="modal">Dismiss</button>
			</div>
		</div> <!-- modal-content -->
	</div> <!-- modal-dialog -->
	
</div> <!-- .modal .fade -->
<div class="modal fade" id="cancelledByUserMsg" role="dialog">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title">WARNING!</h4>
			</div>

			<div class="modal-body">
				<p>The XML file was not saved.  Cancelled by user.</p>
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
<script>


	var dataProcKey;
	
		/*$( window ).load(function() {
			console.log( "window loaded" );
		});*/

	$("#saveXmlBtn").on("click", function(){
		$("#saveMsg").modal();
	});

	$("#saveConfirmBtn").on("click", function() {
		$("#beans-table .ajax-spinner").show();
		setTimeout(function(){ refreshInitiators(); }, 1000);
		$("#saveMsg").modal('hide');
	});
	
	$("#done-workers-btn").on("click", function(){
		$("#beans-table .ajax-spinner").show();
		setTimeout(function(){ refreshInitiators(); }, 750);
		$("#workers-modal").modal('hide');
	});
	
	$("#cancel-workers-btn").on("click", function(){
		$("#workers-modal").modal('hide');
		$("#cancelledByUserMsg").modal('show');
	});

	
	//
	// Sets the enabled flag on a process initiator
	//
	function setEnabled(initiatorId) {
		var cb = $("#toggle_"+initiatorId);
		
		// DISABLE THE CHECKBOX FOR A SECOND, SO USERS DON'T GO HOGWILD
		//
		cb.prop("disabled", true);
		setTimeout(function(){
			$("#toggle_"+initiatorId).prop("disabled", false);
		}, 1000);
		
		$.ajax({
			type: "POST",
			url: "/${base}/rest/initiators/" + initiatorId + "/enabled",
			data: { enabled: cb.is(':checked') }
		})
		.done(function( msg ) {
			//alert( "Data Saved: " + msg );
			refreshIcons();
		});
	}

	//
	// Sets the enabled flag on all initiators
	//
	function setAllEnabled() {
		$.ajax({
			type: "POST",
			url: "/${base}/rest/initiators/all/enabled",
			data: { enabled: $("#active-all input").is(':checked') }
		})
		.done(function( msg ) {
			//alert( "Data Saved: " + msg );
			refreshIcons();
			$("#beans-table .ajax-spinner").hide();
		});
	}

	$("#active-all input").on('click',function(){
		console.log('activate all');
		$("#beans-table .ajax-spinner").show();
		$(this).prop('disabled', true);
		setAllEnabled();
		
		setTimeout(function(){
			$("#active-all input").prop("disabled",false);
		}, 1200);
	});
	
	
	//
	// Refreshes the initiators table
	//
	function refreshTable() {
		// SHOW SPINNER
		$("#beans-table .ajax-spinner").show();

		// CLEAR CONTENTS OF TABLE
		//
		$("#beans-table").html('');
		
		// LOAD TABLE FROM SERVER
		//
		$("#beans-table").load("/${base}/rest/initiators/getInitiatorsHtmlTable", 
		 	function() {
				$("#beans-table").prepend("<div class='ajax-spinner'></div>");
				refreshIcons();
			}
		);

		////if any rows added to the table
		//if ($("#beans-table table tr").length > 1) {
		//	$("#beans-table").prepend("<div><h1>Test</h1></div>");
		//}

		// HIDE SPINNER
		$("#beans-table .ajax-spinner").hide();
	}


	//
	// Refreshes the play/pause icons based on current state of
	// initiators
	//
	function refreshIcons() {
		//
		// DETERMINE WHICH INITIATORS ARE ENABLED
		//
		initiatorEnabled = {};
		$.ajax({
			type:     "GET",
			url:      "/${base}/rest/initiators/all/enabled",
			dataType: "text",
			async:    false
		})
		.done(function( enabled ) {
			initiatorEnabled = JSON.parse(enabled);
		});

		//if there is no "false" in the list, then all are enabled.
		if (Object.values(initiatorEnabled).indexOf("false") == -1) {
			$("#active-all input").prop('checked', true);
		}
		else {
			$("#active-all input").prop('checked', false);
		}

		//
		// TURN ON SWITCHES FOR ENABLED INIATORS
		//
		$('input').each(function() {
			var thisId = $(this).attr('id');
			if ( thisId != undefined && thisId.match(/toggle_/) ) {
				var initiatorId = "";
				initiatorId = thisId.substring(7);
				
				if (initiatorEnabled[initiatorId] == "true") {
					$("#toggle_"+initiatorId).prop('checked', true);
					$("#status_"+initiatorId).html("enabled");
				}
				else if (initiatorEnabled[initiatorId] == "false") {
					$("#toggle_"+initiatorId).prop('checked', false);
					$("#status_"+initiatorId).html("disabled");
				}
				else {
					$("#status_"+initiatorId).html("error getting info");
					$("#toggle_"+initiatorId).hide();
				}
			}
			else {
				//alert("no match");
			}
		});
	}

	function checkIfProcDefKeyDeployed(procDefKey) {
	
		$.ajax({
			url: "/${base}/rest/isProcDefKeyDeployed",
			method: "POST",
			data: { "procDefKey" : procDefKey},
			dataType: "text",
			success: function(val, status) {
				if (val === "true") {
 		
					listWorkersInModal(procDefKey);
					$("#workers-modal").modal('show');

				}
				else {
					$("#errorMsg").html('<hr/>The procDefKey \'<b>' + procDefKey + '</b>\' is not found.  Please check your spelling and/or make sure this procDefKey is deployed.');
					
					$("#xmlErrorMsg").modal('show');
				}

				$("#beans-table .ajax-spinner").hide();
			}
		});
	}
	
	
	//
	//  CALLED WHEN USER CLICKS "Save the XML file" BUTTON
	//
	function refreshInitiators() {
		console.log("refreshInitiators()");
		var editor = ace.edit("editorDiv");
		var code = editor.getSession().getValue();
		editor.setBehavioursEnabled(false);
		
		// MAKE AJAX CALL TO UPDATE INITIATORS
		//
		$.ajax({
			url: "/${base}/rest/initiators/updateChangedInitiators",
			method: "POST",
			data: { "newXmlContext" : code},
			dataType: "text",
			success: function(val, status){
				if (val == "success") {
					refreshTable();
				}
				else{
					// Unsuccessful, check if procDef is not found and then retrieve dataProcKey from 'val'
					//
					dataProcKey = "";
					
					var i1 = val.indexOf('no row for procDef');
					var i2 = -1;
					
					if (i1 > 0) {
						// Parse 'val' message...e.g. "ERROR MESSAGE: One or more initiators invalid: {repeat_1=no row for procDef 'mozartWorkflowMock' exists in DB!}"
						//
					    i1 = val.indexOf('\'', i1);
					    
					    if (i1 > 0) {
					        i1++;
					    	i2 = val.indexOf('\'', i1);
					    	
					    	if (i2 > 0) {
					    		dataProcKey = val.substring(i1, i2);
					    		
					    		checkIfProcDefKeyDeployed(dataProcKey);
					    	}
					    }
					}
					    
					if (dataProcKey === "") {
						$("#errorMsg").html(val);
						$("#xmlErrorMsg").modal('show');						
						$("#beans-table .ajax-spinner").hide();
					}
				}
			}
		});
	}
	
	function enableDisable(wid){
		var enabledFlag = $("#"+wid+"-box").prop("checked");
		var postUrl = "/${base}/rest/worker/"+wid+"/"+dataProcKey+"/updateWorkerProcDefEnabled/"+enabledFlag;
		$.post( postUrl, function( data ) {
				
			if (data == "success") {
				if (enabledFlag == true) {
					$( "#" + wid + "-msg" ).html( '<font color="green">enabled</font>' );
				}
				else {
					$( "#" + wid + "-msg" ).html( '<font color="green">disabled</font>' );
				}
				$( "#" + wid + "-msg font" ).fadeOut(2000, "linear");
			}
			else {
				$( "#" + wid + "-msg" ).html( '<font color="red">update failed</font>' );
			}

			//check the select/deselect checkbox if all workers are selected
			if($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length){
				$("#all-workers").prop('checked',true);
			}
			else
				$("#all-workers").prop('checked',false);
			
		});
	}
	
	//
	// CLICK ACTION FOR
	// "Select All Workers" checkbox in modal
	//
	$("#all-workers").on("click", function() {
		if($(this).prop("checked")) {
			$(".worker-checkbox").each(function() {
				if(!$(this).prop("checked") )
					$(this).trigger("click");
			});
		}
		else{
			$(".worker-checkbox").each(function() {
				if($(this).prop("checked") )
					$(this).trigger("click");
			});
		}
	});
	
	function listWorkersInModal(dataProcKey){
		$("#proc-def-key").html(dataProcKey);
		$.get("/${base}/rest/worker/"+dataProcKey+"/getWorkersForProc", function(data){
			$("#workers-div").html('');
			//Returned JSON is an array of objects
			var listWorkers = JSON.parse(data);
			//$.each(JSON.parse(data), function(i) {
			for(i in listWorkers){
				var div = "<div>" +
				"<input type='checkbox' id='"+listWorkers[i].id+"-box' " + " class='worker-checkbox' " +
				( (listWorkers[i].status == 'down') ? " disabled='disabled'" : '') +
				" onClick=\'enableDisable(\""+listWorkers[i].id.toString()+"\");\' "+(listWorkers[i].accepting_new ? "checked" :"")+"/>"+
				"<label for='"+listWorkers[i].id+"-box'"+( (listWorkers[i].status == 'down') ? " class='w-down'" : '')+">"+listWorkers[i].name+ 
				(listWorkers[i].cws_install_type == 'console_only'? '*' : '') +"</label>"+
				"<span id='"+listWorkers[i].id+"-msg'>"+
				"</div>";
				$("#workers-div").append(div);
			}


			//check the select/deselect checkbox if all workers are selected
			if($("#workers-div input[type='checkbox']:checked").length === $("#workers-div input[type='checkbox']").length){
				$("#all-workers").prop('checked',true);
			}
			else
				$("#all-workers").prop('checked',false);
			
			$("#workers-modal").modal('show');
		});
	}

	$( document ).ready(function() {
		// DISPLAY A RED ERROR MESSAGE, IF THERE IS AN ERROR.
		// OTHERWISE DISPLAY A GREEN MESSAGE
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
		
		//
		// LOAD THE XML EDITOR WITH CONTENTS FROM INITIATORS WORKING FILE
		//
		$( "#editorDiv").load("/${base}/rest/initiators/getXmlContextFile", function(data){
			data = 
			$("#editorDiv").text(data);
			var editor = ace.edit("editorDiv");
			editor.setTheme("ace/theme/monokai");
			editor.getSession().setMode("ace/mode/xml");
			editor.setOptions({
				maxLines: 35
			});
			editor.setBehavioursEnabled(false);
		});

		// REFRESH THE ICONS IN THE INITIATORS TABLE
		//
		setTimeout(function(){ refreshTable(); }, 500);
	});
	
</script>

</body>
</html>
