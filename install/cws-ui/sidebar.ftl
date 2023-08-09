
<div class="col-sm-3 col-md-2 sidebar" id="sidebar0">
	<ul class="nav nav-sidebar" id="sidebar1">
		<li <#if springMacroRequestContext.requestUri?contains("/deployments")> class="active"</#if>><a href="/${base}/deployments">Deployments</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/workers")> class="active"</#if>><a href="/${base}/workers">Workers</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/snippets")> class="active"</#if>><a href="/${base}/snippets">Snippets</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/initiators")> class="active"</#if>><a href="/${base}/initiators">Initiators</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/logs")> class="active"</#if>><a href="/${base}/logs?logLevel=INFO,WARN,ERROR">Logs</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/processes")> class="active"</#if>><a href="/${base}/processes?superProcInstId=null">Processes</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/configuration")> class="active"</#if>><a href="/${base}/configuration">Configuration</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/documentation")> class="active"</#if>><a href="/${base}/documentation">Documentation</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/summary")> class="active"</#if>><a href="/${base}/summary">System Summary</a></li>
	</ul>
</div>

<button type="button" class="toggle-button" id="togglebutton0" onclick="toggleSidebar()">
	<span id="this012" class="glyphicon glyphicon-backward" onclick="toggleSidebar()"></span>
</button>
<#-- <a id="toggleSidebar" href="#toggleSidebar1" class="glyphicon glyphicon-menu-hamburger"></a>
<a id="toggleSidebar" href="#sidebar" class="glyphicon glyphicon-forward"></a> -->

<script>
	function toggleSidebar() {
		const sidebar = document.querySelector('#sidebar0');
		const toggleButton = document.querySelector('#togglebutton0');
		const icon = toggleButton.querySelector('span.glyphicon');

		if (sidebar.style.display === 'none') {
			sidebar.style.display = 'block';
			icon.className = 'glyphicon glyphicon-backward';
		} else {
			sidebar.style.display = 'none';
			icon.className = 'glyphicon glyphicon-forward';
		}
	}
	//If we want to be fancy with movement here is the code
	// window.addEventListener('load', () => {
	// 	const body = document.querySelector('body');
	// 	const sidebar = document.querySelector('#sidebar0');
	// 	const toggleButton = document.querySelector('#togglebutton0');
	// 	const icon = toggleButton.querySelector('span.glyphicon');
	//
	// 	if (sidebar.style.display === 'none') {
	// 		icon.className = 'glyphicon glyphicon-forward';
	// 		body.classList.add('sidebar-hidden');
	// 	} else {
	// 		icon.className = 'glyphicon glyphicon-backward';
	// 		body.classList.remove('sidebar-hidden');
	// 	}
	// });
	//
	//
	// function toggleSidebar() {
	// 	const body = document.querySelector('body');
	// 	const sidebar = document.querySelector('#sidebar0');
	// 	const toggleButton = document.querySelector('#togglebutton0');
	// 	const icon = toggleButton.querySelector('span.glyphicon');
	//
	// 	if (sidebar.style.display === 'none') {
	// 		sidebar.style.display = 'block';
	// 		icon.className = 'glyphicon glyphicon-backward';
	// 		body.classList.remove('sidebar-hidden');
	// 	} else {
	// 		sidebar.style.display = 'none';
	// 		icon.className = 'glyphicon glyphicon-forward';
	// 		body.classList.add('sidebar-hidden');
	// 	}
	// }

</script>