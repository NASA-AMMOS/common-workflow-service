<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Executable Code</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
	<!-- Custom styles for this template -->
	<link href="/${base}/css/dashboard.css" rel="stylesheet">

	<style type="text/css">
	.error-log{
		margin: 20px 0px;
		border:2px solid #af5468;
		background:#111;
		padding: 20px;
		color:#dedede;
		font-size: 55%;
		font-weight: normal;
		font-family: courier, monospace, Consolas;
	}	
	#code{
		margin:2em 0;
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
			
			<h2 class="sub-header">Executable Code</h2>
			<div class="table-responsive">
				<form action="/${base}/rest/snippets/validateAndSaveSnippets" method="post">
					CWS provides a mechanism to execute custom code snippets that return a String from BPMN definitions.<br/>
					This is useful, for example when you want to dynamically inject values such as email addresses, command-line arguments, etc.. into various aspects of your existing BPMN tasks.<br/>
					<br/>
					In order to call a method you define, use the following syntax in your BPMN model:<br/>
					<pre>&#36;{cws.methodName([&#60;method_params&#62;...])}</pre><br/>
					<br/>
					For example, here's a command line execution task that uses dynamically generated arguments:
					<pre>/path/to/program.exe -arg1=&#36;{cws.getArg1('data', processVar1)} -arg2=&#36;{cws.getArg2()}</pre>
					<br/>
					<hr/>
					Edit the below code to add or modify methods that are available to your BPMN processes:
					<div id="editorDiv">code goes here</div>
					<input type="hidden" name="code" id="code" value="" />
					<br/>
					<input type="submit" class="btn btn-primary" id="validateAndSaveSnippetsSubmitBtn" value="Validate & Save" />
					<input type="button" id="revertSnippetsSubmitBtn" width="60" value="Reload Editor with Last Successfully Compiled Code" />
					<br/>
					<br/>
					<b>NOTE:</b> <b>For each external code library (JAR)</b> (referenced by Java import statements) <b>put the JAR in the following place</b>:
					<ul>
						<li>CWS Console Server: <pre>cws/server/apache-tomcat-9.0.33/lib</pre></li>
					</ul>
					<br/>
				</form>
			</div>
		</div>
	</div>
</div>


<script src="/${base}/js/bootstrap.min.js"></script>
<script src="/${base}/js/docs.min.js"></script>
<script src="/${base}/js/ace/ace.js" type="text/javascript" charset="utf-8"></script>
<script src="/${base}/js/jquery.min.js"></script>


	<script>
	var editor;

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
			
			//
			// LOAD LATEST IN PROGRESS CODE
			// AND ENABLE EDITOR
			//
			$.get( "/${base}/rest/snippets/getLatestInProgressCodeSnippet", function( data ) {
				console.log(data);
				$( "#editorDiv" ).text( data );
				editor = ace.edit("editorDiv");
				editor.setTheme("ace/theme/monokai");
				editor.getSession().setMode("ace/mode/java");
				editor.setOptions({
					maxLines: 250
				});
				editor.setBehavioursEnabled(false);
			});

			
			$( '#validateAndSaveSnippetsSubmitBtn' ).click(function() {
				$('#code').val(editor.getValue());
			});
			
			//
			// LOAD LATEST SUCCESSFULLY COMPILED CODE
			//
			$( '#revertSnippetsSubmitBtn' ).click(function() {
				$.get( "/${base}/rest/snippets/getLatestCodeSnippet", function( data ) {
					console.log(data);
					editor.env.document.setValue(data);
				});
			});
			
		});
	
	</script>

</body>
</html>
