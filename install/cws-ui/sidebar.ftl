<script type="text/javascript">
$(document).ready(_ =>{
console.log("initialize tooltips!")
				const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))
})
</script>


<div class="sidebar show" id="sidebar">
	<ul class="nav nav-sidebar">

<!----		
<li <#if springMacroRequestContext.requestUri?contains("/deployments")> class="active"</#if>><a href="/${base}/deployments">Deployments</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/workers")> class="active"</#if>><a href="/${base}/workers">Workers</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/snippets")> class="active"</#if>><a href="/${base}/snippets">Snippets</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/initiators")> class="active"</#if>><a href="/${base}/initiators">Initiators</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/logs")> class="active"</#if>><a href="/${base}/logs?logLevel=ERROR">Logs</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/processes")> class="active"</#if>><a href="/${base}/processes?superProcInstId=null">Processes</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/configuration")> class="active"</#if>><a href="/${base}/configuration">Configuration</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/documentation")> class="active"</#if>><a href="/${base}/documentation">Documentation</a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/summary")> class="active"</#if>><a href="/${base}/summary">System Summary</a></li>
------>

		<li <#if springMacroRequestContext.requestUri?contains("/deployments")> class="active"</#if>><a href="/${base}/deployments" data-bs-toggle="tooltip" data-bs-title="Deployments"><img height="32" width="32" src="/${base}/images/locate.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/workers")> class="active"</#if>><a href="/${base}/workers" data-bs-toggle="tooltip" data-bs-title="Workers"><img height="32" width="32" src="/${base}/images/cpu.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/snippets")> class="active"</#if>><a href="/${base}/snippets" data-bs-toggle="tooltip" data-bs-title="Snippets"><img height="32" width="32" src="/${base}/images/snippet.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/initiators")> class="active"</#if>><a href="/${base}/initiators" data-bs-toggle="tooltip" data-bs-title="Initiators"><img height="32" width="32" src="/${base}/images/double_chevron_right.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/logs")> class="active"</#if>><a href="/${base}/logs?logLevel=ERROR" data-bs-toggle="tooltip" data-bs-title="Logs"><img height="32" width="32" src="/${base}/images/history.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/processes")> class="active"</#if>><a href="/${base}/processes?superProcInstId=null" data-bs-toggle="tooltip" data-bs-title="Processes"><img height="32" width="32" src="/${base}/images/checklist.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/configuration")> class="active"</#if>><a href="/${base}/configuration" data-bs-toggle="tooltip" data-bs-title="Configuration"><img height="32" width="32" src="/${base}/images/utility_wrench-dark.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/documentation")> class="active"</#if>><a href="/${base}/documentation" data-bs-toggle="tooltip" data-bs-title="Documentation"><img height="32" width="32" src="/${base}/images/book.svg" /></a></li>
		<li <#if springMacroRequestContext.requestUri?contains("/summary")> class="active"</#if>><a href="/${base}/summary" data-bs-toggle="tooltip" data-bs-title="System Summary"><img height="32" width="32" src="/${base}/images/document.svg" /></a></li>

	</ul>
</div>
