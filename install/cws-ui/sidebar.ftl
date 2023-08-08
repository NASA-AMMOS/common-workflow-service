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
	<span class="glyphicon glyphicon-backward"></span>
</button>

<#-- <a id="toggleSidebar" href="#toggleSidebar1" class="glyphicon glyphicon-menu-hamburger"></a>
<a id="toggleSidebar" href="#sidebar" class="glyphicon glyphicon-forward"></a> -->

<script>
	function toggleSidebar() {
		const sidebar = document.querySelector('#sidebar0');
		if (sidebar.style.left === '0px') {
			sidebar.style.left = '-200px';
		} else {
			sidebar.style.left = '0';
		}
	}
</script>