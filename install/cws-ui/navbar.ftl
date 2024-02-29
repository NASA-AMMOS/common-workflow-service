<nav class="navbar navbar-expand-sm navbar-dark bg-dark fixed-top justify-content-between" role="navigation">
	<div class="container-fluid">
		<div class="d-flex align-items-center">
			<button class="btn" type="button" data-bs-toggle="collapse" data-bs-target="#sidebar" style="margin-bottom: 3px;"><img height="16" width="16" src="/${base}/images/hamburger.svg"/></button>
			<a class="navbar-brand" href="/${base}/home">
				<span>__CWS_BRAND_HEADER__</span>
			</a>
		</div>
		<div class="collapse navbar-collapse" id="navbarText" style="justify-content: flex-end;">
			<ul class="navbar-nav ml-auto" style="gap: 20px;">
				<li class="nav-item"><a class="nav-link" href="/${base}/modeler" target="_blank"><img height="16" width="16" src="/${base}/images/edit.svg" />Modeler</a></li>
				<li class="nav-item"><a class="nav-link" href="/camunda/app/tasklist"><img height="16" width="16" src="/${base}/images/checklist.svg" />Tasklist</a></li>
				<li class="nav-item"><a class="nav-link" href="/camunda/app/cockpit"><img height="16" width="16" src="/${base}/images/helicopter.svg" />Cockpit</a></li>
				<li class="nav-item"><a class="nav-link" href="/camunda/app/admin/default/#/authorization?resource=0"><img height="16" width="16" src="/${base}/images/person.svg"/>Admin</a></li>
				__CWS_PROJECT_LINK__
				<li class="nav-item"><a class="nav-link" id="logoutLink" href="/${base}/logout"><img height="16" width="16" src="/${base}/images/upload.svg" style="transform: rotate(90deg);" />Logout</a></li>
			</ul>
		</div>
	</div>
</nav>