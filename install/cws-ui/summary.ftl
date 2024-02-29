<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Dashboard</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<script>
	
		function refreshElasticsearchStats() {
		
			var greenHtml = 'Good <span class="glyphicon glyphicon-ok" style="color: #51cf66;"></span>';
			var redHtml = '<span class="glyphicon glyphicon-remove" style="color: #ff6b6b;"></span> Red';
			var offlineHtml = '<span class="glyphicon glyphicon-remove" style="color: #ff6b6b;"></span> Offline';
			var timedOutHtml = '<span class="glyphicon glyphicon-remove" style="color: #ff6b6b;"></span> Timed out';
		
			$.ajax({ 
				url: "/${base}/rest/stats/es/cluster/health",
				success: function( data ) {
					
					if (data.timed_out) {
					
						$("#es_cluster_health").html(timedOutHtml);
					}
					else if (data.status === 'red') {
					
						$("#es_cluster_health").html(redHtml);
					}
					else {
						$("#es_cluster_health").html(greenHtml);
					}
				},
				error: function(){

					$("#es_cluster_health").html(offlineHtml);
				}
			});
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
			
			refreshElasticsearchStats();
		});
	
		$( window ).load(function() {
			console.log( "window loaded" );
		});
	</script>
	
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
		
		<div class="col-sm-9 col-md-10 main">
			<span id="statusMessageDiv"><h2>${msg}</h2></span>
			<h2 class="sub-header">Deployment Summary: <a href="/${base}/deployments">${numTotalProcDefs} deployed, ${numActiveProcDefs} active</a></h2>
			<br/>
			<br/>
			<h2 class="sub-header">System Summary</h2>
			<br/>

				<table class="table table-striped table-bordered sortable">
					<tr>
						<th>Elasticsearch Cluster Health</th>
						<td id="es_cluster_health"></td>
					</tr>
				</table>
			<br/>
			<br/>
			<br/>
			<br/>
			<br/>
			<br/>
			<b>New to CWS?</b>  Click <a href="https://github.com/NASA-AMMOS/common-workflow-service/wiki" target="_blank">here for the User's Guide</a>
		</div>
	</div>
</div>

<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="/${base}/js/bootstrap.min.js"></script>
<script src="/${base}/js/docs.min.js"></script>

</body>
</html>
