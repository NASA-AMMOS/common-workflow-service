<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Documentation</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
	<script>
	var params = {};
	var rows;

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
	});

	
	</script>
	
	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->

	<style type="text/css">
		#datepicker-div input{
			/*width:40%;*/
			margin-bottom: 1em;
			float:left;
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
	
			<h2 class="sub-header">Documentation</h2>
			
			<div id="config-div">
				<table id="configData" class="table table-striped table-bordered sortable">
					<tr>
						<th class="sort">Resource</th>
					</tr>
					<tr>
						<td><a href="https://github.com/NASA-AMMOS/common-workflow-service/wiki" target="_blank">CWS Wiki</a></td>
					</tr>
					<tr>
						<td><a href="https://docs.camunda.org/manual/7.17/reference/bpmn20/" target="_blank">Camunda BPMN 2.0 Implementation Reference</a></td>
					</tr>
					<tr>
						<td><a href="http://www.bpmnquickguide.com/quickguide/index.html" target="_blank">BP Incubator BPMN Quick Guide</a></td>
					</tr>
				</table>
			</div>
			
			<table>
		</div>
	</div>
</div>

<script src="/${base}/js/cws.js"></script>

</body>
</html>
