<table class="table table-striped" id="inits-table">
	<thead>
		<tr>
			<th>Initiator ID</th>
			<th>Type</th>
			<th>Process Def Key</th>
			<th>Status</th>
			<th>Action</th>
		</tr>
	</thead>
	<tbody>
		<#if initiators??>
			<#list initiators as x>
				<tr>
					<td class="initiator_id">${x.initiatorId}</td>
					<td>${x.type}</td>
					<td>${x.procDefKey}</td>
					<td id="status_${x.initiatorId}">${x.status}</td>
					<td>
						<!--<img id="disable_${x.initiatorId}_icon"  src="/${base}/images/pause.20.png" onClick="setEnabled('${x.initiatorId}', false);" style="display: none;" />
						<img id="enable_${x.initiatorId}_icon"  src="/${base}/images/play.20.png" onClick="setEnabled('${x.initiatorId}', true);" style="display: none;" />-->
						<div class="slide-switch">
							<input id="toggle_${x.initiatorId}" type="checkbox" onClick="setEnabled('${x.initiatorId}');" />
							<label for="toggle_${x.initiatorId}"><span>Power</span></label>
						</div>
					</td>
				</tr>
			</#list>
		<#else>
			<tr>
				<td colspan="5">no initiators</td>
			</tr>
		</#if>
	</tbody>
</table>
