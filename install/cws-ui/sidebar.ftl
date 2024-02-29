<div class="col-sm-3 col-md-2 sidebar collapse collapse-horizontal show" id="sidebar">
	<ul class="nav nav-sidebar">
		<li <#if springMacroRequestContext.requestUri?contains("/deployments")> class="active"</#if>><a href="/${base}/deployments">Deployments</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/workers")> class="active"</#if>><a href="/${base}/workers">Workers</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/snippets")> class="active"</#if>><a href="/${base}/snippets">Snippets</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/initiators")> class="active"</#if>><a href="/${base}/initiators">Initiators</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/logs")> class="active"</#if>><a href="/${base}/logs?logLevel=ERROR">Logs</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/processes")> class="active"</#if>><a href="/${base}/processes?superProcInstId=null">Processes</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/configuration")> class="active"</#if>><a href="/${base}/configuration">Configuration</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/documentation")> class="active"</#if>><a href="/${base}/documentation">Documentation</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/summary")> class="active"</#if>><a href="/${base}/summary">System Summary</a></li>
	</ul>
</div>