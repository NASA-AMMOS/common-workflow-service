<!DOCTYPE html>

<html>

<head>
    <meta charset="utf-8">
    <title>CWS - Processes</title>
    <script src="/${base}/js/jquery.min.js"></script>
    <script src="/${base}/js/popper.min.js"></script>
    <script src="/${base}/js/bootstrap.min.js"></script>
    <script src="/${base}/js/moment.js"></script>
    <script src="/${base}/js/DataTables/datatables.js"></script>
    <script src="/${base}/js/bootstrap-datepicker.min.js"></script>
    <script src="/${base}/js/DataTablesDateFilter.js"></script>
    <!-- Custom js adaptation script; override this file from your adaptation project -->
    <script src="/${base}/js/adaptation-process-actions.js"></script>
    <script src="/${base}/js/cws.js"></script>
    <!-- Load CSS Files-->
    <link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
    <link href="/${base}/css/dashboard.css" rel="stylesheet">
    <link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
    <link href="/${base}/css/microtip.css" rel="stylesheet">
    <link href="/${base}/css/processes.css" rel="stylesheet">
    <link href="/${base}/js/DataTables/datatables.css" rel="stylesheet">

    <!-- Just for debugging purposes. Don't actually copy this line! -->
    <!--[if lt IE 9]>
    <script src="../../assets/js/ie8-responsive-file-warning.js"></script><![endif]-->

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
		<script src="/${base}/js/html5shiv.js"></script>
		<script src="/${base}/js/respond.min.js"></script>
	<![endif]-->

</head>

<body>

    <#include "navbar.ftl">

        <div class="container-fluid">
            <div class="row">
                <#include "sidebar.ftl">
                    <div class="main-content">

                        <span id="statusMessageDiv">
                            <h2>${msg}</h2>
                        </span>

                        <h2 class="sub-header">Processes</h2>

                        <!-- Filters box (top of page) -->
                        <div id="filters-div">
                            <h4 style="margin-top: 10px;">Filters:</h4>
                            <p>Select filters before retrieving data to reduce loading time.</p>
                            <div style="display: flex; gap: 20px;">
                                <div>
                                    <h4>Process Definition:</h4>
                                    <select id="pd-select">
                                        <option value="def">Select PD</option>
                                        <#list procDefs as pd>
                                            <option value="${pd.key}">${pd.name}</option>
                                        </#list>
                                    </select>
                                </div>
                                <div style="width: 200px;">
                                    <h4>Subprocess & Superprocess:</h4>
                                    <input id="super-proc-inst-id-in" style="width: 90%" type="text"
                                        class="form-control" placeholder="Superprocess Instance ID" />
                                    <div style="margin-top: 10px" id="hide-subprocs-div">
                                    <label for="hide-subprocs">Hide Subprocesses</label>
                                    <input name="hide-subprocs" id="hide-subprocs-btn" type="checkbox">
                                </div>
                                </div>

                                <div>
                                    <h4>Status:</h4>
                                    <div id="status-select">
                                        <input id="fail" type="checkbox" value="fail" />
                                        <label for="fail">Failed</label><br />
                                        <input id="complete" type="checkbox" value="complete" />
                                        <label for="complete">Complete</label><br />
                                        <input id="resolved" type="checkbox" value="resolved" />
                                        <label for="resolved">Resolved</label><br />
                                        <input id="running" type="checkbox" value="running" />
                                        <label for="running">Running</label><br />
                                        <input id="pending" type="checkbox" value="pending" />
                                        <label for="pending">Pending</label><br />
                                        <input id="disabled" type="checkbox" value="disabled" />
                                        <label for="disabled">Disabled</label><br />
                                        <input id="failedToStart" type="checkbox" value="failedToStart" />
                                        <label for="failedToStart">Failed to Start</label><br />
                                        <input id="incident" type="checkbox" value="incident" />
                                        <label for="incident">Incident</label><br />
                                    </div>
                                </div>
                                <div id="datepicker-div">
                                    <h4>Created Date:</h4>
                                    <input id="min-date" class="form-control" data-date-format="yyyy-mm-dd" type="text"
                                        placeholder="From...">

                                    <input id="max-date" class="form-control" data-date-format="yyyy-mm-dd" type="text"
                                        placeholder="To...">
                                </div>
                                <div id="max-return-div">
                                    <h4>Max Results:</h4>
                                    <input id="max-return-num" class="form-control" type="number" min="1" value="5000">
                                    <input id="max-return-all" type="checkbox" value="-1" />
                                    <label style="margin-top: 5px;" for="max-return-all">Return all</label><br />
                                </div>
                            </div>
                            <br />
                            <div style="display: flex; gap: 10px; align-items: baseline;">
                                <input type="button" id="filter-submit-btn" class="btn btn-info btn-sm pull-right"
                                    value="Filter" />
                                <h6 class="pull-right" style="margin-right: 8px;">Matched Processes: <span
                                        id="numMatchProcesses"></span></h6>
                                <h6 class="pull-right" id="procCountWarning" style="color: red; margin-right: 8px;">
                                </h6>
                            </div>
                        </div>

                        <!-- Toggle visibility of filters button -->
                        <div id="filters-btn" class="btn btn-warning btn-sm"><img height="16" width="16" src="/${base}/images/filter.svg" />&nbsp;Filters&nbsp;<img height="16" width="16" src="/${base}/images/chevron_up.svg" />
                        </div>

                        <!-- Shows superprocess ID when displaying subprocesses, hidden otherwise -->
                        <div id="display-subprocs-div">
                            <h4>Displaying Subprocesses for Process Instance ID: <span
                                    id="super-proc-inst-id">34374-349083748</span></h4>
                        </div>
                        <div id="action_msg"></div>

                        <!-- Processes table -->
                        <div id="proc-log">
                            <div class="ajax-spinner" id="ajax-spinner"></div>
                            <table id="processes-table"
                                class="table table-striped table-responsive table-bordered sortable"
                                style="width: 100% !important; overflow: hidden;">
                                <thead>
                                    <tr>
                                        <th data-priority="1">
                                            <div class="btn-group">
                                                <button id="selectDropdownBtn" type="button"
                                                    class="btn btn-default dropdown-toggle" data-toggle="dropdown"
                                                    aria-haspopup="true" aria-label="Select Options"
                                                    style="background-color: transparent; border-color: transparent; box-shadow: none; display: flex; justify-content: center; width: 14px; border: 0px solid transparent; padding-left: 15px; padding-right: 15px">
                                                    <img height="16" width="16" src="/${base}/images/chevron_down.svg" />
                                                </button>
                                                <ul class="dropdown-menu">
                                                    <li><a id="selectOnPage" onclick="selectPage()">Select x processes
                                                            on this page</a></li>
                                                    <li><a id="selectAll" onclick="selectAll()">Select all x
                                                            processes</a></li>
                                                    <li role="separator" class="divider"></li>
                                                    <li><a id="invertOnPage" onclick="invertPage()">Invert selection on
                                                            this page</a></li>
                                                    <li><a id="invertAll" onclick="invertAll()">Invert selection</a>
                                                    </li>
                                                    <li role="separator" class="divider"></li>
                                                    <li><a id="deselectOnPage" onclick="deselectPage()">Deselect x
                                                            processes on this page</a></li>
                                                    <li><a id="deselectAll" onclick="deselectAll()">Deselect all x
                                                            processes</a></li>
                                                </ul>
                                            </div>
                                        </th>
                                        <th></th>
                                        <th>Definition Key</th>
                                        <th>Proc Inst ID</td>
                                        <th>Status</th>
                                        <th>Schedule Queued Time</th>
                                        <th>Started on Worker</th>
                                        <th>Process Start</th>
                                        <th>Process End</th>
                                        <th style="word-wrap: break-word;">Input Variables</th>
                                        <th>Superprocess ID</th>
                                        <th>UUID</th>
                                        <th style="word-wrap: break-word;">Output Variables</ </thead>
                                <tbody>
                                </tbody>
                            </table>
                        </div>
                    </div>
            </div>
        </div>

        <script type="text/javascript">

            //STATE PERSISTANCE CONSTS
            var username = document.cookie.substring(document.cookie.indexOf("cwsUsername=") + 12);
            if (username.indexOf(";") > 0) {
                username = username.substring(0, username.indexOf(";"));
            }
            const hideSubProcsVar = "CWS_DASH_PROCS_HIDE_SUBPROCS-" + username;
            const qStringVar = "CWS_DASH_PROCS_QSTRING-" + username;
            //GLOBAL VARS
            var params = {};

            //DOCUMENT.READY START
            $(document).ready(function () {
                //try to load hideSubProcsVar from local storage. If it doesn't exist, set it to true
                //(automatically hides subprocs if never visited this page before)
                if (localStorage.getItem(hideSubProcsVar) === null) {
                    localStorage.setItem(hideSubProcsVar, true);
                }

                //try to load qStringVar from local storage. If it doesn't exist, ignore
                if (localStorage.getItem(qStringVar) !== null) {
                    //if we have a qStringVar, we want to apply it and then update location
                    var qString = localStorage.getItem(qStringVar);
                    var qStringObj = getQueryString();
                    if (qStringObj["cache"] == null || qStringObj["cache"] == undefined) {
                        //we have no cache param - load from cache
                        if(!(isEqual(parseQueryString(qString), getQueryString()))){
                            applyParamsToFilters(getQueryString());
                            updateLocation(false, 0);
                        }
                    }
                }

                //initialize our datepicker elements
                $("#min-date").datepicker({
                    orientation: 'left top',
                    todayBtn: 'true',
                    todayHighlight: true
                });

                $("#max-date").datepicker({
                    orientation: 'left top',
                    todayBtn: 'true',
                    todayHighlight: true
                });

                
                //Toggle direction of chevron on filter visiblity toggle button
                $("#filters-btn").on("click", function () {
                    if ($("#filters-div").is(":visible"))
                        $("#filter-arrow").attr("src", "/${base}/images/chevron_down.svg");
                    else
                        $("#filter-arrow").attr("src","/${base}/images/chevron_up.svg");
                    $("#filters-div").slideToggle();
                });

                //Catch click on filter submit button
                $("#filter-submit-btn").on("click", function () {
                    updateLocation(false, 1);
                });

                //Hide div that displays superprocess ID by default
                $("#display-subprocs-div").css('display', 'none');

                //get our params from the url
                params = getQueryString();

                //get our current url
                var currentUrl = window.location.href;

                //apply our params to the filter section of the page
                applyParamsToFilters(params);
                getNumMatchingProcesses();

                //when we edit a filter in the filters table, get the new number of matching processes
                $("#pd-select").change(function () {
                    getNumMatchingProcesses();
                });
                $("#status-select").change(function () {
                    getNumMatchingProcesses();
                });
                $("#min-date").change(function () {
                    getNumMatchingProcesses();
                });
                $("#max-date").change(function () {
                    getNumMatchingProcesses();
                });
                $("#super-proc-inst-id-in").change(function () {
                    getNumMatchingProcesses();
                });
                $("#filter-submit-btn").on("click", function () {
                    updateLocation(false, 1);
                });
                $("#hide-subprocs-btn").on("click", function () {
                    updateLocation(true, 1);
                    if (!($("#hide-subprocs-btn")).is(":checked")) {
                        $("#super-proc-inst-id-in").hide();
                    } else {
                        $("#super-proc-inst-id-in").show();
                    }
                });
                $("#max-return-num").change(function () {
                    getNumMatchingProcesses();
                });
                $("#max-return-all").change(function () {
                    getNumMatchingProcesses();
                    if ($("#max-return-all").is(":checked")) {
                        $("#max-return-num").prop('disabled', true);
                    } else {
                        $("#max-return-num").prop('disabled', false);
                    }
                });

                //display message if we received one from the server
                displayMessage();

                //initialize our moment format (used for dates in table)
                $.fn.dataTable.moment('MMM D, YYYY, h:mm:ss A');

                //initialize our DataTable
                $("#processes-table").DataTable({
                    "autoWidth": false,
                    //allows for editing what buttons / text says on/in the table
                    language: {
                        searchBuilder: {
                            add: "Add Local Filter",
                        }
                    },
                    //delays rendering the data in the dom until it has to (ex: don't render page 10 until we browse to page 10)
                    //improves loading time
                    deferRender: true,
                    //define what we do with the data we receive from our API call
                    //render: function() passes in data (the data we receive from the API call) and type (why DataTables is requesting it (like for search, display, filter, etc.))
                    columns: [
                        {
                            data: null,
                            defaultContent: '',
                            className: 'select-checkbox',
                            orderable: false
                        },
                        {
                            data: "procInstId",
                            defaultContent: '',
                            className: 'details-control',
                            orderable: false,
                            render: function (data, type) {
                                if (type === 'display') {
                                    if (data == null) {
                                        return '<a onclick="viewHistory(\'' + data + '\')" href="/${base}/history?procInstId=' + data + '" ><button style=\"margin-bottom: 5px;\" class="btn btn-outline-dark btn-sm disabled">History</button></a>' +
                                            "<a style=\"margin-bottom: 5px;\" onclick=\"viewSubProcs('" + data + "')\"><button class=\"btn btn-outline-dark btn-sm disabled\" style=\"margin-bottom: 5px\">Subprocs</button></a>";
                                    }
                                    return '<a onclick="viewHistory(\'' + data + '\')" href="/${base}/history?procInstId=' + data + '" ><button style=\"margin-bottom: 5px;\" class="btn btn-outline-dark btn-sm">History</button></a>' +
                                        "<a style=\"margin-bottom: 5px;\" onclick=\"viewSubProcs('" + data + "')\"><button class=\"btn btn-outline-dark btn-sm\">Subprocs</button></a>";
                                }
                                return data;
                            }
                        },
                        {
                            data: "procDefKey",
                            render: function (data, type) {
                                if (type !== "display") {
                                    return data;
                                } else {
                                    if (data === null || data === undefined || data === "") {
                                        return "";
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                }
                            }
                        },
                        {
                            data: { procInstId: "procInstId", status: "status" },
                            render: function (data, type) {
                                if (type === 'display') {
                                    if (data.procInstId === null) {
                                        return "";
                                    }
                                    if (data.status === "incident") {
                                        var incidentUrl = "/camunda/app/cockpit/default/#/process-instance/" + data.procInstId + "/runtime?tab=incidents-tab";
                                        return "<div class='table-cell-flex'><a href=\"" + incidentUrl + "\" target=\"blank_\">" + data.procInstId + "</a>"
                                            + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                            + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data.procInstId + "\" onClick=''>"
                                            + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                            + "</span></div>";
                                    } else {
                                        return '<div class="table-cell-flex"><p>' + data.procInstId + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data.procInstId + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                    }
                                } else {
                                    return data.procInstId;
                                }
                            }
                        },
                        { 
                            data: "status",
                            render: function(data, type) {
                                var status;
                                if (type === 'display') {
                                    switch(data) {
                                        case "fail" :
                                            status = "<p class=\"tr-fail\">" + data + "</p>";
                                            break;
                                        case "incident" :
                                            status = "<p class=\"tr-incident\">" + data + "</p>";
                                            break;
                                        case "complete": 
                                            status = "<p class=\"tr-complete\">" + data + "</p>";
                                            break;
                                        case "resolved": 
                                            status = "<p class=\"tr-complete\">" + data + "</p>";
                                            break;
                                        case "running":
                                            status = "<p class=\"tr-running\">" + data + "</p>";
                                            break;
                                        case "pending":
                                            status = "<p class=\"tr-pending\">" + data + "</p>";
                                            break;
                                        case "disabled":
                                            status = "<p class=\"tr-failed\">" + data + "</p>";
                                            break;
                                        case "failedToStart":
                                            status =  "<p class=\"tr-failed\">" + data + "</p>";
                                            break;
                                        default:
                                            status =  data;
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                } else {
                                    return data;
                                }
                            }
                        },
                        { 
                            data: "createdTimestamp",
                            render: function (data, type) {
                                if (type !== "display") {
                                    return data;
                                } else {
                                    if (data === null || data === undefined || data === "") {
                                        return "";
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                }
                            }
                        },
                        {
                            data: "startedByWorker",
                            render: function (data, type) {
                                if (type === 'display') {
                                    if (data !== null) {
                                        return '<div class="table-cell-main-flex">'
                                            + '<div class="table-cell-flex"><p>'
                                            + data 
                                            + '</p>' 
                                            + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                            + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                            + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                            + "</span></div></div>"
                                            + '<div class="table-cell-flex"><div style="margin-top: 0px;">'
                                            + "<b>Worker IP: </b>" + data.split("_").slice(0, -2).join(".") + '</div>'
                                            + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                            + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data.split("_").slice(0, -2).join(".") + "\" onClick=''>"
                                            + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                            + "</span></div></div>";
                                    }
                                }
                                return data;
                            }
                        },
                        { 
                            data: "procStartTime",
                            render: function (data, type) {
                                if (type !== "display") {
                                    if (data === null) {
                                        return "";
                                    }
                                    return data;
                                } else {
                                    if (data === null || data === undefined || data === "") {
                                        return "";
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                }
                            }
                        },
                        {
                            data: { procEndTime: "procEndTime", procStartTime: "procStartTime" },
                            render: function (data, type) {
                                if (type === 'display') {
                                    if (data.procEndTime == null) {
                                        return "";
                                    }
                                    if (data.procStartTime !== '' && data.procEndTime !== '') {
                                        var start = moment(data.procStartTime);
                                        var end = moment(data.procEndTime);
                                        var procDuration = "<br><i>(~" + moment.duration(end.diff(start)).humanize() + ")</i>";
                                    } else {
                                        var procDuration = '';
                                    }
                                    return '<div class="table-cell-flex"><p>' 
                                        + data.procEndTime + procDuration
                                        + '</p>' 
                                        + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                        + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data.procEndTime + "\" onClick=''>"
                                        + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                        + "</span></div>";
                                } else {
                                    return data.procEndTime;
                                }
                            }
                        },
                        {
                            data: { inputVariables: "inputVariables", procStartTime: "procStartTime"},
                            render: function (data, type) {
                                if (jQuery.isEmptyObject(data.inputVariables)) {
                                    return "None";
                                }
                                if (type === 'display') {
                                    var output = "";
                                    var before = "";
                                    var after = "";
                                    var putAllAfter = 0;
                                    var count = 0;
                                    var timeStart = moment(data.procStartTime);
                                    for (const [key, value] of Object.entries(data.inputVariables)) {
                                        var temp = "";
                                        var varTimeSet = key.substring(key.indexOf("[")+1, key.indexOf("]"));
                                        if (moment(varTimeSet).diff(timeStart, "seconds") > 1) {
                                            continue;
                                        }
                                        var tempVal = value;
                                        var tempKey = key.substring(key.indexOf("]") + 1);
                                        if (tempKey === "workerId") {
                                            continue;
                                        }
                                        if (count > 2) {
                                            putAllAfter = 1;
                                        }
                                        if (key.includes("(file, image")) {
                                            tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                            temp = `<div class="var-row-div-flex">`
                                                + `<div class="var-row-div-flex-sub-1">`
                                                + `<b>` + tempKey + `: </b>`
                                                + `<img class="grow" src="` + tempVal + `"></div>`
                                                + `<div class="var-row-div-flex-sub-2"></div>`
                                                + `<div class="copySpan" style="width: 30px;">`
                                                + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick="">`
                                                + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                + `</span></div></div>`;
                                        } else if (key.includes("{")) {
                                            var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
                                            tempKey = tempKey.substring(0, tempKey.indexOf(" {"));
                                            temp = `<div class="var-row-div-flex">`
                                                + `<div class="var-row-div-flex-sub-1">`
                                                + `<b>` + tempKey + `: </b>`
                                                + `<i>` + fileName + `</i></div>`
                                                + `<div class="var-row-div-flex-sub-2"></div>`
                                                + `<div class="copySpan" style="width: 30px;">`
                                                + `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick="">`
                                                + `<img src="images/download.svg" class="copy-icon clipboard">`
                                                + `</span></div></div>`;
                                        } else if (checkforImageURL(tempVal)) {
                                            tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                            temp = `<div class="var-row-div-flex">`
                                                + `<div class="var-row-div-flex-sub-1">`
                                                + `<b>` + tempKey + `: </b>`
                                                + `<img class="grow" src="` + tempVal + `"></div>`
                                                + `<div class="var-row-div-flex-sub-2"></div>`
                                                + `<div class="copySpan" style="width: 30px;">`
                                                + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                + `</span></div></div>`;
                                        } else if (checkForURL(tempVal)) {
                                            tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                            temp = `<div class="var-row-div-flex">`
                                                + `<div class="var-row-div-flex-sub-1">`
                                                + `<b>` + tempKey + `: </b><a href="` + tempVal + `">` + tempVal + `</a></div>`
                                                + `<div class="var-row-div-flex-sub-2"></div>`
                                                + `<div class="copySpan" style="width: 30px;">`
                                                + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                + `</span></div></div>`;
                                        } else {
                                            if (tempKey.toUpperCase().includes("(STRING)")) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                            }
                                            temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b><p style="margin-bottom: 0px;">` + tempVal + `</p></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                        }
                                        if (putAllAfter === 0) {
                                            before = before + temp;
                                        } else {
                                            after = after + temp;
                                        }
                                        count++;
                                    }
                                    if (after.length == 0) {
                                        output = before;
                                    } else {
                                        output = before + "<details><summary><b> Show All</b></summary>" + after + "</details>";
                                    }
                                    return output;
                                } else {
                                    var outputToString = "";
                                    for (const [key, value] of Object.entries(data)) {
                                        if (key === "workerId") {
                                            continue;
                                        }
                                        outputToString += outputToString + key + ": " + value + ",";
                                    }
                                    return outputToString;
                                }
                            }
                        },
                        { 
                            data: "superProcInstId",
                            render: function (data, type) {
                                if (type !== "display") {
                                    return data;
                                } else {
                                    if (data === null || data === undefined || data === "") {
                                        return "";
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                }
                            }
                        },
                        {
                            data: "uuid",
                            render: function (data, type) {
                                if (type !== "display") {
                                    return data;
                                } else {
                                    if (data === null || data === undefined || data === "") {
                                        return "";
                                    }
                                    return '<div class="table-cell-flex"><p>' + data + '</p>' 
                                                + "<div class=\"copySpan\" style=\"width: 20px;\">"
                                                + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + data + "\" onClick=''>"
                                                + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                                + "</span></div>";
                                }
                            }
                        },
                        {
                            data: "outputVariables",
                            render: function (data, type) {
                                if (jQuery.isEmptyObject(data)) {
                                    return "None";
                                }
                                if (type === 'display') {
                                    var output = "";
                                    var before = "";
                                    var after = "";
                                    var count = 0;
                                    if (Object.keys(data).includes("output_display_order (object)")) {
                                        //we have an order array
                                        var orderTruncated = data["output_display_order (object)"].substring(1, data["output_display_order (object)"].length - 1).split(", ");
                                        var fullKeys = Object.keys(data);
                                        var fullKeysInOrder = [];
                                        for (var i = 0; i < Object.keys(orderTruncated).length; i++) {
                                            var result =fullKeys.findIndex(element => element.includes(orderTruncated[i]));
                                            if (result > -1) {
                                                fullKeysInOrder.push(fullKeys[result]);
                                            }
                                        }
                                        //the orderTruncated array now contains the full keys in the order they should be displayed
                                        for (key in fullKeysInOrder) {
                                            var temp = "";
                                            var tempVal = data[fullKeysInOrder[key]];
                                            var tempKey = fullKeysInOrder[key].substring(7);
                                            if (tempKey.includes("(file, image")) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<img class="grow" src="` + tempVal + `"></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (tempKey.includes("{")) {
                                                var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
                                                tempKey = tempKey.substring(tempKey.indexOf("[")+1, tempKey.indexOf(" {"));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<i>` + fileName + `</i></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick="">`
                                                    + `<img src="images/download.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (checkforImageURL(tempVal)) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<img class="grow" src="` + tempVal + `"></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (checkForURL(tempVal)) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b><a href="` + tempVal + `">` + tempVal + `</a></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else {
                                                if (tempKey.toUpperCase().includes("(STRING)")) {
                                                    tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                }
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b><p style="margin-bottom: 0px;">` + tempVal + `</p></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            }
                                            if (count > 2) {
                                                after += temp;
                                            } else {
                                                before += temp;
                                            }
                                            count++;
                                        }
                                        if (after !== "") {
                                            output = before + '<details><summary style="margin-top: 5px;"><b> Show All</b></summary>' + after + "</details>";
                                        } else {
                                            output = before;
                                        }
                                    } else {
                                        for (const [key, value] of Object.entries(data)) {
                                            var temp = "";
                                            var tempVal = value;
                                            var tempKey = key.substring(7);
                                            if (tempKey === "workerId") {
                                                continue;
                                            }
                                            if (tempKey.includes("(file, image")) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<img class="grow" src="` + tempVal + `"></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="true" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (tempKey.includes("{")) {
                                                var fileName = tempKey.substring(tempKey.indexOf("{") + 1, tempKey.indexOf("}"));
                                                tempKey = tempKey.substring(tempKey.indexOf("]")+1, tempKey.indexOf(" {"));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<i>` + fileName + `</i></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Download" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-downloadValue="` + tempVal + `" data-downloadName="` + fileName + `" onClick="">`
                                                    + `<img src="images/download.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (checkforImageURL(tempVal)) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b>`
                                                    + `<img class="grow" src="` + tempVal + `"></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else if (checkForURL(tempVal)) {
                                                tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b><a href="` + tempVal + `">` + tempVal + `</a></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            } else {
                                                if (tempKey.toUpperCase().includes("(STRING)")) {
                                                    tempKey = tempKey.substring(0, tempKey.indexOf(" ("));
                                                }
                                                temp = `<div class="var-row-div-flex">`
                                                    + `<div class="var-row-div-flex-sub-1">`
                                                    + `<b>` + tempKey + `: </b><p style="margin-bottom: 0px;">` + tempVal + `</p></div>`
                                                    + `<div class="var-row-div-flex-sub-2"></div>`
                                                    + `<div class="copySpan" style="width: 30px;">`
                                                    + `<span aria-label="Copy to clipboard" data-microtip-position="top-left" role="tooltip" class="copy" data-isImage="false" data-copyValue="` + tempVal + `" onClick="">`
                                                    + `<img src="images/copy.svg" class="copy-icon clipboard">`
                                                    + `</span></div></div>`;
                                            }
                                            if (tempKey === "startedOnWorkerId") {
                                                after = after + temp;
                                                count += 2;
                                            } else if (count <= 2) {
                                                before = before + temp;
                                            } else {
                                                after = after + temp;
                                            }
                                            count++;
                                        }
                                    }
                                    if (after.length == 0) {
                                        output = before;
                                    } else {
                                        output = before + '<details><summary style="margin-top: 5px;"><b> Show All</b></summary>' + after + "</details>";
                                    }
                                    return output;
                                } else {
                                    var outputToString = "";
                                    for (const [key, value] of Object.entries(data)) {
                                        if (key === "workerId") {
                                            continue;
                                        }
                                        outputToString += outputToString + key + ": " + value + ",";
                                    }
                                    return outputToString;
                                }
                            }
                        },
                    ],
                    //wait 250ms after the user's last keypress in the search box to start search (better performance)
                    searchDelay: 250,
                    select: {
                        style: 'multi+shift',
                        selector: 'td:first-child'
                    },
                    order: [[2, 'asc']],
                    //set configuration of the columns (width, visibility order, etc.)
                    columnDefs: [
                        {
                            orderable: false,
                            className: 'select-checkbox noVis',
                            targets: 0
                        },
                        {
                            orderable: false,
                            className: 'noVis',
                            searchable: false,
                            targets: 1
                        },
                        {
                            targets: [5, 6, 10, 11],
                            visible: false
                        },
                        {
                            targets: [1],
                            "width": "75px"
                        },
                        {
                            targets: [0],
                            "width": "30px"
                        },
                        {
                            targets: [9, 12],
                            width: "200px"
                        },
                        {
                            targets: [1, 2],
                            responsivePriority: 1
                        },
                        {
                            targets: [3, 9, 12],
                            responsivePriority: 2
                        },
                        {
                            targets: [4],
                            responsivePriority: 3
                        },
                        {
                            targets: [8],
                            responsivePriority: 4
                        },
                        {
                            targets: [5, 7],
                            responsivePriority: 5
                        },
                        {
                            targets: [6],
                            responsivePriority: 6
                        },
                        {
                            targets: [10],
                            responsivePriority: 7
                        },
                        {
                            targets: [11],
                            responsivePriority: 8
                        },
                    ],
                    //tells datatables to put specific elements in divs it creates (quotes are classes)
                    dom: "Q<'above-table-div'<'above-table-buttons'B><'above-table-length'l><'above-table-filler'><'above-table-filter'f>>"
                        + "t"
                        + "<'below-table-div'ip>",
                    //tells datatables we want to use one of the predefined buttons
                    buttons: [
                        {
                            extend: 'colvis',
                            columns: ':not(.noVis)',
                            className: 'btn btn-primary',
                            text: '<img height="16" width="16" src="/${base}/images/visible_show_dark.svg" style="margin-right: 5px; margin-bottom: 3px;" />Columns',
                        }
                    ],
                    //enables the complex search builder, tells it what columns should be able to be searched
                    searchBuilder: {
                        columns: [2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
                    },
                    //enables responsive mode (hides columns when screen is too small), tells it not to display content that is hidden in a subrow (conflicts with checkmark)
                    responsive: {
                        details: false
                    }
                });

                //now that the datatable is initialized, we can start using its API calls
                var table = $("#processes-table").DataTable();

                //when we select a row, we want to update the action list
                table.on('select', function (e, dt, type, indexes) {
                    updateActionList();
                });

                //when we deselect a row, we want to update the action list
                table.on('deselect', function (e, dt, type, indexes) {
                    updateActionList();
                });

                //when we click the copy button next to an input/output variable, we want to copy the value to the clipboard
                $(document).on('click', '.copy', function (e) {
                    e.preventDefault();
                    if ($(this).attr("data-downloadValue") !== undefined && $(this).attr("data-downloadValue") !== false && $(this).attr("data-downloadValue") !== null) {
                        var downloadValue = $(this).attr('data-downloadValue');
                        var downloadName = $(this).attr('data-downloadName');
                        downloadFile(downloadValue, downloadName);
                        return;
                    }
                    var copyValue = $(this).attr('data-copyValue');
                    var isImage = $(this).attr('data-isImage');
                    console.log(isImage);
                    copyInput(copyValue, isImage);
                    $(this).attr('aria-label', 'Copied!');
                    setTimeout(function () {
                        $('.copy').attr('aria-label', 'Copy');
                    }, 2000);
                    console.log("fire");
                });

                //add our action dropdown button to the div that datatables created (created in dom: above)
                $('<div class="btn-group" style="margin-bottom: 5px"><div style="display: flex; align-items: center; gap: 5px;"><input id="select-all-btn" type="checkbox">Select All</select></div></div><div class="btn-group" style="margin-bottom: 5px"><button id="menu3" class="btn btn-primary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false"><img height="16" width="16" src="/${base}/images/waterfall_light.svg" />&nbsp;Actions &nbsp;'
                    + '<span class="caret"></span>'
                    + '</button>'
                    + '<ul id="action-list" class="dropdown-menu" role="menu" aria-labelledby="menu3">'
                    + `<li id="action_open_selected_new_tabs" class="disabled" role="presentation"><a class="dropdown-item" id="action_open_selected_new_tabs_atag" role="menuitem">Open selected rows in new tabs (must not be pending)<span style="margin-left: 10px;" class="label label-info">Requires Pop-ups to be enabled</span></a></li>`
                    + `<li id="action_copy_all_selected_history_links" class="disabled" role="presentation"><a class="dropdown-item" id="action_copy_all_selected_history_links_atag" role="menuitem">Copy all selected history links (must not be pending)</a></li>`
                    + '<li role="separator" class="divider"></li>'
                    + `<li id="action_delete_selected" class="disabled" role="presentation"><a class="dropdown-item" id="action_delete_selected_atag" role="menuitem">Stop running selected rows (all rows selected must be 'running')</a></li>`
                    + `<li id="action_disable" class="disabled" role="presentation"><a class="dropdown-item" id="action_disable_atag" role="menuitem">Disable selected rows (all rows selected must be 'pending')</a></li>`
                    + `<li id="action_enable" class="disabled" role="presentation"><a class="dropdown-item" id="action_enable_atag" role="menuitem">Enable selected rows (all rows selected must be 'disabled')</a></li>`
                    + `<li id="action_retry_incident" class="disabled" role="presentation"><a class="dropdown-item" id="action_retry_incident_atag" role="menuitem">Retry all selected incident rows (all rows selected must be 'incident')</a></li>`
                    + `<li id="action_retry_failed_to_start" class="disabled" role="presentation"><a class="dropdown-item" id="action_retry_failed_to_start_atag" role="menuitem">Retry all selected failed to start rows (all rows selected must be 'failedToStart')</a></li>`
                    + `<li id="action_mark_as_resolved" class="disabled" role="presentation"><a class="dropdown-item" id="action_mark_as_resolved_atag" role="menuitem">Mark all selected failed rows as resolved (all rows selected must be 'fail')</a></li>`
                    + `<#include "adaptation-process-actions.ftl">`
                    + `</ul></div>`).appendTo(".above-table-buttons");

                //add our download dropdown button to the div that datatables created (created in dom: above)
                $('<div class="btn-group" style="margin-bottom: 5px"><button id="action-download-group" class="btn btn-primary btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown"><img height="16" width="16" src="/${base}/images/save_light.svg" />&nbsp;Download &nbsp;'
                    + '<span class="caret"></span>'
                    + '</button>'
                    + '<ul id="action-list" class="dropdown-menu" role="menu" aria-labelledby="action-download-group">'
                    + `<li id="action_download_selected_list" class="disabled" role="presentation"><a class="dropdown-item" id="action_download_selected_list_atag" role="menuitem">Download list of selected processes (must select at least one row)</a></li>`
                    + `<li id="action_download_selected_json" class="disabled" role="presentation"><a class="dropdown-item" id="action_download_selected_json_atag" role="menuitem">Download logs of selected processes (JSON) (all rows selected must not be pending)</a></li>`
                    + `<li id="action_download_selected_csv" class="disabled" role="presentation"><a class="dropdown-item" id="action_download_selected_csv_atag" role="menuitem">Download logs of selected processes (CSV) (all rows selected must not be pending)</a></li>`
                    + `<#include "adaptation-process-actions.ftl">`
                    + `</ul></div>`).appendTo(".above-table-buttons");

                //datatable is now setup - fetch our data on initial page load
                fetchAndDisplayProcesses();

             //when the select-all checkbox is clicked, select/deselect all rows
            $("#select-all-btn").on('change', function() {
                if($(this).is(":checked")) {
                    selectAll();
                } else {
                    deselectAll(); 
                }
            });

            });
            //DOCUMENT.READY END

            function downloadFile(data, name) {
                var decodedData = atob(data);
                $.fn.dataTable.fileSave(
                    new Blob([decodedData]), name
                );
            }

            function checkforImageURL(potentialURL) {
                if (potentialURL === undefined || potentialURL === null || potentialURL === "") {
                    return false;
                } else if (potentialURL.startsWith("www.") || potentialURL.startsWith("http://") || potentialURL.startsWith("https://") || potentialURL.startsWith("s3://")) {
                    if (potentialURL.endsWith(".png") || potentialURL.endsWith(".jpg") || potentialURL.endsWith(".jpeg") || potentialURL.endsWith(".gif")) {
                        return true;
                    }
                }
                try {
                    new URL(potentialURL);
                    return true;
                }
                catch (e) {
                    return false;
                }
            }

            //fetches processes from server (using filters if provided) and displays them in the datatable
            function fetchAndDisplayProcesses() {
                //create our qstring
                var qstring = "?";
                if (params != null) {
                    for (p in params) {
                        qstring += encodeURI(p) + "=" + encodeURI(params[p]) + "&";
                    }
                }
                qstring = qstring.substring(0, qstring.length - 1);
                //fetch number of processes
                var numProcs = 0;
                //show ajax spinner
                $(".ajax-spinner").show();
                $.ajax({
                    url: "/${base}/rest/processes/getInstancesSize" + qstring,
                    type: "GET",
                    async: false,
                    success: function (data) {
                        numProcs = data;
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log("Error getting number of processes: " + thrownError);
                    }
                });
                //fetch processes
                var numCalls = Math.ceil(numProcs / 50);
                var returnedData = [];
                var doneArr = [];
                var urlPageAddition = "";
                if (qstring === "") {
                    urlPageAddition = "?page=";
                } else {
                    urlPageAddition = "&page=";
                }
                for (var i = 0; i < numCalls; i++) {
                    $.ajax({
                        url: "/${base}/rest/processes/getInstancesCamunda" + qstring + urlPageAddition + i,
                        type: "GET",
                        async: true,
                        success: function (data) {
                            returnedData = returnedData.concat(data);
                            doneArr.push(true);
                        },
                        error: function (xhr, ajaxOptions, thrownError) {
                            console.log("Error getting processes: " + thrownError);
                        }
                    });
                }
                //we need to wait until all ajax calls are done before we can display the data
                var interval = setInterval(function () {
                    if (doneArr.length === numCalls) {
                        clearInterval(interval);
                        $("#processes-table").DataTable().clear();
                        $("#processes-table").DataTable().rows.add(returnedData).draw();
                        $("#processes-table").DataTable().responsive.recalc();
                        updateSelectDropDown();
                        //hide ajax spinner
                        $(".ajax-spinner").hide();
                    }
                }, 250);

                setTimeout(function () {
                    //apply our local filter
                    if (params != null && params["searchBuilder"] != null && params["searchBuilder"] !== undefined) {
                        var sbState = JSON.parse(decodeURIComponent(decodeURIComponent(params["searchBuilder"])));
                        $("#processes-table").DataTable().searchBuilder.rebuild(sbState);
                    }
                }, 250);
            }

            //Update the text of the select dropdown to reflect number of rows loaded/in datatable
            function updateSelectDropDown() {
                //we need to get the # of rows in the entire table & number of rows on current page
                var numTotalRows = $("#processes-table").DataTable().rows().count();
                var numCurrentRows = $("#processes-table").DataTable().rows({ page: 'current' }).count();
                if (numTotalRows === 0) {
                    $("#selectDropdownBtn").removeAttr("data-toggle");
                } else {
                    $("#selectDropdownBtn").attr("data-toggle", "dropdown");
                }
                if (numTotalRows === 1) {
                    $("#selectOnPage").html("Select 1 process on this page");
                    $("#selectAll").html("Select 1 process");
                    $("#deselectOnPage").html("Deselect 1 process on this page");
                    $("#deselectAll").html("Deselect 1 process");
                } else {
                    $("#selectOnPage").html("Select " + numCurrentRows + " processes on this page");
                    $("#selectAll").html("Select all " + numTotalRows + " processes");
                    $("#deselectOnPage").html("Deselect " + numCurrentRows + " processes on this page");
                    $("#deselectAll").html("Deselect all " + numTotalRows + " processes");
                }
            }

            //selects rows on current page
            function selectPage() {
                $("#processes-table").DataTable().rows({ page: 'current' }).select();
                updateActionList();
            }

            //selects rows that meet filter (no filter = all rows)
            function selectAll() {
                $("#processes-table").DataTable().rows({ filter: "applied" }).select();
                updateActionList();
            }

            //inverts the selection status of rows on current page
            function invertPage() {
                //go through all rows on current page and invert their selection
                $("#processes-table").DataTable().rows({ page: 'current' }).every(function () {
                    if (this.selected()) {
                        this.deselect();
                    } else {
                        this.select();
                    }
                });
                updateActionList();
            }

            //inverts the selection status of rows that meet filter (no filter = all rows)
            function invertAll() {
                //go through all rows and invert their selection
                $("#processes-table").DataTable().rows({ filter: "applied" }).every(function () {
                    if (this.selected()) {
                        this.deselect();
                    } else {
                        this.select();
                    }
                });
                updateActionList();
            }

            //deselects rows on current page
            function deselectPage() {
                $("#processes-table").DataTable().rows({ page: 'current' }).deselect();
                updateActionList();
            }

            //deselects rows that meet filter (no filter = all rows)
            function deselectAll() {
                $("#processes-table").DataTable().rows({ filter: "applied" }).deselect();
                updateActionList();
            }

            //applies filters given by user & refreshes the page to apply to URL
            function updateLocation(changeHideSubs, sbDetailCheck) {
                var localParams = {};
                if ($("#pd-select").val() != "def") {
                    localParams["procDefKey"] = $("#pd-select").val();
                }
                localParams["status"] = '';
                $("#status-select input:checked").each(function () {
                    localParams["status"] += $(this).val() + ',';
                });
                if (localParams["status"] != '')
                    localParams["status"] = localParams["status"].substr(0, localParams["status"].length - 1);
                else
                    delete localParams['status'];
                if ($("#super-proc-inst-id-in").val() != "") {
                    localParams["superProcInstId"] = $("#super-proc-inst-id-in").val();
                }
                if ($("#min-date").val() != "") {
                    localParams["minDate"] = encodeURIComponent($("#min-date").val());
                }
                if ($("#max-date").val() != "") {
                    localParams["maxDate"] = encodeURIComponent($("#max-date").val());
                }
                if ($("#max-return-all").prop("checked")) {
                    localParams["maxReturn"] = -1;
                } else {
                    localParams["maxReturn"] = $("#max-return-num").val();
                }
                if ($("#hide-subprocs-btn").prop("checked")) {
                    localParams["superProcInstId"] = "null";
                } else if (!($("#hide-subprocs-btn").prop("checked"))) {
                    delete localParams["superProcInstId"];
                }
                if (sbDetailCheck == 1) {
                    var sbDetails = $("#processes-table").DataTable().searchBuilder.getDetails();
                    if (sbDetails !== {}) {
                        localParams["searchBuilder"] = encodeURIComponent(JSON.stringify(sbDetails));
                    }
                }

                var qstring = "?";
                if (localParams != null) {
                    for (p in localParams) {
                        qstring += encodeURI(p) + "=" + encodeURI(localParams[p]) + "&";
                    }
                }
                localStorage.setItem(qStringVar, qstring);
                qstring = qstring + "cache=false"
                console.log(encodeURI(qstring));
                window.location = "/${base}/processes" + qstring;
            }

            //gets the number of processes in DB that match the filters that user has applied
            function getNumMatchingProcesses() {
                var localParams = {};

                if ($("#pd-select").val() != "def") {
                    localParams["procDefKey"] = $("#pd-select").val();
                }
                localParams["status"] = '';
                $("#status-select input:checked").each(function () {
                    localParams["status"] += $(this).val() + ',';
                });
                if (localParams["status"] != '')
                    localParams["status"] = localParams["status"].substr(0, localParams["status"].length - 1);
                else
                    delete localParams['status'];
                if ($("#super-proc-inst-id-in").val() != "") {
                    localParams["superProcInstId"] = $("#super-proc-inst-id-in").val();
                }
                if ($("#min-date").val() != "") {
                    localParams["minDate"] = encodeURIComponent($("#min-date").val());
                }
                if ($("#max-date").val() != "") {
                    localParams["maxDate"] = encodeURIComponent($("#max-date").val());
                }
                if ($("#max-return-all").prop("checked")) {
                    localParams["maxReturn"] = -1;
                } else {
                    localParams["maxReturn"] = $("#max-return-num").val();
                }
                if ($("#hide-subprocs-btn").prop("checked")) {
                    localParams["superProcInstId"] = "null";
                } else {
                    delete localParams["superProcInstId"];
                }
                var qstring = "?";
                if (localParams != null) {
                    for (p in localParams) {
                        qstring += encodeURI(p) + "=" + encodeURI(localParams[p]) + "&";
                    }
                }
                qstring = qstring.substring(0, qstring.length - 1);
                var numMatching = 0;
                $.ajax({
                    url: "/${base}/rest/processes/getInstancesSize" + qstring,
                    type: "GET",
                    async: false,
                    success: function (data) {
                        numMatching = data;
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log("Error getting number of processes: " + thrownError);
                        numMatching = "Error";
                    }
                });
                $("#numMatchProcesses").text(numMatching);
                if (numMatching > 5000) {
                    $("#procCountWarning").text("Warning: Large number of processes may increase load time.");
                } else {
                    $("#procCountWarning").text("");
                }
            }

            //gets value of filters from URL and applys them in the GUI
            function applyParamsToFilters(params) {
                if (params != null) {
                    $("#pd-select").val(params.procDefKey || "def");
                    if (params.status) {
                        var k = params.status.split(',');
                        for (i in k) {
                            $("#status-select input[value='" + k[i] + "']").prop("checked", true);
                        }
                    }
                    if (!params) {
                        $("#super-proc-inst-id-in").show();
                        $("#hide-subprocs-btn").prop('checked', false);
                        $("#display-subprocs-div").css('display', 'none');
                    } else if (params.superProcInstId == undefined) {
                        $("#super-proc-inst-id-in").show();
                        $("#hide-subprocs-btn").prop('checked', false);
                        $("#display-subprocs-div").css('display', 'none');
                    } else if (params.superProcInstId.toLowerCase() === 'null') {
                        $("#super-proc-inst-id-in").hide();
                        $("#hide-subprocs-btn").prop('checked', true);
                        $("#display-subprocs-div").css('display', 'none');
                    } else {
                        $("#hide-subprocs-btn").prop('checked', false);
                        $("#super-proc-inst-id-in").show();
                        $("#hide-subprocs-div").css('display', 'block');
                        $("#super-proc-inst-id").html(params.superProcInstId);
                    }
                    //$("#status-select").val(params.status);
                    $("#min-date").val(params.minDate || "");
                    $("#max-date").val(params.maxDate || "");
                    $("#max-return").val(params.maxReturn || 5000);
                    $("#super-proc-inst-id-in").val(params.superProcInstId || "");
                }
            }

            // ---------------------------------
            // DISPLAY STATUS MESSAGE (IF ANY)
            //
            function displayMessage() {

                if ($("#statusMessageDiv:contains('ERROR:')").length >= 1) {
                    $("#statusMessageDiv").css("color", "red");
                } else {
                    $("#statusMessageDiv").css("color", "green");
                    if ($('#statusMessageDiv').html().length > 9) {
                        $('#statusMessageDiv').fadeOut(5000, "linear");
                    }
                }
            }

            // loads the history page of a process
            function viewHistory(procInstId) {

                if (procInstId !== '') {
                    window.location = "/${base}/history?procInstId=" + procInstId;
                } else {
                    return false;
                }
            }

            // loads the sub-processes page of a process
            function viewSubProcs(procInstId) {
                if (procInstId !== '') {
                    // Update URL params
                    params["superProcInstId"] = procInstId;
                    delete params["cache"];
                    
                    // Update UI elements
                    $("#hide-subprocs-btn").prop('checked', false);
                    $("#super-proc-inst-id-in").val(procInstId);
                    $("#super-proc-inst-id-in").show();
                    
                    // Save state and reload data
                    localStorage.setItem(hideSubProcsVar, false);
                    fetchAndDisplayProcesses();
                    
                    // Update URL without page reload
                    var qstring = "?";
                    for (p in params) {
                        qstring += encodeURI(p) + "=" + encodeURI(params[p]) + "&";
                    }
                    qstring = qstring.substring(0, qstring.length - 1);
                    window.history.pushState({}, '', "/${base}/processes" + qstring);
                } else {
                    return false;
                }
            }

            // ---------------------------------------------------------------
            // Updates the list of active items in the Actions drop-down list
            //
            function updateActionList() {
                console.log("updateActionList called");

                var table = $("#processes-table").DataTable();

                var selectedRows = table.rows({ selected: true });

                var numSelected = selectedRows.count();
                var numDisabledSelected = 0;
                var numPendingSelected = 0;
                var numIncidentSelected = 0;
                var numFailedToStartSelected = 0;
                var numFailedSelected = 0;
                var numComplete = 0;
                var numRunning = 0;

                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    switch (data["status"]) {
                        case 'disabled':
                            numDisabledSelected++;
                            break;
                        case 'pending':
                            numPendingSelected++;
                            break;
                        case 'incident':
                            numIncidentSelected++;
                            break;
                        case 'failedToStart':
                            numFailedToStartSelected++;
                            break;
                        case 'fail':
                            numFailedSelected++;
                            break;
                        case 'complete':
                            numComplete++;
                            break;
                        case 'running':
                            numRunning++;
                            break;
                    }
                });

                if (numSelected > 0) {
                    var disabled = numDisabledSelected == numSelected;
                    var pending = numPendingSelected == numSelected;
                    var incident = numIncidentSelected == numSelected;
                    var failedToStart = numFailedToStartSelected == numSelected;
                    var failed = numFailedSelected == numSelected;
                    var running = numRunning == numSelected;
                }

                // Disable everything
                $("#action_disable").addClass("disabled");
                $("#action_disable").removeClass("enabled");
                $("#action_enable").addClass("disabled");
                $("#action_enable").removeClass("enabled");
                $("#action_retry_incident").addClass("disabled");
                $("#action_retry_incident").removeClass("enabled");
                $("#action_retry_failed_to_start").addClass("disabled");
                $("#action_retry_failed_to_start").removeClass("enabled");
                $("#action_mark_as_resolved").addClass("disabled");
                $("#action_mark_as_resolved").removeClass("enabled");
                $("#action_open_selected_new_tabs").addClass("disabled");
                $("#action_open_selected_new_tabs").removeClass("enabled");
                $("#action_copy_all_selected_history_links").addClass("disabled");
                $("#action_copy_all_selected_history_links").removeClass("enabled");
                $("#action_download_selected_json").addClass("disabled");
                $("#action_download_selected_json").removeClass("enabled");
                $("#action_download_selected_csv").addClass("disabled");
                $("#action_download_selected_csv").removeClass("enabled");
                $("#action_download_selected_list").addClass("disabled");
                $("#action_download_selected_list").removeClass("enabled");
                $("#action_delete_selected").addClass("disabled");
                $("#action_delete_selected").removeClass("enabled");

                // Remove hrefs from the anchor tags
                $("#action_disable_atag").removeAttr("href");
                $("#action_enable_atag").removeAttr("href");
                $("#action_retry_incident_atag").removeAttr("href");
                $("#action_retry_failed_to_start_atag").removeAttr("href");
                $("#action_mark_as_resolved_atag").removeAttr("href");
                $("#action_open_selected_new_tabs_atag").removeAttr("href");
                $("#action_copy_all_selected_history_links_atag").removeAttr("href");
                $("#action_download_selected_json_atag").removeAttr("href");
                $("#action_download_selected_csv_atag").removeAttr("href");
                $("#action_download_selected_list_atag").removeAttr("href");
                $("#action_delete_selected_atag").removeAttr("href");

                // Enable the right one

                // only disabled rows are selected
                if (disabled) {
                    $("#action_enable").removeClass("disabled");
                    $("#action_enable_atag").attr("href", "javascript:action_enable_rows();");
                }
                // only pending rows are selected
                else if (pending) {
                    $("#action_disable").removeClass("disabled");
                    $("#action_disable_atag").attr("href", "javascript:action_disable_rows();");
                }
                // only incident rows are selected
                else if (incident) {
                    $("#action_retry_incident").removeClass("disabled");
                    $("#action_retry_incident_atag").attr("href", "javascript:action_retry_incident_rows()");
                }
                // only failedToStart rows are selected
                else if (failedToStart) {
                    $("#action_retry_failed_to_start").removeClass("disabled");
                    $("#action_retry_failed_to_start_atag").attr("href", "javascript:action_retry_failed_to_start();");
                }
                // only failed rows are selected
                else if (failed) {
                    $("#action_mark_as_resolved").removeClass("disabled");
                    $("#action_mark_as_resolved_atag").attr("href", "javascript:action_mark_as_resolved();");
                } else if (running) {
                    $("#action_delete_selected").removeClass("disabled");
                    $("#action_delete_selected_atag").attr("href", "javascript:action_delete_selected();");
                }

                if ((numSelected > 0)) {
                    $("#action_download_selected_list").removeClass("disabled");
                    $("#action_download_selected_list_atag").attr("href", "javascript:downloadListJSON();");
                    if (numPendingSelected === 0) {
                        $("#action_open_selected_new_tabs").removeClass("disabled");
                        
                        $("#action_open_selected_new_tabs_atag").on("click", function() {action_open_selected_new_tabs();})
                        // $("#action_open_selected_new_tabs_atag").attr("href", "javascript:action_open_selected_new_tabs();");
                        
                        $("#action_copy_all_selected_history_links").removeClass("disabled");
                        $("#action_copy_all_selected_history_links_atag").on("click", function() {action_copy_all_selected_history_links();})
                        $("#action_download_selected_json").removeClass("disabled");

                        $("#action_download_selected_json_atag").on("click", function() {downloadSelectedJSON();})
                        // $("#action_download_selected_json_atag").attr("href", "javascript:downloadSelectedJSON();");
                        
                        $("#action_download_selected_csv").removeClass("disabled");

                        $("#action_download_selected_csv_atag").on("click", function() {downloadSelectedCSV();})
                        // $("#action_download_selected_csv_atag").attr("href", "javascript:downloadSelectedCSV();");
                    }
                }

                // Execute adaptation actions if any
                updateAdaptationActionList();
            }

            function action_delete_selected() {
                var table = $("#processes-table").DataTable();
                var selectedRows = table.rows({ selected: true });
                var procInstIds = [];
                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    procInstIds.push(this.data()["procInstId"]);
                });
                    $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/delete",
                    Accept: "application/json",
                    contentType: "application/json",
                    data: JSON.stringify(procInstIds),
                    success: function (msg) {
                        fetchAndDisplayProcesses();
                    }
                    })
                    .fail(function (xhr, err) {
                        console.error(xhr.responseTextmsg);
                        console.error(err);
                    })
            }

            //opens selected rows' history pages in new tabs
            function action_open_selected_new_tabs() {
                var table = $("#processes-table").DataTable();
                var selectedRows = table.rows({ selected: true });
                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    window.open("/${base}/history?procInstId=" + data["procInstId"], "_blank");
                });
            }

            //copies selected rows' history page links to clipboard
            function action_copy_all_selected_history_links() {
                var table = $("#processes-table").DataTable();
                const protocol = window.location.protocol;
                const host = window.location.host;
                var selectedRows = table.rows({ selected: true });
                var links = "";
                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    links += protocol + "://" + host + "/${base}/history?procInstId=" + data["procInstId"] + "\n";
                });
                navigator.clipboard.writeText(links);
            }

            // -------------------------------------------------------------------------------
            // Function fired when user clicks on "Enable Selected Rows..." in drop-down list
            //
            function action_enable_rows() {
                var table = $("#processes-table").DataTable();
                $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/makeDisabledRowsPending",
                    dataType: "json",
                    Accept: "application/json",
                    contentType: "application/json",
                    data: JSON.stringify(getSelectedRowUuids())
                })
                    .done(function (msg) {
                        $("#action_msg").html(msg.message);
                        fetchAndDisplayProcesses();
                    })
                    .fail(function (xhr, err) {
                        $("#action_msg").html(xhr.responseTextmsg.message);
                    });
            }

            // --------------------------------------------------------------------------------
            // Function fired when user clicks on "Disable Selected Rows..." in drop-down list
            //
            function action_disable_rows() {
                var table = $("#processes-table").DataTable();
                $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/makePendingRowsDisabled",
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    data: JSON.stringify(getSelectedRowUuids())
                })
                    .done(function (msg) {
                        $("#action_msg").html(msg.message);
                        fetchAndDisplayProcesses();
                    })
                    .fail(function (xhr, err) {
                        $("#action_msg").html(xhr.responseTextmsg.message);
                    });
            }

            // --------------------------------------------------------------------------------
            // Function fired when user clicks on "Retry Selected Incident Rows..." in drop-down list
            //
            function action_retry_incident_rows() {
                var table = $("#processes-table").DataTable();
                $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/retryIncidentRows",
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    data: JSON.stringify(getSelectedRowUuids())
                })
                    .done(function (msg) {
                        $("#action_msg").html(msg.message);
                        table.ajax.reload();
                    })
                    .fail(function (xhr, err) {
                        $("#action_msg").html(xhr.responseTextmsg.message);
                    });
            }

            // --------------------------------------------------------------------------------
            // Function fired when user clicks on "Retry Selected Failed to Start Rows..." in drop-down list
            //
            function action_retry_failed_to_start() {
                var table = $("#processes-table").DataTable();
                $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/retryFailedToStart",
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    data: JSON.stringify(getSelectedRowUuids())
                })
                    .done(function (msg) {
                        $("#action_msg").html(msg.message);
                        fetchAndDisplayProcesses();
                    })
                    .fail(function (xhr, err) {
                        $("#action_msg").html(xhr.responseTextmsg.message);
                    });
            }

            // --------------------------------------------------------------------------------
            // Function fired when user clicks on "Mark Selected Failed Rows As Resolved..." in drop-down list
            //
            function action_mark_as_resolved() {
                var table = $("#processes-table").DataTable();
                $.ajax({
                    type: "POST",
                    url: "/${base}/rest/processes/markResolved",
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    data: JSON.stringify(getSelectedRowUuids())
                })
                    .done(function (msg) {
                        $("#action_msg").html(msg.message);
                        fetchAndDisplayProcesses();
                    })
                    .fail(function (xhr, err) {
                        $("#action_msg").html(xhr.responseTextmsg.message);
                    });
            }

            // ------------------------------------
            // Get array of selected rows (uuids)
            //
            function getSelectedRowUuids() {
                var selectedRowUuids = [];
                //
                // For each selected row...
                //
                var table = $('#processes-table').DataTable();
                var selectedRows = table.rows({ selected: true });

                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    var status = data["status"];
                    var uuid = data["uuid"];
                    var procInstId = data["procInstId"];
                    switch (status) {
                        case 'disabled':
                            selectedRowUuids.push(uuid);
                            break;
                        case 'pending':
                            selectedRowUuids.push(uuid);
                            break;
                        case 'incident':
                            selectedRowUuids.push(procInstId);
                            break;
                        case 'failedToStart':
                            selectedRowUuids.push(uuid);
                            break;
                        case 'fail':
                            selectedRowUuids.push(procInstId);
                            break;
                    }
                });
                return selectedRowUuids;
            }

            //downloads the selected rows' log files as JSON
            function downloadSelectedJSON() {
                var mainJSON = {};
                //get selected rows
                var table = $('#processes-table').DataTable();
                var selectedRows = table.rows({ selected: true });
                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    var procInstId = data["procInstId"];
                    var json = getInstanceJSON(procInstId);
                    mainJSON[procInstId] = json;
                });
                $.fn.dataTable.fileSave(
                    new Blob([JSON.stringify(mainJSON)]),
                    'processes-' + moment().format('MMM-DD-YYYY-hh-mm-a') + '.json'
                );
            }

            //downloads the selected rows' log files as CSV
            function downloadSelectedCSV() {
                var mainCSV = `"process_definition","process_instance","time stamp","type","source","details"\r\n`;
                //get selected rows
                var table = $('#processes-table').DataTable();
                var selectedRows = table.rows({ selected: true });
                selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    var procInstId = data["procInstId"];
                    var csv = getInstanceCSV(procInstId);
                    mainCSV += csv;
                });
                $.fn.dataTable.fileSave(
                    new Blob([mainCSV]),
                    'processes-' + moment().format('MMM-DD-YYYY-hh-mm-a') + '.csv'
                );
            }

            //intercepts the click event on the download json button
            $("#json-bttn").on("click", function (e) {
                e.preventDefault();
                downloadListJSON();
            });

            //downloads a json list of the statuses of selected rows (less detail than functions above)
            function downloadListJSON() {
                var dt = $('#processes-table').DataTable();
                //number of rows
                var numRows = dt.rows({ selected: true }).count();
                var jsonFile = {};
                var processes = {};
                var noProcInstIDCounter = 0;

                dt.rows({ selected: true}).every(function (rowIdx, tableLoop, rowLoop) {
                    var data = this.data();
                    console.log(data);
                    var thisProcJSON = {};
                    var startedOnWorker = "";
                    var workerIP = "";
                    var duration = "";
                    var process_end = "";
                    var inputVars = "";
                    var inputVarsTemp = "";

                    if (data["startedByWorker"] !== "" && data["startedByWorker"] !== null) {
                        startedOnWorker = data["startedByWorker"];
                        workerIP = data["startedByWorker"].split("_").slice(0, -2).join(".");
                    } else {
                        startedOnWorker = data["startedByWorker"];
                    }

                    if (data["procEndTime"] !== null) {
                        process_end = data["procEndTime"];
                        if (data["procStartTime"] !== '' && data["procEndTime"] !== '') {
                            var start = moment(data["procStartTime"]);
                            var end = moment(data["procEndTime"]);
                            duration = moment.duration(end.diff(start)).humanize();
                        } else {
                            duration = '';
                        }
                    }

                    if (data["inputVariables"] !== {}) {
                        for (var key in data["inputVariables"]) {
                            inputVarsTemp += key + ": " + data["inputVariables"][key] + ", ";
                        }
                        if (inputVarsTemp.length > 2) {
                            inputVars = inputVarsTemp.substring(0, inputVarsTemp.length - 2);
                        }
                    }

                    thisProcJSON["definition_key"] = data["procDefKey"];
                    thisProcJSON["process_instance_id"] = data["procInstId"];
                    thisProcJSON["status"] = data["status"];
                    thisProcJSON["schedule_queued_time"] = data["createdTimestamp"];
                    thisProcJSON["started_on_worker"] = startedOnWorker;
                    thisProcJSON["worker_ip"] = workerIP;
                    thisProcJSON["process_start"] = data["procStartTime"];
                    thisProcJSON["process_end"] = process_end;
                    thisProcJSON["duration"] = duration;
                    thisProcJSON["input_variables"] = inputVars;

                    if (data["procInstId"] !== null && data["procDefKey"] !== "") {
                        processes[data["procInstId"]] = thisProcJSON;
                    } else {
                        processes["no_assigned_proc_inst_id_" + noProcInstIDCounter] = thisProcJSON;
                        noProcInstIDCounter++;
                    }

                });
                jsonFile["processes"] = processes;
                console.log(jsonFile);
                $.fn.dataTable.fileSave(
                    new Blob([JSON.stringify(jsonFile)]),
                    'processes_export.json'
                );
            }

            //gets the JSON for a single process
            function getInstanceJSON(procInstId) {
                var outputJSON = {};
                var logLinesJSON = {};
                var logLines = [];
                var scrollId = "";
                var baseEsReq = {
                    "from": 0,
                    "size": 20,
                    "query": {
                        "bool": {
                            "must": []
                        }
                    },
                    "sort": { "@timestamp": { "order": "asc" } }
                };
                baseEsReq.query.bool.must.push({ "query_string": { "fields": ["procInstId"], "query": "\"" + decodeURIComponent(procInstId) + "\"" } });

                //get process history
                $.ajax({
                    type: "GET",
                    url: "/${base}/rest/history/" + procInstId,
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    async: false,
                    success: function (data) {
                    var status = data.state;
                    if (data.state === "COMPLETED") {
                        status = "Complete";
                    }
                    else if (data.state === "ACTIVE") {
                        status = "Running";
                    }
                    var proc_info = {
                        "process_definition": data.procDefKey,
                        "process_instance": data.procInstId,
                        "start_time": data.startTime,
                        "end_time": data.endTime,
                        "duration": convertMillis(data.duration),
                        "status": status,
                        "input_variables": data.inputVariables,
                        "output_variables": data.outputVariables
                    };
                    outputJSON["process_info"] = proc_info;
                    for (const entry of data.details) {
                        let date = entry["date"];
                        if (entry["message"].startsWith("Ended ")) {
                            date += " ";
                        }
                        const row = [date, entry["type"], entry["activity"], outputMessage(entry["message"])];
                        logLines.push(row);
                    }
                    }
                }).fail(function (xhr, err) {
                    console.error("Error getting instance JSON: " + xhr.responseText);
                });

                $.ajax({
                    type: "GET",
                    url: "/${base}/rest/logs/get?source=" + encodeURIComponent(JSON.stringify(baseEsReq)),
                    Accept: "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    async: false,
                    success: function (data) {
                    var finished = false;
                    scrollId = data._scroll_id;
                    if (data.hits) {
                        for (const hit of data.hits.hits) {
                            const source = hit._source;
                            const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                            logLines.push(row);

                        }
                    }
                    while (!finished) {
                        $.ajax({
                            type: "POST",
                            url: "/${base}/rest/logs/get/scroll",
                            data: "scrollId=" + scrollId,
                            async: false,
                            success: function (data) {
                                if (data.hits) {

                                    if (data.hits.hits.length > 0) {
                                        for (const hit of data.hits.hits) {
                                            const source = hit._source;
                                            const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                                            logLines.push(row);
                                        }
                                        scrollId = data._scroll_id;
                                    }
                                    else {
                                        finished = true;
                                    }
                                }
                            },
                            error: function (e) {
                                alert("Error retrieving history data.");
                            }
                        });
                    }
                }
                }).fail(function (xhr, err) {
                    console.error("Error getting instance JSON: " + xhr.responseText);
                });
                logLines.sort(function (a, b) {
                    var aTemp = a[0];
                    //if there is a space in the last char, remove it
                    if (aTemp.charAt(aTemp.length - 1) == " ") {
                        aTemp = aTemp.substring(0, aTemp.length - 1);
                    }
                    var bTemp = b[0];
                    //if there is a space in the last char, remove it
                    if (bTemp.charAt(bTemp.length - 1) == " ") {
                        bTemp = bTemp.substring(0, bTemp.length - 1);
                    }
                    var aDate = moment(aTemp);
                    var bDate = moment(bTemp);
                    if (aDate.isBefore(bDate)) return -1;
                    if (bDate.isBefore(aDate)) return 1;
                    return 0;
                });

                var i = 0;
                logLines.forEach(function (row) {
                    var data = row;
                    var tmpDetails = data[3];
                    var details = "";
                    var lineJson = {};
                    var nestedJson = {};
                    //go through data[0] and if there is a space at the end, remove it
                    if (data[0].charAt(data[0].length - 1) == " ") {
                        data[0] = data[0].substring(0, data[0].length - 1);
                    }
                    if (data[3].indexOf("Setting (json)") === -1) {
                        //check if data[3] starts with "<table><tr>". If it does, remove it.
                        if (data[3].startsWith("<table><tr>")) {
                            tmpDetails = data[3].substring(11);
                        }
                        details = data[3];
                        lineJson = {
                            "time-stamp": data[0],
                            "type": data[1],
                            "source": data[2],
                            "details": details
                        };
                    } else {
                        var fixedDetails = "";
                        if (data[3].startsWith("<table><tr>")) {
                            data[3] = data[3].substring(11);
                        }
                        //we need to first separate the string from the rest of the HTML
                        if (data[3].indexOf("_in =") !== -1) {
                            details = data[3].substring(0, data[3].indexOf("_in =") + 3);
                            tmpDetails = data[3].substring(data[3].indexOf("_in =") + 3);
                        } else {
                            details = data[3].substring(0, data[3].indexOf("_out =") + 4);
                            tmpDetails = data[3].substring(data[3].indexOf("_out =") + 4);
                        }
                        //now we need to go through and get details from json string
                        //note: key is always after <tr><td ...> and value is the following td
                        while (tmpDetails.indexOf("<tr><td") !== -1) {
                            tmpDetails = tmpDetails.substring(tmpDetails.indexOf("<tr><td") + 8);
                            tmpDetails = tmpDetails.substring(tmpDetails.indexOf(">") + 1);
                            var key = tmpDetails.substring(0, tmpDetails.indexOf("</td>"));
                            tmpDetails = tmpDetails.substring(tmpDetails.indexOf("<td>") + 4);
                            var value = tmpDetails.substring(0, tmpDetails.indexOf("</td>"));
                            nestedJson[key] = value;
                        }
                        //check/clean nested json object
                        if (nestedJson["stdout"] !== undefined) {
                            //replace all break points with new line
                            nestedJson["stdout"] = nestedJson["stdout"].replaceAll(/<br>/g, "\n");
                            //find and remove everything between <summary>  and  </summary>
                            nestedJson["stdout"] = nestedJson["stdout"].replace(/<summary>.*<\/summary>/g, "");
                        }
                        lineJson = {
                            "time-stamp": data[0],
                            "type": data[1],
                            "source": data[2],
                            "details": details,
                            "json": nestedJson
                        };
                    }
                    //check/clean details
                    if (lineJson["details"] !== "") {
                        //replace all break points with new line
                        details = details.replaceAll('<br>', "\n");
                        details = details.replaceAll('<br/>', "\n");
                        details = details.replaceAll("<p>", "");
                        details = details.replaceAll("</p>", "");
                        lineJson["details"] = details;
                    }
                    logLinesJSON[i] = lineJson;
                    i++;
                });
                outputJSON["logs"] = logLinesJSON;
                return outputJSON;
            };

            function outputMessage(msg) {

                if (msg.startsWith("Setting (json) ")) {

                    var i2 = msg.indexOf("= ")

                    if (i2 != -1) {
                        var cmd = msg.substring(0, i2 + 1)
                        var jsonObj = JSON.parse(msg.substring(i2 + 2))
                        var output = '<table><tr>' + cmd + '<br/><br/><table id=\"logDataNest\" class=\"table table-striped table-bordered\">'

                        Object.keys(jsonObj).forEach(function (key) {
                            var value = jsonObj[key];
                            output += makeRow(key, value, cmd)
                        });

                        output += '</table>'

                        return output
                    }
                }

                return msg.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
            }

            function makeRow(key, value, cmd) {

                var style = 'width: 210px;'

                if (cmd.endsWith('_out =')) {
                    style = 'width: 120px;'
                }

                if (key == 'stdout' || key == 'stderr') {
                    return '<tr><td style="' + style + ';font-weight:bold;">' + key + '</td><td>' + formatMsg(value) + '</td></tr>'
                }
                return '<tr><td style="' + style + ';font-weight:bold;">' + key + '</td><td>' + value + '</td></tr>'
            }

            function formatMsg(msg) {

                var index = 0, count = 0, maxCount = 30

                for (; count < maxCount && i2 != -1; count++) {

                    var i2 = msg.indexOf('\n', index)

                    if (i2 != -1) {
                        index = i2 + 1
                    }
                }

                if (count < maxCount - 1 || index > msg.length / 2) {
                    return msg.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
                }

                var first = msg.substring(0, index).replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")
                var rest = msg.substring(index).replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>")

                return first + '<details><summary>Show All</summary>' + rest + '</details>'
            }

            function convertMillis(millis) {

                var x = millis / 1000
                var seconds = Math.floor(x % 60)
                x /= 60
                var minutes = Math.floor(x)

                if (minutes === 0)
                    return millis / 1000 + " sec";

                return minutes + " min " + seconds + " sec"
            }

            //gets csv string of a specific process (does not include header)
            function getInstanceCSV(procInstId) {
                var outputCSV = "";
                var logLines = [];
                var scrollId = "";
                var proc_info = {};
                var baseEsReq = {
                    "from": 0,
                    "size": 20,
                    "query": { 
                        "bool": {
                            "must" :[]
                        }
                    },
                    "sort": { "@timestamp": { "order": "asc" } }
                };
                baseEsReq.query.bool.must.push({"query_string":{"fields":["procInstId"],"query" : "\"" + decodeURIComponent(procInstId) + "\""}});

                //get process history
                $.ajax({
                    type: "GET",
                    url: "/${base}/rest/history/" + procInstId,
                    Accept : "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    async: false,
                    success: function(data) {
                    var status = data.state;
                    if (data.state === "COMPLETED") {
                        status = "Complete";
                    }
                    else if (data.state === "ACTIVE") {
                        status = "Running";
                    }
                    proc_info["process_definition"] = data.procDefKey;
                    proc_info["process_instance"] = data.procInstId;
                    proc_info["start_time"] = data.startTime;
                    proc_info["end_time"] = data.endTime;
                    proc_info["duration"] = convertMillis(data.duration);
                    proc_info["status"] = status;
                    for (const entry of data.details) {
                        let date = entry["date"];
                        if (entry["message"].startsWith("Ended ")) {
                            date += " ";
                        }
                        const row = [date, entry["type"], entry["activity"], outputMessage(entry["message"])];
                        logLines.push(row);
                    }
                }
                }).fail(function(xhr, err) {
                    console.error("Error getting instance JSON: " + xhr.responseText);
                });

                $.ajax({
                    type: "GET",
                    url: "/${base}/rest/logs/get?source=" + encodeURIComponent(JSON.stringify(baseEsReq)),
                    Accept : "application/json",
                    contentType: "application/json",
                    dataType: "json",
                    async: false,
                    success:function(data) {
                    var finished = false;
                    scrollId = data._scroll_id;
                    if (data.hits) {
                        for (const hit of data.hits.hits) {
                            const source = hit._source;
                            const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                            logLines.push(row);
                            
                        }
                    }
                    while (!finished) {
                        $.ajax({
                            type: "POST",
                            url: "/${base}/rest/logs/get/scroll",
                            data: "scrollId=" + scrollId,
                            async: false,
                            success: function(data) {
                                if (data.hits) {
                                    
                                    if (data.hits.hits.length > 0) {
                                        for (const hit of data.hits.hits) {
                                            const source = hit._source;
                                            const row = [source["@timestamp"], "Log", source.actInstId.split(':')[0], "<p>" + source.msgBody.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, "<br/>") + "</p>"];
                                            logLines.push(row);
                                        }
                                        scrollId = data._scroll_id;
                                    }
                                    else {
                                        finished = true;
                                    }
                                }
                            },
                            error: function(e) {
                                alert("Error retrieving history data.");
                            }
                        });
                    }
                }
                }).fail(function(xhr, err) {
                    console.error("Error getting instance JSON: " + xhr.responseText);
                });
                logLines.sort(function(a, b) {
                    var aTemp = a[0];
                    //if there is a space in the last char, remove it
                    if (aTemp.charAt(aTemp.length - 1) == " ") {
                        aTemp = aTemp.substring(0, aTemp.length - 1);
                    }
                    var bTemp = b[0];
                    //if there is a space in the last char, remove it
                    if (bTemp.charAt(bTemp.length - 1) == " ") {
                        bTemp = bTemp.substring(0, bTemp.length - 1);
                    }
                    var aDate = moment(aTemp);
                    var bDate = moment(bTemp);
                    if (aDate.isBefore(bDate)) return -1;
                    if (bDate.isBefore(aDate)) return 1;
                    return 0;
                });

                logLines.forEach(function(row) {
                    var data = row;
                    var details = data[3];
                    var tmpDetails = "";
                    var lineString = "";
                    if (data[3].indexOf("Setting (json)") === -1) {
                        details = details.replaceAll('<br>', "\n");
                        details = details.replaceAll("<p>", "");
                        details = details.replaceAll("</p>", "");
                        details = details.replaceAll('"' , '""');
                        details = details.replaceAll('\n' , ' ');
                        //add first and last char as double quotes
                        details = '"' + details + '"';
                        lineString = proc_info["process_definition"] + "," + proc_info["process_instance"] + "," + data[0] + "," + data[1] + "," + data[2] + "," + details + "\r\n";
                    } else {
                        lineString = proc_info["process_definition"] + "," + proc_info["process_instance"] + "," + data[0] + "," + data[1] + "," + data[2] + ",";
                        //remove last char
                        if (data[3].indexOf("_in =") !== -1) {
                            lineString += '"' + details.substring(0, details.indexOf("_in =")+3) + " ";
                            details = details.substring(details.indexOf("_in =")+3);
                        } else {
                            lineString += '"' + details.substring(0, details.indexOf("_out =")+4) + " ";
                            details = details.substring(details.indexOf("_out =")+4);
                        }
                        //now we need to go through and get details from json string
                        //note: key is always after <tr><td ...> and value is the following td
                        while (details.indexOf("<tr><td") !== -1) {
                            details = details.substring(details.indexOf("<tr><td")+8);
                            details = details.substring(details.indexOf(">")+1);
                            var key = details.substring(0, details.indexOf("</td>"));
                            details = details.substring(details.indexOf("<td>")+4);
                            var value = details.substring(0, details.indexOf("</td>"));
                            tmpDetails += key + ": " + value + "; ";
                        }
                        //check/clean tmpDetails
                        if (tmpDetails !== "") {
                            //replace all break points with new line
                            tmpDetails = tmpDetails.replaceAll(/<br>/g, " ");
                            //find and remove everything between <summary>  and  </summary>
                            tmpDetails = tmpDetails.replace(/<summary>.*<\/summary>/g, "");
                            //find and remove <details>  and  </details>
                            tmpDetails = tmpDetails.replace(/<details>/g, "");
                            tmpDetails = tmpDetails.replace(/<\/details>/g, "");
                            //CSV quirk: replace all " with ""
                            tmpDetails = tmpDetails.replaceAll('"' , '""');
                        }
                        //remove last char
                        tmpDetails = tmpDetails.substring(0, tmpDetails.length-1);
                        tmpDetails = tmpDetails + '"';
                        lineString += tmpDetails + "\r\n";
                    }
                    lineString = lineString.replaceAll("<table><tr>", "");
                    outputCSV = outputCSV + lineString;
                } );
                return outputCSV;
            };

           
        </script>

</body>

</html>
