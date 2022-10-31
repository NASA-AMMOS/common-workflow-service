// override with js code file in your adaptation project for example like this
// cp your-cws-adaptation/src/main/resources/cws-ui/adaptation-deployments.js common-workflow-service/cws-ui/src/web/main/webapp/js/adaptation-deployments.js
function addAdaptationWorkersInfo(dataProcKey, listWorkers) {
	return listWorkers; // Common workflow behavior is to do nothing
}

