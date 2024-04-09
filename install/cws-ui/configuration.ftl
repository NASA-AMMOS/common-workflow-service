<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Configuration</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<script src="/${base}/js/bootstrap.min.js"></script>
	<script src="/${base}/js/bootstrap-datepicker.min.js"></script>
	<script src="/${base}/js/popper.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
	<script>
	var params = {};
	var rows;
	var es = [];
	
	function formatBytes(bytes,decimals) {
	   if(bytes == 0) return '0 Bytes';
	   
	   var k = 1024,
	       dm = decimals <= 0 ? 0 : decimals || 2,
	       sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
	       i = Math.floor(Math.log(bytes) / Math.log(k));
	       
	   return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
	}

	function getEsIndices() {
	
	
		$.ajax({ 
			url: "/${base}/rest/stats/es/indices",
			success: function( data ) {
				
				for (var i = 0; i < data.length; i++) {
				
					var index = data[i];
					var health = index.health;
					
					if (health === 'yellow') {
						health += ' (OK)';
					}
					
					var size = formatBytes(parseInt(index["store.size"], 10));
					
					$("#es_indices").append('<tr><td>' + index.index + '</td><td>' + health + '</td><td>' + size + '</td></tr>');
				}
				
			//	$("#es_num_indices").html(data.length);
			},
			error: function(err){
				console.log("getes indices failed!", err);
			}
		});
		
	}
	
	function refreshElasticsearchStats() {
	
		$.ajax({ 
			url: "/${base}/rest/stats/es",
			success: function( data ) {
				
				for (var property in data.nodes) {
				  if (data.nodes.hasOwnProperty(property)) {
				  
				    var node = data.nodes[property];
				    
				    es.push( { 	host: node.host,
				    			size: node.indices.store.size_in_bytes,
				    			disk_free: node.fs.total.free_in_bytes,
				    			cpu_usage_percent: node.os.cpu.percent,
				    			memory_used_precent: node.os.mem.used_percent } );
				  }
				}
				
				$("#es_host").html(es[0].host);
				$("#es_disk_used").html(formatBytes(es[0].size));
				$("#es_disk_free").html(formatBytes(es[0].disk_free));
				$("#es_cpu").html(es[0].cpu_usage_percent + "%");
				$("#es_memory").html(es[0].memory_used_precent + "%");
				
			},
			error: function(){
			}
		});
	}
	
	function updateValues() {
					
		var dbSize = formatBytes(parseInt('${databaseSize}'.replace(/,/g, ''), 10));
		
		$("#db_size").html(dbSize);
		
		
		// Update table values
		var table = $("#diskUsage tbody");
		
        table.find('tr').each(function (i, el) {
        
	        var $tds = $(this).find('td'),
	        	node = $tds.eq(1),
	            sizeWithCommas = node.text();

            if (sizeWithCommas) {
            	var size = sizeWithCommas.replace(/,/g, '');

            	if (!isNaN(size)) {
	            	node.html(formatBytes(parseInt(size, 10)));
            	}
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
		
		updateValues();
		getEsIndices();
		refreshElasticsearchStats();
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
		
		.database {
			display: flex;
			align-items: center;
			margin: 30px 0px 50px;
		}
		
		.database > div {
			font-size: 23px;
		}
		
		#db_size {
			border-radius: 8px;
			margin-left: 25px;
			background: #ededed;
			padding: 10px;
		}
		
		.margin {
			margin-bottom: 20px;
		}
		
	</style>
</head>

<body>

<#include "navbar.ftl">

<div class="container-fluid">
	<div class="row">
		<#include "sidebar.ftl">
		<div class="main-content">
	
			<span id="statusMessageDiv">
				<h2>${msg}</h2>
			</span>
	
			<h2 class="sub-header">Configuration</h2>
			
			<div id="config-div">
				<table id="configData" class="table table-striped table-bordered sortable">
					<tr>
						<th class="sort">Name</th>
						<th class="sort">Value</th>
					</tr>
					<tr>
						<td>CWS Version</td>
						<td>${version}</td>
					</tr>
					<tr>
						<td>CWS Database Type</td>
						<td>${dbType}</td>
					</tr>
					<tr>
						<td>CWS Database Host</td>
						<td>${dbHost}</td>
					</tr>
					<tr>
						<td>CWS Database Name</td>
						<td>${dbName}</td>
					</tr>
					<tr>
						<td>CWS Database Port</td>
						<td>${dbPort}</td>
					</tr>
					<tr>
						<td>CWS Authentication Scheme</td>
						<td>${authScheme}</td>
					</tr>
					<tr>
						<td>CWS Install directory (console)</td>
						<td>${installDir}</td>
					</tr>
					<tr>
						<td>CWS Tomcat lib Directory (console)</td>
						<td>${tomcatLib}</td>
					</tr>
					<tr>
                        <td>CWS Tomcat bin Directory (console)</td>
                        <td>${tomcatBin}</td>
					</tr>
					<tr>
                        <td>CWS Tomcat home Directory (console)</td>
                        <td>${tomcatHome}</td>
					</tr>
					<tr>
                        <td>CWS Tomcat webapps Directory (console)</td>
                        <td>${tomcatWebapps}</td>
					</tr>
					<tr>
						<td>CWS History Level</td>
						<td>${historyLevel?capitalize}</td>
					</tr>
					<tr>
						<td>CWS History Days To Live (Logs & ES)</td>
						<td>${historyDaysToLive}</td>
					</tr>
					<tr>
						<td>Camunda Version</td>
						<td>${camundaVersion}</td>
					</tr>
					<tr>
						<td>Java Version</td>
						<td>${javaVersion}</td>
					</tr>					
					<tr>
						<td>Java Home Path</td>
						<td>${javaHome}</td>
					</tr>
				</table>
			</div>

			<br/>
			<div>
				<h3 class="sub-header">System Health</h3>
				
				<div class="database">
					<div>Database Disk Usage</div>
					<div id="db_size">${databaseSize}</div>
				</div>
				
				<h3 class="margin">System</h3>
				
					<table id="diskUsage" class="table table-striped table-bordered sortable">
					<tr>
						<th>Name</th>
						<th>Disk Available</th>
						<th>Logs</th>
					</tr>
					<#list workersInfo?sort_by("name") as w>
						<tr>
						<td>${w.name}</td>
						<td>
							<#if (w.diskFreeBytes)??>
								${w.diskFreeBytes}
							<#else>
								N/A
							</#if>
						</td>
						<td>
							<table class="table table-striped">
								<tr>
									<th>Name</th>
									<th>Size</th>
								</tr>
								<#list w.logs?sort_by("name") as l>
									<tr>
										<td>${l.name}</td>
										<td>${l.size}</td>
									</tr>
								</#list>
							</table>
						</td>
						</tr>
					</#list>
					</table>
				<br/>
				<div id="esdata">
					<h3 class="margin">Elasticsearch Server</h3>
					
					<table class="table table-striped table-bordered sortable">
						<tr>
							<!-- Placeholder will get overwritten in by refreshElasticsearchStats() --> 
							<th>Hostname</th>
							<td id="es_host">127.0.0.1</td>
						</tr>
						<tr>
							<th>Disk Used</th>
							<td id="es_disk_used"></td>
						</tr>
						<tr>
							<th>Disk Available</th>
							<td id="es_disk_free"></td>
						</tr>
						<tr>
							<th>CPU Used</th>
							<td id="es_cpu"></td>
						</tr>
						<tr>
							<th>Memory Used</th>
							<td id="es_memory"></td>
						</tr>
						<tr>
							<th>Indices</th>
							<td>
								<table id="es_indices" class="table table-striped">
									<tr>
										<th>Name</th>
										<th>Health</th>
										<th>Size</th>
									</tr>
								</table>
							</td>
						</tr>
					</table>
				</div>
			</div>
		</div>
	</div>
</div>

<script src="/${base}/js/cws.js"></script>

</body>
</html>
