// override with js code file in your adaptation project for example like this
// cp your-cws-adaptation/src/main/resources/cws-ui/adaptation-deployments.js common-workflow-service/cws-ui/src/web/main/webapp/js/adaptation-deployments.js
// This adaptation allows extra information to be added to each worker. The particular use case that
// drove this need is a request to add the aws scaling class each worker belongs to
function addAdaptationWorkersInfo(dataProcKey, listWorkers) {
	return; // Common workflow behavior is to do nothing
}

