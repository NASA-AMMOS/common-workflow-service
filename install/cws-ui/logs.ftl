<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>CWS-Logs</title>
		<!--Load JS Libraries-->
		<script src="/${base}/js/jquery.min.js"></script>
		<script src="/${base}/js/popper.min.js"></script>
		<script src="/${base}/js/bootstrap.min.js"></script>
		<script src="/${base}/js/bootstrap-toggle.min.js"></script>
		
		<script src="/${base}/js/moment.js"></script>
		<script src="/${base}/js/moment-timezone.js"></script>
		<script src="/${base}/js/moment-timezone-with-data.js"></script>
		<script src="/${base}/js/DataTables/datatables.js"></script>
		<script src="/${base}/js/DataTablesDateFilter.js"></script>
		<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
		<script src="/${base}/js/DataTables/dataTables.responsive.min.js"></script>
		<script src="/${base}/js/DataTables/responsive.bootstrap.min.js"></script>
		<script src="/${base}/js/cws.js" type="text/javascript"></script> 
		<!--Load CSS Stylesheets-->
		<link href="/${base}/css/bootstrap.min.css"rel="stylesheet">
		<link href="/${base}/css/bootstrap-datepicker.min.css"rel="stylesheet">
		<link href="/${base}/js/DataTables/datatables.css"rel="stylesheet">
		<link href="/${base}/css/dashboard.css"rel="stylesheet">
		<link href="/${base}/css/logs.css"rel="stylesheet">
		
		<!--Just for debugging purposes.Don't actually copy this line! -->
		<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->
		<!--HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries-->
		<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
		<![endif]-->
		<script type="text/javascript">
			//STATE PERSISTANCE CONSTS
			var username = document.cookie.substring(document.cookie.indexOf("cwsUsername=") + 12);
			if (username.indexOf(";") > 0) {
				username = username.substring(0, username.indexOf(";"));
			}
			const refreshRateVar="CWS_DASH_LOGS_REFRESH_RATE-"+username;
			const refreshVar="CWS_DASH_LOGS_REFRESH-"+username;
			const qstringVar="CWS_DASH_LOGS_QSTRING-"+username;
		
			// Global vars
			var params;
			var FETCH_COUNT=200;
			var htmlTableRows=[];
			var renderFlag=0;
			var workerIdArr = [];
			<#list workerIds as workerId>
				workerIdArr.push("${workerId}");
			</#list>
		
			var now = moment().utcOffset('+0000').format("YYYY-MM-DDTHH:mm:ss.SSSSSS");
			var refID;
		
			var baseEsReq={
				"from":0,
				"size":FETCH_COUNT,
				"query":{
					"bool":{
						"must":[]
					}
				},
				"sort":{
				}
			};
			var mainEsReq;
		
			//----------------------------
			function getEsReq(){
				renderFlag=0;
				htmlTableRows=[];
				var esReq=baseEsReq;
				if (params !== undefined && params !== null) {
					if(params.procDefKey !== undefined && params.procDefKey !== null){
						esReq.query.bool.must.push({"match":{"procDefKey":params.procDefKey}});
					}
					if(params.logLevel  !== undefined && params.logLevel !== null){
						esReq.query.bool.must.push({"match":{"logLevel":params.logLevel}});
					}
					if(params.program  !== undefined && params.program !== null){
						esReq.query.bool.must.push({"match":{"program":params.program}});
					}
					if(params.procInstId  !== undefined && params.procInstId !== null){
						//esReq.query.bool.must.push({"match":{"procInstId" : decodeURIComponent(params.procInstId)}});
						esReq.query.bool.must.push({"query_string":{"fields":["procInstId"],"query":"\""+decodeURIComponent(params.procInstId)+"\""}});
					}
					if(params.search  !== undefined && params.search !== null){
						esReq.query.bool.must.push({"query_string":{"fields":["msgBody"],"query":decodeURIComponent(params.search)}});
					}
					if (params.workerId !== undefined && params.workerId !== null) {
						esReq.query.bool.must.push({"match":{"cwsWorkerId":params.workerId}});
					}
			
					var startDate=params.startDate?decodeURIComponent(params.startDate):"";
					var endDate=params.endDate?decodeURIComponent(params.endDate):"";
			
					if(startDate!=""){
						//esReq.query.range["@timestamp"].gte = startDate;
						esReq.query.bool.must.push({"range":{"@timestamp":{"gte":startDate}}});
					}
					if(endDate!=""){
						//esReq.query.range["@timestamp"].lte = endDate;
						esReq.query.bool.must.push({"range":{"@timestamp":{"lte":endDate}}});
					}
			
					$("#pd-select").val(params.procDefKey||"def");
					//$("#level-select").val(params.logLevel || "def");
					if(params.logLevel){
						params.logLevel.split(',').forEach(function(lvl){
							$("#log-level-sel input[value='"+lvl+"']").prop("checked",true);
						});
					}
					if (esReq.query.bool.must.length == 0) {
						esReq.query.bool.must.push({"match": {"logLevel":"TRACE,DEBUG,INFO,WARN,ERROR"}});
					}
					$("#pi-text").val(params.procInstId?decodeURIComponent(params.procInstId):"");
					$("#worker-id-text").val(params.workerId?decodeURIComponent(params.workerId):"");
					$("#search-text").val(params.search?decodeURIComponent(params.search):"");
					$("#start-date").val(startDate);
					$("#end-date").val(endDate);
				}
				return esReq;
			}
		
			$(document).ready(function(){
			//show ajax spinner
			$(".ajax-spinner").show();
			document.addEventListener("click", function (e) {
				closeAllLists(e.target);
			});
		
			params = getQueryString();
			mainEsReq = getEsReq();
			//push our timestamp to the esreq
			mainEsReq.query.bool.must.push({"range": {"@timestamp": {"lte": now}}});
		
			// DISPLAY MESSAGE AT TOP OF PAGE
			//
			if($("#statusMessageDiv:contains('ERROR:')").length>=1){
				$("#statusMessageDiv").css("color","red");
			} else if($("#statusMessageDiv h2").text()!=""){
				$("#statusMessageDiv").css("color","green");
				if($('#statusMessageDiv').html().length>9){
					$('#statusMessageDiv').fadeOut(5000,"linear");
				}
			}
		
			$("#logData").DataTable({
				language: {
					searchBuilder: {
						add: "<img height=\"16\" width=\"16\" src=\"/${base}/images/search.svg\" /> Add Local Filter",
					}
				},
				deferRender: true,
				stateSave: true,
				scroller: {
					boundaryScale: 0.50,
					displayBuffer: 20,
					loadingIndicator: true,
				},
				scrollY: 600,
				serverSide: true,
				searchDelay: 250,
				dom: 'Bfrtip',
				"initComplete": function(settings, json) {
					//hide ajax spinner
					$(".ajax-spinner").hide();
					if ($("#logData_info").text().includes(" of 10,000 entries")) {
					$("#warning-msg").show();
					} else {
						$("#warning-msg").hide();
					}
					$(".messageStyle").each(function(i, obj) {
					if (obj.scrollHeight > obj.clientHeight) {
						$(obj).css("resize", "vertical");
					} else {
						$(obj).css("resize", "none");
					}
				});
				},
				order: [[0, 'desc']],
				columnDefs: [
					{
						targets: [1,2,5,6],
						visible: false,
					},
					{
						targets: [0],
						width: "215px",
					},
					{
						targets: [1],
						width: "75px"
					},
					{
						targets: [3],
						width: "75px"
					},
					{
						targets: [4],
						width: "150px"
					},
				],
				buttons: [
					{
						extend: 'colvis',
						columns: ':not(.noVis)',
						className: 'btn btn-primary',
						text: '<img height="16" width="16" src="/${base}/images/visible_show_dark.svg" style="margin-right: 5px; margin-bottom: 3px;" />Columns',
					}
				],
				dom: "<'above-table-div'<'above-table-buttons'B><'above-table-length'i><'above-table-filler'><'above-table-filter'f>>"
		+ "t"
		+ "<'below-table-div'p>",
				ajax: function (data, callback, settings) {
					//store our draw value
					var draw = data.draw;
		
					//get total number of records
					var totalRecords = 0;
					$.ajax({
						url: "/${base}/rest/logs/get/count",
						type: "GET",
						async: false,
						success: function (data) {
							totalRecords = data.count;
						},
						error: function (jqXHR, textStatus, errorThrown) {
							console.log("Error getting total number of records: " + errorThrown);
						}
					});
					//update our esReq with the new from value
					mainEsReq.from = parseInt(data.start);
					
					//we need to change the esReq to return the requested number of elements
					mainEsReq.size = data.length;
					//we need to support sorting
					//get the column # and direction we are sorting
					var sortCol = data.order[0].column;
					var sortDir = data.order[0].dir;
					//get the column name
					//NOTE: ES does not support sorting on all data types (especially text)
					//However, since ES 5, text fields have a keyword subfield that can be used for sorting
					var sortColName = "";
					switch (sortCol) {
						case 0:
							sortColName = "@timestamp";
							break;
						case 1:
							sortColName = "cwsHost.keyword";
							break;
						case 2:
							sortColName = "cwsWorkerId.keyword";
							break;
						case 3:
							sortColName = "logLevel.keyword";
							break;
						case 4:
							sortColName = "threadName.keyword";
							break;
						case 5:
							sortColName = "procDefKey.keyword";
							break;
						case 6:
							sortColName = "procInstId.keyword";
							break;
						case 7:
							sortColName = "msgBody.keyword";
							break;
						default:
							sortColName = "@timestamp";
							break;
					}
					//update the esreq
					mainEsReq.sort = {};
					mainEsReq.sort = {
						[sortColName]: {
							"order": sortDir
						}
					};
					var returnData;
					var fetchError = "";
					var local_esReq = JSON.stringify(mainEsReq);
					local_esReq = encodeURIComponent(local_esReq);
					//sometimes double quotes get left here? replace double quotes with url encoded
					local_esReq = local_esReq.replace(/"/g, "%22");
					$.ajax({
						url: "/${base}/rest/logs/get/noScroll",
						data: "source=" + local_esReq,
						type: "GET",
						async: false,
						success: function (ajaxData) {
							returnData = ajaxData;
							//we should have our data now. We need to format it for the table
							var formattedData = [];
							for (hit in returnData.hits.hits) {
								//apply our local search here too
								var hitData = returnData.hits.hits[hit]._source;
								var formattedRow = {};
								if (hitData["@timestamp"] !== undefined) {
									formattedRow["timestamp"] = hitData["@timestamp"];
								} else {
									formattedRow["timestamp"] = "";
								}
								if (hitData["cwsHost"] !== undefined) {
									formattedRow["cws_host"] = hitData["cwsHost"];
								} else {
									formattedRow["cws_host"] = "";
								}
								if (hitData["cwsWorkerId"] !== undefined) {
									formattedRow["cws_worker_id"] = hitData["cwsWorkerId"];
								} else {
									formattedRow["cws_worker_id"] = "";
								}
								if (hitData["logLevel"] !== undefined) {
									formattedRow["log_level"] = hitData["logLevel"];
								} else {
									formattedRow["log_level"] = "";
								}
								if (hitData["threadName"] !== undefined) {
									formattedRow["thread_name"] = hitData["threadName"];
								} else {
									formattedRow["thread_name"] = "";
								}
								if (hitData["procDefKey"] !== undefined) {
									formattedRow["procDefKey"] = hitData["procDefKey"];
								} else {
									formattedRow["procDefKey"] = "";
								}
								if (hitData["procInstId"] !== undefined) {
									formattedRow["procInstId"] = hitData["procInstId"];
								} else {
									formattedRow["procInstId"] = "";
								}
								if (hitData["msgBody"] !== undefined) {
									formattedRow["message"] = hitData["msgBody"];
								} else {
									formattedRow["message"] = "";
								}
								if (Object.values(formattedRow).join("").toUpperCase().includes(data.search.value.toUpperCase())) {
									formattedData.push(formattedRow);
								}
							}
				
							//we can get the # of filtered requests from the returnData
							var filteredRecords = returnData.hits.total.value;
				
							//now we need to build our return object
							var returnObj = {
								"draw": draw,
								"recordsTotal": totalRecords,
								"recordsFiltered": filteredRecords,
								"data": formattedData,
								"error": fetchError
							}
							//console.error("RETURN OBJ: " + JSON.stringify(returnObj));
							callback(returnObj);
						},
						error: function (jqXHR, textStatus, errorThrown) {
							fetchError = "Error getting initial data: " + errorThrown;
							returnData = fetchError;
						}
					});
					
				},
				columns: [
					{
						data: "timestamp",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "cws_host",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "cws_worker_id",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "log_level",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "thread_name",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "procDefKey",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "procInstId",
						render: function (data, type) {
							return data;
						}
					},
					{
						data: "message",
						render: function (data, type) {
							if (type == "display") {
								return "<p class=\"messageStyle\">" + data.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>";
							} else {
								return data;
							}
						}
					}
				],
			});
			$('#logData').DataTable().on("draw.dt", function() {
				$(".messageStyle").each(function(i, obj) {
					if (obj.scrollHeight > obj.clientHeight) {
						$(obj).css("resize", "vertical");
					} else {
						$(obj).css("resize", "none");
					}
				});
			});
		
			if(localStorage.getItem(refreshRateVar)===null){
				localStorage.setItem(refreshRateVar,"10000");
			}
		
			$("#refresh-rate").val((parseInt(localStorage.getItem(refreshRateVar))/1000).toString());
			if(localStorage.getItem(refreshVar)===null){
				localStorage.setItem(refreshVar,"false");
			}
			if(localStorage.getItem(refreshVar)=="true"){
				$("#refresh-checkbox").prop("checked",true);
				refID=setInterval(refreshLogs,parseInt(localStorage.getItem(refreshRateVar)));
			}
			$("#refresh-rate").change(function(){
				localStorage.setItem(refreshRateVar,$(this).val()*1000);
				if($("#refresh-checkbox").is(":checked")){
					clearInterval(refID);
					refID=setInterval(refreshLogs,parseInt(localStorage.getItem(refreshRateVar)));
				}
			});
			//onchange for checkbox
			$("#refresh-checkbox").change(function(){
				if($(this).is(":checked")){
					localStorage.setItem(refreshVar,"true");
					refID=setInterval(refreshLogs,parseInt(localStorage.getItem(refreshRateVar)));
				} else {
					localStorage.setItem(refreshVar,"false");
					clearInterval(refID);
				}
			});
		
			//get our current url
			var currentUrl=window.location.href;
			//get our local storage url
			var localStorageUrl=localStorage.getItem(qstringVar);
			//check if a cookie has been stored (indicating we can restore state)
			if(localStorageUrl!=null){
				//remove everything before ?
				currentUrl=currentUrl.substring(currentUrl.indexOf("logs")+4);
				//compare against what is in local storage
				if(currentUrl!=localStorageUrl){
					//if they are different, go to the one in local storage (essentially restoring from last time used)
					window.location="/${base}/logs"+localStorageUrl;
				}
			}
		
			//GET query string values
			params=getQueryString();
			$("#start-date").datepicker({
				orientation: 'left top',
				todayBtn: 'true',
				todayHighlight: true
			});
			$("#end-date").datepicker({
				orientation: 'left top',
				todayBtn: 'true',
				todayHighlight: true
			});
			$("#search-text").keypress(function(e){
				if(e.which==13){
					e.preventDefault();
					$("#filter-submit-btn").trigger("click");
				}
			});
			$("#pi-text").keypress(function(e){
				if(e.which==13){
					e.preventDefault();
					$("#filter-submit-btn").trigger("click");
				}
			});
			$("#worker-id-text").keypress(function(e){
				if(e.which==13){
					e.preventDefault();
					$("#filter-submit-btn").trigger("click");
				}
			});
			$("#filter-submit-btn").on("click", function(e){
				e.preventDefault();
				window.location="/${base}/logs"+getFilterQString();
			});
		
			$("#filter-submit-btn").on("contextmenu",function(e){
				$(this).attr("href","/${base}/logs"+getFilterQString(false));
			});
			$("#filters-btn").on("click", function(){
				if($("#filters-div-flex").is(":visible"))
					$("#filter-arrow").attr("src","/${base}/images/chevron_down.svg");
				else
					$("#filter-arrow").attr("src","/${base}/images/chevron_up.svg");
		
				$("#filters-div-flex").slideToggle();
			});
			autocomplete(document.getElementById("worker-id-text"), workerIdArr);
		
			}); //END OF DOCUMENT.READY
		
			function getFilterQString(){
				var params={};
				if($("#pd-select").val()!="def"){
					params.procDefKey=$("#pd-select").val();
				}
				if($("#pi-text").val()!=""){
					params.procInstId=encodeURIComponent($("#pi-text").val());
				}
				if($("#worker-id-text").val()!=""){
					params.workerId=encodeURIComponent($("#worker-id-text").val());
				}
				if($("#log-level-sel input:checked").length>0){
					var lvl=[]
					$("#log-level-sel input:checked").each(function(){
						lvl.push($(this).val());
					});
					params.logLevel=lvl.toString();
				}
				if($("#search-text").val()!=""){
					params.search=encodeURIComponent($("#search-text").val());
				}
				if($("#start-date").val()!=""){
					params.startDate=encodeURIComponent($("#start-date").val());
				}
				if($("#end-date").val()!=""){
					params.endDate=encodeURIComponent($("#end-date").val());
				}
				var qstring="?";
		
				for(p in params){
					qstring+=p+"="+params[p]+"&";
				}
				qstring=qstring.substring(0,qstring.length-1);
				localStorage.setItem(qstringVar,qstring);
				//console.log(encodeURI(qstring));
				return qstring;
			}
		
			var today=new Date();
			var todayDate=today.getDate()+"/"+today.getMonth()+"/"+today.getFullYear();
			//$("#start-date").attr("value", todayDate);
		
			$("#start-date").datepicker({
				orientation:'left top',
				todayBtn:'true',
				todayHighlight:true
			});
		
			$("#end-date").datepicker({
				orientation:'left top',
				todayBtn:'true',
				todayHighlight:true
			});
			function refreshLogs(){
				$(".ajax-spinner").show();
				//update timestamp to grab new logs
				var oldNow = now;
				now=moment().utcOffset('+0000').format("YYYY-MM-DDTHH:mm:ss.SSSSSSZ");
				
				//find the condition with oldNow timestamp and update it to be now
				for (var i = 0; i < mainEsReq.query.bool.must.length; i++) {
					if (mainEsReq.query.bool.must[i].range !== undefined) {
						if (mainEsReq.query.bool.must[i].range["@timestamp"] !== undefined) {
							if (mainEsReq.query.bool.must[i].range["@timestamp"].lte !== undefined) {
								if (mainEsReq.query.bool.must[i].range["@timestamp"].lte == oldNow) {
									mainEsReq.query.bool.must[i].range["@timestamp"].lte = now;
								}
							}
						}
					}
				}
				$("#logData").DataTable().ajax.reload(function(){
					$(".ajax-spinner").hide();
				},false);
			}
			
		</script>
	</head>
	<body>
		<#include "navbar.ftl">
		<div class="container-fluid">
			<div class="row">
				<div class="col main">
					<#include "sidebar.ftl">
					<div class="main-content">
						
						<span id="statusMessageDiv">
							<h2>${msg}</h2>
						</span>
						
						<h2 class="sub-header">Logs</h2>
						<div id="filters-div-flex">
							<div id="filters-div-header">
								<h5>Filters:</h5>
							</div>
							<div id="filters-div-row">
								<div id="filters-div-col-1">
									<h6>Process Definitions</h6>
									<select id="pd-select">
										<option value="def">All Process Definitions</option>
										<#list procDefs as pd>
										<option value="${pd.key}">${pd.name}</option>
										</#list>
									</select>
									<h6>Process Instances</h6>
									<div class="autocomplete">
										<input id="pi-text"type="text"class="form-control"placeholder="Instance ID...">
									</div>
									<h6>Worker ID</h6>
									<div class="autocomplete">
										<input id="worker-id-text" type="text" class="form-control" placeholder="Worker ID...">
									</div>
								</div>
								<div id="filters-div-col-2">
									<h6>Log Level</h6>
									<div id="log-level-sel">
										<input type='checkbox'id='trace'value='TRACE'/>
										<label for='trace'>Trace</label><br/>
										<input type="checkbox"id='debug'value="DEBUG"/>
										<label for='debug'>Debug</label><br/>
										<input type="checkbox"id='info'value="INFO"/>
										<label for='info'>Information</label><br/>
										<input type="checkbox"id='warning'value="WARN"/>
										<label for='warning'>Warning</label><br/>
										<input type="checkbox"id='error'value="ERROR"/>
										<label for='error'>Error</label><br/>
									</div>
								</div>
								<div id="filters-div-col-3">
									<h6>Search by Keyword</h6>
									<input id="search-text"id="filter-text"type="text"class="form-control"placeholder="Search..."/>
									<h6>Start Date:</h6>
									<input id="start-date"class="form-control"placeholder="yyyy-mm-dd"
									data-date-format="yyyy-mm-dd"maxlength="10"type="text">
									<h6>End Date:</h6>
									<input id="end-date"class="form-control"placeholder="yyyy-mm-dd"
									data-date-format="yyyy-mm-dd"size="16"type="text">
								</div>
							</div>
							<div class="filter-div-submit">
								<b style="margin-top: auto; margin-bottom: auto; color: red; display: none;" id="warning-msg">Warning: Only the first 10,000 entries are displayed. Please narrow your search criteria.</b>
								<a id="filter-submit-btn" class="btn btn-info btn-sm" href="#">Filter</a>
							</div>
						</div>
						<div id="filter-btn-refresh-flexbox">
							<div class="icon-button">
								<div id="filters-btn"class="btn btn-warning btn-sm"><img height="16" width="16" src="/${base}/images/filter.svg" />
								</span>&nbsp;Filters&nbsp;<img id="filter-arrow" height="16" width="16" src="/${base}/images/chevron_up.svg" />
							</div>
						</div>
						<div style="margin:30px 15px;"class="pull-right">
							<input type="checkbox"id="refresh-checkbox"/>&nbsp;
							<label for='refresh-checkbox' style="color: black;">Refresh the logs every</label>&nbsp;
							<select id="refresh-rate">
								<option value="10">10 seconds</option>
								<option value="5">5 seconds</option>
								<option value="3">3 seconds</option>
								<option value="1">1 second</option>
							</select>
						</div>
					</div>
					
					<div id="log-div" style="width: 98%;">
						<div class="ajax-spinner"></div>
						<table id="logData"class="table table-striped table-bordered sortable" style="width: 100%">
							<thead>
								<tr>
									<th>Time Stamp</th>
									<th>CWS Host</th>
									<th>CWS Worker ID</th>
									<th>Log Level</th>
									<th>Thread Name</th>
									<th>Proc Def Key</th>
									<th>Proc Inst ID</th>
									<th>Message</th>
								</tr>
							</thead>
						</table>
					</div>
				</div>
			</div>
		</div>
		<div class="modal fade"id="ajax-error-modal"role="dialog"data-backdrop="static"data-keyboard="false">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title">AJAX ERROR!</h4>
					</div>
					<div class="modal-body">
						<p>There was an error sending an AJAX call to CWS.Please make sure that CWS is up and running.</p>
					</div>
					<div class="modal-footer">
						<button type="button"class="btn btn-primary"data-dismiss="modal">Dismiss</button>
					</div>
					</div><!--modal-content-->
					</div><!--modal-dialog-->
					</div><!--.modal.fade-->
				</body>
			</html>