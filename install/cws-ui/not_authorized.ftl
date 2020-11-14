<html>
<head>
	<meta charset="utf-8">
	<title>Not Authorized</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">
	<script>
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


<div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="/${base}/home">__CWS_BRAND_HEADER__</a>
        </div>
        <div class="navbar-collapse collapse">
          <ul class="nav navbar-nav navbar-right">
				<li><a href="/camunda/app/tasklist">Tasklist</a></li>
				<li><a href="/camunda/app/cockpit">Cockpit</a></li>
				<li><a href="/camunda/app/admin/default/#/authorization?resource=0">Admin</a></li>
            <li><a id="logoutLink" href="/${base}/logout">Logout</a></li>
          </ul>
        </div>
      </div>
    </div>

    <div class="container-fluid">
      <div class="row">
        <div class="col-sm-3 col-md-2 sidebar">
          <ul class="nav nav-sidebar">
            <li><a href="/${base}/home">Processes</a></li>
            <li><a href="/${base}/workers">Workers</a></li>
            <li><a href="/${base}/snippets">Snippets</a></li>
            <li><a href="/${base}/initiators">Initiators</a></li>
          </ul>
        </div>
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">
       
<span id="statusMessageDiv"><h2>${msg}</h2></span>
          <a href="/camunda/app/admin/default/#/authorization?resource=0">Click here to view authorization settings</a>
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
