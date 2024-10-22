<html>
<head>
	<meta charset="utf-8">
	<title>Login</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	
	<!-- Just for debugging purposes. Don't actually copy this line! -->
	<!--[if lt IE 9]><script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->
	<script>
		$(document).ready(function() {
			if ($("#message:contains('ERROR:')").length >= 1) {
				$("#message").css( "color", "red" );
			}
			else {
				$("#message").css( "color", "green" );
			}
		});
	</script>
</head>

<body>


<div class="navbar navbar-dark bg-dark fixed-top" role="navigation">
	<div class="container-fluid">
		<div class="navbar-header">
			<a class="navbar-brand" style="color: #9c9c9c;" href="#">__CWS_BRAND_HEADER__</a>
		</div>
		<div class="navbar-collapse collapse">
		</div>
	</div>
</div>

<div class="container">
		
		<div id="login-form" class="col-md-6 col-md-push-3 col-sm-12" style="margin-top: 200px">
			<div style="display: flex; justify-content: center; margin-bottom: 30px; flex-direction: column;">
				<div style="display: flex; justify-content: center;">
					<h2>__CWS_BRAND_HEADER__</h2>
				</div>
				<div style="display: flex; justify-content: center;">
					<h4 id="message">${msg}</h4>
				</div>
			</div>
			
			<form action="/${base}/logintotarget" method="POST">
				<label for="username">
				Username:
				</label>
				<input class="form-control" type="text" name="username" id="username" autofocus/><br/>
				
				<label for="password" >Password:</label> 
				<input type="password" class="form-control" name="password" id="password" /><br/>
				<input class="btn btn-primary pull-right" type="submit" id="submit" value="Submit" />
				<#if RequestParameters.target??>
					<input type="hidden" id="targetPage" name="targetPage" value="${RequestParameters.target}" />
				<#elseif cwsProjectWebappRoot??>
					<input type="hidden" id="targetPage" name="targetPage" value="/${cwsProjectWebappRoot}" />
				<#else>
					<input type="hidden" id="targetPage" name="targetPage" value="/${base}/home" />
				</#if>
			</form>
			<span class="text-muted"></span>
		</div>

</div>

<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="/${base}/js/bootstrap.min.js"></script>
<script src="/${base}/js/docs.min.js"></script>

</body>
</html>
