<html>
<head>
	<meta charset="utf-8">
	<title>CWS - Modeler</title>
	<script src="/${base}/js/jquery.min.js"></script>
	<!-- Custom styles for this template -->

	<link rel="stylesheet" href="/${base}/css/diagram-js.css" />
  	<link rel="stylesheet" href="/${base}/css/bpmn-embedded.css" />
  	<link rel="stylesheet" href="/${base}/css/app.css" />
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
			getProcDef();
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


<span class="container-fluid">
	<div class="row">
	
		
		<div class="content" id="js-drop-zone">

    <div class="message intro">
      <div class="note">
        Drop BPMN diagram from your desktop or <a id="js-create-diagram" href>create a new diagram</a> to get started.
      </div>
    </div>

    <div class="message error">
      <div class="note">
        <p>Oops, we could not display the BPMN 2.0 diagram.</p>

        <div class="details">
          <span>cause of the problem</span>
          <pre></pre>
        </div>
      </div>
    </div>

    <div class="canvas" id="js-canvas"></div>
    <div class="properties-panel-parent" id="js-properties-panel"></div>
  </div>

  <ul class="buttons">
    <li>
      <a id="js-download-diagram" href title="download BPMN diagram">
        Save as BPMN diagram
      </a>
    </li>
    <li>
      <a id="js-download-svg" href title="download as SVG image">
        Save as SVG image
      </a>
    </li>
    <li>
      <a id="deploy-to-cws" href title="deploy to CWS" onclick="deployHandler()">
      	Deploy to CWS
      </a>
    </li>
  </ul>
  
  <div id="modal-window" class="bjs-powered-by-lightbox" style="display: none;z-index: 1001;position: fixed;top: 0;left: 0;right: 0;bottom: 0">
    <div id="modal-window-backdrop" class="backdrop" style="width: 100%;height: 100%;background: rgba(0,0,0,0.2)">
    </div>
    <div class="notice" id="modal-message" style="position: absolute;left: 25%;top: 40%;margin: 0 -130px;width: 50%;padding: 10px;background: white;border: solid 1px #AAA;border-radius: 3px;font-family: Helvetica, Arial, sans-serif;font-size: 14px;text-align: center"><div id="modal-title" style="line-height: 200%">Hello</div><div id="modal-body" style="line-height: 200%">Web-based tooling for BPMN, DMN and CMMN diagrams powered by <a href="http://bpmn.io" target="_blank">bpmn.io</a>.</div><button type="button" style="font-size: 16px" onclick="hideModal()">OK</button>
    </div>
  </div>

  <script src="/${base}/js/modeler.js"></script>
	</div>
</span>

 <script>
  	function deployHandler() {
 		bpmnModeler.saveXML({ format: true }, function (err, xml) {
 			$.ajax({
  				type: "POST",
  				url: "/${base}/rest/deployments/deployModelerFile",
  				data: { 
  					filename: $( ".layer-base" ).attr("data-element-id"), 
  					xmlData: xml 
  				}
  			})
 			.done(function( msg ) {
        $('#modal-title').html("<h1>Deployment Status: " + $( ".layer-base" ).attr("data-element-id") + ".bpmn</h1>");
 				$('#modal-body').html("<h2>"+msg+"</h2>");
 				if ($("#modal-body:contains('ERROR:')").length >= 1) {
				  $("#modal-body").css( "color", "red" );
				}
				else {
				  $("#modal-body").css( "color", "green" );
				}
 				$('#modal-window').show();
  		});
  	});
	}
	function hideModal() {
		$('#modal-window').hide();
	}
	function getProcDef() {
		var procDef = getParameterByName("procDefKey");
		if (procDef != null) {
			getXML(procDef);
		}
		
	}
	$("#get-id").click(getProcDef);
	// Utility function to get a query string parameter out of the URL
    //
    function getParameterByName(name) {
        var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
        return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
    }
    function getXML(proc_key){
		$.ajax({
			type: "GET",
			url: "/engine-rest/process-definition/key/" + proc_key + "/xml",
		})
		.done(function( msg ) {
			openDiagram(msg["bpmn20Xml"]);
		});
	}
  </script>


<script type="text/javascript">
	$("#bpmnio").width(document.innerWidth - $(".sidebar").width());
	function displayXML() {
		setInterval(myAlert, 30000);
	}
	function myAlert() {
		alert("Content");
	}
</script>
</body>
</html>
