<html>
<head>
	<meta charset="utf-8">
	<title>CWS-Logs</title>

	<!--Load JS Libraries-->

	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/docs.min.js"></script><!--What is this?Are we using this currently?TODO:Investigate/remove-->
	<script src="/${base}/js/moment.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<script src="/${base}/js/DataTables/datatables.js"></script>
    <script src="/${base}/js/DataTablesDateFilter.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
    <script src="/${base}/js/DataTables/dataTables.responsive.min.js"></script>
    <script src="/${base}/js/DataTables/responsive.bootstrap.min.js"></script>

	<!--Load CSS Stylesheets-->
	<link href="/${base}/css/bootstrap.min.css"rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css"rel="stylesheet">
	<link href="/${base}/js/DataTables/datatables.css"rel="stylesheet">
	<link href="/${base}/css/dashboard.css"rel="stylesheet">
	<link href="/${base}/css/logs.css"rel="stylesheet">

	<!--JavaScript-->
	<script>

	//STATE PERSISTANCE CONSTS
	const username="username"; //temporary, hardcoded value for now
	const refreshRateVar="CWS_DASH_LOGS_REFRESH_RATE-"+username;
	const refreshVar="CWS_DASH_LOGS_REFRESH-"+username;
	const qstringVar="CWS_DASH_LOGS_QSTRING-"+username;

	// Global vars
	var params;
	var FETCH_COUNT=200;
	var htmlTableRows=[];
	var renderFlag=0;

	var baseEsReq={
		"from":0,
		"size":FETCH_COUNT,
		"query":{
			"bool":{
				"must":[]
			}
		},
		"sort":{
			"@timestamp":{
				"order":"desc"
			}
		}
	};

	//----------------------------
	function fetchInitialLogs(){
		renderFlag=0;
		htmlTableRows=[];
		var esReq=baseEsReq;

		if(params.procDefKey){
			esReq.query.bool.must.push({"match":{"procDefKey":params.procDefKey}});
		}
		if(params.logLevel){
			esReq.query.bool.must.push({"match":{"logLevel":params.logLevel}});
		}
		if(params.program){
			esReq.query.bool.must.push({"match":{"program":params.program}});
		}
		if(params.procInstId){
			//esReq.query.bool.must.push({"match":{"procInstId" : decodeURIComponent(params.procInstId)}});
			esReq.query.bool.must.push({"query_string":{"fields":["procInstId"],"query":"\""+decodeURIComponent(params.procInstId)+"\""}});
		}
		if(params.search){
			esReq.query.bool.must.push({"query_string":{"fields":["msgBody"],"query":decodeURIComponent(params.search)}});
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
		$("#pi-text").val(params.procInstId?decodeURIComponent(params.procInstId):"");
		$("#search-text").val(params.search?decodeURIComponent(params.search):"");
		$("#start-date").val(startDate);
		$("#end-date").val(endDate);

		getLogData(esReq);
	}

	$(document).ready(function(){

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

	$("#logData").DataTable();

	if(localStorage.getItem(refreshRateVar)===null){
		localStorage.setItem(refreshRateVar,"10000");
	}

	$("#refresh-rate").val((parseInt(localStorage.getItem(refreshRateVar))/1000).toString());


	if(localStorage.getItem(refreshVar)==="1"){
		$("#refresh-checkbox").prop("checked",true);
		refreshRate=parseInt(localStorage.getItem(refreshRateVar));
		refID=setInterval(refreshLogs,refreshRate);
	} else{
		$("#refresh-checkbox").prop("checked",false);
	}

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

	//
	// INITIAL LOG SET
	//
	$("#log-div .ajax-spinner").show();

	//GET query string values
	params=getQueryString();

	if(params==null){
		loadDefaultLog();
	}
	else{
		loadLogsToTable();
	}

	$("#log-div .ajax-spinner").hide();

	}); //END OF DOCUMENT.READY

	</script>
	
	<!--Just for debugging purposes.Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!--HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries-->
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
		<div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
	
			<span id="statusMessageDiv">
				<h2>${msg}</h2>
			</span>
			
			<h2 class="sub-header">Logs</h2>
			<div id="filters-div">
				<h4>Filters:</h4>
					<div class="col-md-3">
						<h5>Process Definitions</h5>
						<select id="pd-select">
							<option value="def">All Process Definitions</option>
							<#list procDefs as pd>
								<option value="${pd.key}">${pd.name}</option>
							</#list>
						</select>
					</div>
					<div class="col-md-2">
						<h5>Process Instances</h5>
						<input id="pi-text"type="text"class="form-control"placeholder="Instance ID...">
					</div>
					<div class="col-md-3">
						<h5>Log Level</h5>
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
					<div class="col-md-2">
						<h5>Search by Keyword</h5>
						<input id="search-text"id="filter-text"type="text"class="form-control"placeholder="Search..."/>
					</div>
					<div class="col-md-3">
						<span>Start Date:</span>
						<input id="start-date"class="form-control"placeholder="yyyy-mm-dd"
        data-date-format="yyyy-mm-dd"maxlength="10"type="text">
					</div>
					<div class="col-md-3">
						<span>End Date:</span>
						<input id="end-date"class="form-control"placeholder="yyyy-mm-dd"
        data-date-format="yyyy-mm-dd"size="16"type="text">
					</div>

					<div class="col-md-12">
						<a id="filter-submit-btn"class="btn btn-info pull-right"href="#">Filter</a>
					</div>
			</div>
			<div id="filters-btn"class="btn btn-warning"><span class="glyphicon glyphicon-filter">
				</span>&nbsp;Filters&nbsp;<span id="filter-arrow"class="glyphicon glyphicon-chevron-up"></span>
			</div>
			<div style="margin:30px 0px;"class="pull-right">
				<input type="checkbox"id="refresh-checkbox"/>&nbsp;
				<label for='refresh-checkbox'>Refresh the logs every</label>&nbsp;
				<select id="refresh-rate">
					<option value="10">10seconds</option>
					<option value="5">5seconds</option>
					<option value="3">3seconds</option>
					<option value="1">1second</option>
				</select>
			</div>
			<div id="resultCount"></div>
			
			<div id="log-div">
				<div class="ajax-spinner"></div>
				<table id="logData"class="table table-striped table-bordered sortable"style="width:100%;">
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


<script type="text/javascript"src="/${base}/js/cws.js"></script>
<script type="text/javascript">
	$("#filters-btn").click(function(){
		if($("#filters-div").is(":visible"))
			$("#filter-arrow").removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
		else
			$("#filter-arrow").removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");

		$("#filters-div").slideToggle();
	});

	function getFilterQString(){
		var params={};
		if($("#pd-select").val()!="def"){
			params.procDefKey=$("#pd-select").val();
		}
		if($("#pi-text").val()!=""){
			params.procInstId=encodeURIComponent($("#pi-text").val());
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

	$("#filter-submit-btn").click(function(e){
		e.preventDefault();
		window.location="/${base}/logs"+getFilterQString();
	});

	$("#filter-submit-btn").on("contextmenu",function(e){
		$(this).attr("href","/${base}/logs"+getFilterQString(false));
	});

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

	//
	// AUTO-REFRESH SETTING
	//
	var refID;
	$("#refresh-checkbox").click(function(){
		if($(this).prop("checked")){
			localStorage.setItem(refreshVar,"1");
			refreshRate=parseInt(localStorage.getItem(refreshRateVar));
			refreshLogs();
			refID=setInterval(refreshLogs,refreshRate);
		} else {
			localStorage.setItem(refreshVar,"0");
			clearInterval(refID);
		}
	});

	$("#refresh-rate").on('change',function(){
		refreshRate=parseInt($(this).val())*1000;
		localStorage.setItem(refreshRateVar,refreshRate.toString());
		if($("#refresh-checkbox").prop("checked")){
			clearInterval(refID);
			refID=setInterval(refreshLogs,refreshRate);
		}
	});
</script>

</body>
</html>
