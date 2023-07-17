<html>
<head>
    <meta charset="utf-8">
    <title>CWS - Processes</title>
    <script src="/${base}/js/jquery.min.js"></script>
    <script src="/${base}/js/bootstrap.min.js"></script>
    <link rel="stylesheet" href="/${base}/js/DataTables/datatables.css"/>
    <script src="/${base}/js/moment.js"></script>
    <script src="/${base}/js/DataTables/datatables.js"></script>
    <script src="/${base}/js/bootstrap-datepicker.min.js"></script>
    <script src="/${base}/js/DataTablesDateFilter.js"></script>
    <!-- Custom js adaptation script; override this file from your adaptation project -->
    <script src="/${base}/js/adaptation-process-actions.js"></script>
    <link href="/${base}/css/bootstrap.min.css" rel="stylesheet">
    <!-- Custom styles for this template -->
    <link href="/${base}/css/dashboard.css" rel="stylesheet">
    <link href="/${base}/css/bootstrap-datepicker.min.css" rel="stylesheet">
    <link href="/${base}/css/microtip.css" rel="stylesheet">
    <style>
        .dataTables_wrapper .filter .dataTables_filter {
            float: right;
            padding-top: 15px;
            display: inline;
        }

        .dataTables_wrapper .length .dataTables_length {
            float: left;
            display: inline;
            padding-top: 15px;
            padding-left: 15px;
            padding-right: 15px;
        }

        .dataTables_wrapper .buttons .dt-buttons {
            float: left;
            display: inline;
            padding-top: 15px;
            padding-left: 15px;
            padding-right: 15px;
        }

        .dataTables_wrapper .action-button {
            padding-top: 15px;
            padding-right: 15px;
        }

        .dataTables_wrapper .download-button {
            padding-top: 15px;
            padding-left: -15px;
            padding-right: 15px;
        }

        .dataTables_wrapper .dtsb-titleRow {
            display: none;
        }

        .dataTables_wrapper .dtsb-group {
            padding-bottom: -15px !important;
            padding-top: 8px;
        }

        summary {
            width: 100px;
            display: list-item;
        }

        #processes-table {
            font-size: 90%;
        }

        #pd-select {
            width: 90%;
        }

        #status-select {
            border: 1px solid #ddd;
            border-radius: 3px;
            padding: 10px;
        }

        #status-select label {
            cursor: pointer;
            padding-left: 5px;
        }

        #datepicker-div input {
            /*width:40%;*/
            margin-bottom: 1em;
            float: left;
        }

        .tr-fail {
            color: #D9534F;
        }

        .tr-complete {
            color: black;
        }

        .tr-running {
            color: #5BC0DE;
        }

        .tr-pending {
            color: #F0AD4E;
        }

        .tr-incident {
            color: #C347ED;
        }

        /*#F142F4*/
        ;
        }

        #hide-subprocs-div {
            margin: 20px 0px;
        }

        #display-subprocs-div {
            margin: 30px 0px;
        }

        #super-proc-inst-id {
            background: #ededed;;
            padding: 5px;
            border-radius: 8px;
            margin-left: 8px;
            padding: 5px;
        }
    </style>

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
        <div class="col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main">

			<span id="statusMessageDiv">
				<h2>${msg}</h2>
			</span>

            <h2 class="sub-header">Processes</h2>

            <div id="filters-div">
                <h3 style="margin-top: 10px;">Filters:</h3>
                <p>Select filters before retrieving data to reduce loading time.</p>
                <div class="col-md-4">
                    <h4>Process Definition:</h4>
                    <select id="pd-select">
                        <option value="def">Select PD</option>
                        <#list procDefs as pd>
                            <option value="${pd.key}">${pd.name}</option>
                        </#list>
                    </select>
                    <div>
                        <h4 style="margin-top: 15px;">Subprocess & Superprocess:</h4>
                        <input id="super-proc-inst-id-in" style="width: 90%" type="text" class="form-control"
                               placeholder="Superprocess Instance ID"/>
                    </div>
                    <div style="margin-top: 15px" id="hide-subprocs-div">
                        <label for="hide-subprocs">Hide Subprocesses</label>
                        <input name="hide-subprocs" id="hide-subprocs-btn" type="checkbox">
                    </div>
                </div>
                <div class="col-md-4">
                    <h4>Status:</h4>
                    <div id="status-select">
                        <input id="fail" type="checkbox" value="fail"/>
                        <label for="fail">Failed</label><br/>
                        <input id="complete" type="checkbox" value="complete"/>
                        <label for="complete">Complete</label><br/>
                        <input id="resolved" type="checkbox" value="resolved"/>
                        <label for="resolved">Resolved</label><br/>
                        <input id="running" type="checkbox" value="running"/>
                        <label for="running">Running</label><br/>
                        <input id="pending" type="checkbox" value="pending"/>
                        <label for="pending">Pending</label><br/>
                        <input id="disabled" type="checkbox" value="disabled"/>
                        <label for="disabled">Disabled</label><br/>
                        <input id="failedToStart" type="checkbox" value="failedToStart"/>
                        <label for="failedToStart">Failed to Start</label><br/>
                        <input id="incident" type="checkbox" value="incident"/>
                        <label for="incident">Incident</label><br/>
                    </div>
                </div>
                <div class="col-md-4">
                    <div id="datepicker-div">
                        <h4>Created Date:</h4>
                        <input id="min-date" class="form-control"
                               data-date-format="yyyy-mm-dd" type="text" placeholder="From...">

                        <input id="max-date" class="form-control"
                               data-date-format="yyyy-mm-dd" type="text" placeholder="To...">
                    </div>
                    <div id="max-return-div">
                        <h4>Max Results:</h4>
                        <input id="max-return-num" class="form-control" type="number" min="1" value="5000">
                        <input id="max-return-all" type="checkbox" value="-1"/>
                        <label style="margin-top: 5px;" for="max-return-all">Return all</label><br/>
                    </div>
                </div>
                <br/>
                <div class="col-md-12">
                    <input type="button" id="filter-submit-btn" class="btn btn-info pull-right" value="Filter"/>
                    <h5 class="pull-right" style="margin-right: 8px;">Matched Processes: <span
                                id="numMatchProcesses"></span></h5>
                    <h5 class="pull-right" id="procCountWarning" style="color: red; margin-right: 8px;"></h5>
                </div>
            </div>


            <div id="filters-btn" class="btn btn-warning"><span class="glyphicon glyphicon-filter">
				</span>&nbsp;Filters&nbsp;<span id="filter-arrow" class="glyphicon glyphicon-chevron-up"></span>
            </div>

            <div id="display-subprocs-div">
                <h3>Displaying Subprocesses for Process Instance ID: <span
                            id="super-proc-inst-id">34374-349083748</span></h3>
            </div>
            <div id="action_msg"></div>

            <div id="proc-log">
                <div class="ajax-spinner" id="ajax-spinner"></div>
                <table id="processes-table" class="table table-striped table-bordered sortable">
                    <thead>
                    <tr>
                        <th style="max-width: 25px; min-width: 15px;"></th>
                        <th></th>
                        <th>Definition Key</th>
                        <th>Proc Inst ID</td>
                        <th>Status</th>
                        <th>Schedule Queued Time</th>
                        <th>Started on Worker</th>
                        <th>Process Start</th>
                        <th>Process End</th>
                        <th style="word-wrap: break-word; min-width: 200px;">Input Variables</th>
                        <th>Superprocess ID</th>
                        <th>UUID</th>
                        <th style="word-wrap: break-word; min-width: 200px;">Output Variables</th>
                    </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<script type="text/javascript">

    //STATE PERSISTANCE CONSTS
    const username = "username"; //temporary, hardcoded value for now
    const hideSubProcsVar = "CWS_DASH_PROCS_HIDE_SUBPROCS-" + username;

    //GLOBAL VARS
    var params = {};

    //DOCUMENT.READY START
    $(document).ready(function () {
        //try to load hideSubProcsVar from local storage. If it doesn't exist, set it to true
        //(automatically hides subprocs if never visited this page before)
        if (localStorage.getItem(hideSubProcsVar) === null) {
            localStorage.setItem(hideSubProcsVar, true);
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

        $("#filters-btn").click(function () {
            if ($("#filters-div").is(":visible"))
                $("#filter-arrow").removeClass("glyphicon-chevron-up").addClass("glyphicon-chevron-down");
            else
                $("#filter-arrow").removeClass("glyphicon-chevron-down").addClass("glyphicon-chevron-up");
            $("#filters-div").slideToggle();
        });

        $("#filter-submit-btn").click(function () {
            updateLocation(false);
        });

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
        $("#filter-submit-btn").click(function () {
            updateLocation(false);
        });
        $("#hide-subprocs-btn").click(function () {
            updateLocation(true);
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

        displayMessage();

        $.fn.dataTable.moment('MMM D, YYYY, h:mm:ss A');

        $("#processes-table").DataTable({
            language: {
                searchBuilder: {
                    add: "Add Criteria"
                }
            },
            deferRender: true,
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
                                return '<a onclick="viewHistory(\'' + data + '\')" href="/${base}/history?procInstId=' + data + '" class="btn btn-default btn-sm disabled">History</a>' +
                                    "<a style=\"margin-top: 5px;\" onclick=\"viewSubProcs('" + data + "')\" href=\"/${base}/processes?superProcInstId=" + data + "\" class=\"btn btn-default btn-sm disabled\">Subprocs</a>";
                            }
                            return '<a onclick="viewHistory(\'' + data + '\')" href="/${base}/history?procInstId=' + data + '" class="btn btn-default btn-sm">History</a>' +
                                "<a style=\"margin-top: 5px;\" onclick=\"viewSubProcs('" + data + "')\" href=\"/${base}/processes?superProcInstId=" + data + "\" class=\"btn btn-default btn-sm\">Subprocs</a>";
                        }
                        return data;
                    }
                },
                {
                    data: "procDefKey",
                },
                {
                    data: {procInstId: "procInstId", status: "status"},
                    render: function (data, type) {
                        if (type === 'display') {
                            if (data.status === "incident") {
                                var incidentUrl = "/camunda/app/cockpit/default/#/process-instance/" + data.procInstId + "/runtime?tab=incidents-tab";
                                return "<a href=\"" + incidentUrl + "\" target=\"blank_\">" + data.procInstId + "</a>";
                            } else {
                                return data.procInstId;
                            }
                        } else {
                            return data;
                        }
                    }
                },
                {data: "status"},
                {data: "createdTimestamp"},
                {
                    data: "startedByWorker",
                    render: function (data, type) {
                        if (type === 'display') {
                            if (data !== null) {
                                return data + "<br><b>Worker IP: </b>" + data.split("_").slice(0, -2).join(".");
                            }
                        }
                        return data;
                    }
                },
                {data: "procStartTime"},
                {
                    data: {procEndTime: "procEndTime", procStartTime: "procStartTime"},
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
                            return data.procEndTime + procDuration;
                        } else {
                            return data.procEndTime;
                        }
                    }
                },
                {
                    data: "inputVariables",
                    render: function (data, type) {
                        if (jQuery.isEmptyObject(data)) {
                            return "";
                        }
                        if (type === 'display') {
                            var output = "";
                            var before = "";
                            var after = "";
                            var putAllAfter = 0;
                            var count = 0;
                            for (const [key, value] of Object.entries(data)) {
								var tempVal = value;
                                if (key === "workerId") {
                                    continue;
                                }
                                if (count > 3) {
                                    putAllAfter = 1;
                                }
								if (key.includes("(file, image)")) {
									tempVal = '<a class="thumbnail">'
										+ '<img src="' + tempVal + '">'
										+ '</a>'
								}
                                var temp = "<div><div style=\"width: 85%; max-width: 300px; min-height: 25px; float:left; overflow-wrap: break-word;\"><b>" + key + ":</b> " + tempVal + "</div><div class=\"copySpan\" style=\"width: 15%; float:right\">"
                                    + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + key + "\" onClick=''>"
                                    + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                    + "</span></div></div><br>";
                                if (key === "startedOnWorkerId") {
                                    after = after + temp;
                                    putAllAfter = 1;
                                } else if (putAllAfter === 0) {
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
                {data: "superProcInstId"},
                {data: "uuid"},
                {
					data: "outputVariables",
					render: function (data, type) {
                        if (jQuery.isEmptyObject(data)) {
                            return "";
                        }
                        if (type === 'display') {
                            var output = "";
                            var before = "";
                            var after = "";
                            var putAllAfter = 0;
                            var count = 0;
                            for (const [key, value] of Object.entries(data)) {
								var tempVal = value;
                                if (key === "workerId") {
                                    continue;
                                }
                                if (count > 3) {
                                    putAllAfter = 1;
                                }
								if (key.includes("(file, image)")) {
									tempVal = '<a class="thumbnail">'
										+ '<img src="' + tempVal + '">'
										+ '</a>'
								}
                                var temp = "<div><div style=\"width: 85%; max-width: 300px; min-height: 25px; float:left; overflow-wrap: break-word;\"><b>" + key + ":</b> " + tempVal + "</div><div class=\"copySpan\" style=\"width: 15%; float:right\">"
                                    + "<span aria-label=\"Copy to clipboard\" data-microtip-position=\"top-left\" role=\"tooltip\" class=\"copy\" data-copyValue=\"" + key + "\" onClick=''>"
                                    + "<img src=\"images/copy.svg\" class=\"copy-icon clipboard\">"
                                    + "</span></div></div><br>";
                                if (key === "startedOnWorkerId") {
                                    after = after + temp;
                                    putAllAfter = 1;
                                } else if (putAllAfter === 0) {
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
            ],
            searchDelay: 250,
            select: {
                style: 'multi+shift',
                selector: 'td:first-child'
            },
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
                {"max-width": "300px", "targets": [9,12]},
            ],
            "stateSave": true,
            "stateLoadParams": function (settings, data) {
                data.columns.forEach(function (column) {
                    column.search.search = "";
                });
            },
            dom: "Q<'row'<'col-sm-auto buttons'B>><'row'<'col-sm-1 action-button'><'col-sm-5 length'l><'col-sm-6 filter'f>>" + "tip",
            buttons: [
                {
                    extend: 'colvis',
                    columns: ':not(.noVis)'
                },
                {
                    text: "Select all on page",
                    action: function () {
                        $("#processes-table").DataTable().rows({page: "current"}).select();
                        updateActionList();
                    }
                },
                {
                    text: "Unselect all on page",
                    action: function () {
                        $("#processes-table").DataTable().rows({page: "current"}).deselect();
                        updateActionList();
                    }
                },
                {
                    text: "Select all",
                    action: function () {
                        $("#processes-table").DataTable().rows({filter: "applied"}).select();
                        updateActionList();
                    }
                },
                {
                    text: "Unselect all",
                    action: function () {
                        $("#processes-table").DataTable().rows().deselect();
                        updateActionList();
                    }
                },
            ],
            searchBuilder: {
                columns: [2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
            }
        });

        var table = $("#processes-table").DataTable();
        table.on('select', function (e, dt, type, indexes) {
            updateActionList();
        });

        table.on('deselect', function (e, dt, type, indexes) {
            updateActionList();
        });

        $(document).on('click', '.copy', function (e) {
            e.preventDefault();
            var copyValue = $(this).attr('data-copyValue');
            copyInput(copyValue);
            $(this).attr('aria-label', 'Copied!');
            setTimeout(function () {
                $('.copy').attr('aria-label', 'Copy');
            }, 2000);
        });

        $('<button id="menu3" class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">&nbsp;Actions &nbsp;'
            + '<span class="caret"></span>'
            + '</button>'
            + '<ul id="action-list" class="dropdown-menu test" role="menu" aria-labelledby="menu3">'
            + `<li id="action_open_selected_new_tabs" class="disabled" role="presentation"><a id="action_open_selected_new_tabs_atag" role="menuitem">Open selected rows in new tabs (must not be pending)</a></li>`
            + `<li id="action_copy_all_selected_history_links" class="disabled" role="presentation"><a id="action_copy_all_selected_history_links_atag" role="menuitem">Copy all selected history links (must not be pending)</a></li>`
            + `<li id="action_download_selected_list" class="disabled" role="presentation"><a id="action_download_selected_list_atag" role="menuitem">Download list of selected processes (JSON) (must select at least one row)</a></li>`
            + `<li id="action_download_selected_json" class="disabled" role="presentation"><a id="action_download_selected_json_atag" role="menuitem">Download logs of selected processes (JSON) (all rows selected must not be pending)</a></li>`
            + `<li id="action_download_selected_csv" class="disabled" role="presentation"><a id="action_download_selected_csv_atag" role="menuitem">Download logs of selected processes (CSV) (all rows selected must not be pending)</a></li>`
            + `<li id="action_disable" class="disabled" role="presentation"><a id="action_disable_atag" role="menuitem">Disable selected rows (all rows selected must be 'pending')</a></li>`
            + `<li id="action_enable" class="disabled" role="presentation"><a id="action_enable_atag" role="menuitem">Enable selected rows (all rows selected must be 'disabled')</a></li>`
            + `<li id="action_retry_incident" class="disabled" role="presentation"><a id="action_retry_incident_atag" role="menuitem">Retry all selected incident rows (all rows selected must be 'incident')</a></li>`
            + `<li id="action_retry_failed_to_start" class="disabled" role="presentation"><a id="action_retry_failed_to_start_atag" role="menuitem">Retry all selected failed to start rows (all rows selected must be 'failedToStart')</a></li>`
            + `<li id="action_mark_as_resolved" class="disabled" role="presentation"><a id="action_mark_as_resolved_atag" role="menuitem">Mark all selected failed rows as resolved (all rows selected must be 'fail')</a></li>`
            + `<#include "adaptation-process-actions.ftl">`
            + `</ul>`).appendTo(".action-button");

        fetchAndDisplayProcesses();
    });

    //DOCUMENT.READY END

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
        var interval = setInterval(function () {
            if (doneArr.length === numCalls) {
                clearInterval(interval);
                $("#processes-table").DataTable().clear();
                $("#processes-table").DataTable().rows.add(returnedData).draw();
                //hide ajax spinner
                $(".ajax-spinner").hide();
            }
        }, 250);
    }

    function updateLocation(changeHideSubs) {
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
        var qstring = "?";
        if (localParams != null) {
            for (p in localParams) {
                qstring += encodeURI(p) + "=" + encodeURI(localParams[p]) + "&";
            }
        }
        qstring = qstring.substring(0, qstring.length - 1);
        console.log(encodeURI(qstring));
        window.location = "/${base}/processes" + qstring;
    }

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
                $("#super-proc-inst-id-in").show();
                $("#hide-subprocs-div").css('display', 'none');
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

    function viewHistory(procInstId) {

        if (procInstId !== '') {
            window.location = "/${base}/history?procInstId=" + procInstId;
        } else {
            return false;
        }
    }

    function viewSubProcs(procInstId) {

        if (procInstId !== '') {
            window.location = "/${base}/processes?superProcInstId=" + procInstId;
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

        var selectedRows = table.rows({selected: true});

        var numSelected = selectedRows.count();
        var numDisabledSelected = 0;
        var numPendingSelected = 0;
        var numIncidentSelected = 0;
        var numFailedToStartSelected = 0;
        var numFailedSelected = 0;
        var numComplete = 0;

        selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
            var data = this.data();
            switch (data[4]) {
                case 'disabled':
                    numDisabledSelected++;
                    break;
                case 'pending':
                    numPendingSelected++;
                    break;
                case 'incident':
                    numIncidentSelected++;
                    break;
                case 'failed_to_start':
                    numFailedToStartSelected++;
                    break;
                case 'failed':
                    numFailedSelected++;
                    break;
                case 'complete':
                    numComplete++;
                    break;
            }
        });

        if (numSelected > 0) {
            var disabled = numDisabledSelected == numSelected;
            var pending = numPendingSelected == numSelected;
            var incident = numIncidentSelected == numSelected;
            var failedToStart = numFailedToStartSelected == numSelected;
            var failed = numFailedSelected == numSelected;
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
        }

        if ((numSelected > 0)) {
            $("#action_download_selected_list").removeClass("disabled");
            $("#action_download_selected_list_atag").attr("href", "javascript:downloadListJSON();");
            if (numPendingSelected === 0) {
                $("#action_open_selected_new_tabs").removeClass("disabled");
                $("#action_open_selected_new_tabs_atag").attr("href", "javascript:action_open_selected_new_tabs();");
                $("#action_copy_all_selected_history_links").removeClass("disabled");
                $("#action_copy_all_selected_history_links_atag").attr("href", "javascript:action_copy_all_selected_history_links();");
                $("#action_download_selected_json").removeClass("disabled");
                $("#action_download_selected_json_atag").attr("href", "javascript:downloadSelectedJSON();");
                $("#action_download_selected_csv").removeClass("disabled");
                $("#action_download_selected_csv_atag").attr("href", "javascript:downloadSelectedCSV();");
            }
        }

        // Execute adaptation actions if any
        updateAdaptationActionList();
    }

    function action_open_selected_new_tabs() {
        var table = $("#processes-table").DataTable();
        var selectedRows = table.rows({selected: true});
        selectedRows.every(function (rowIdx, tableLoop, rowLoop) {
            var data = this.data();
            window.open("/${base}/history?procInstId=" + data["procInstId"], "_blank");
        });
    }

    function action_copy_all_selected_history_links() {
        var table = $("#processes-table").DataTable();
        const protocol = window.location.protocol;
        const host = window.location.host;
        var selectedRows = table.rows({selected: true});
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
                table.ajax.reload();
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
                table.ajax.reload();
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

    function copyInput(varValue) {
        navigator.clipboard.writeText(varValue);
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
                table.ajax.reload();
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
                table.ajax.reload();
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
        var selectedRows = table.rows({selected: true});

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

    function downloadSelectedJSON() {
        var mainJSON = {};
        //get selected rows
        var table = $('#processes-table').DataTable();
        var selectedRows = table.rows({selected: true});
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

    function downloadSelectedCSV() {
        var mainCSV = `"process_definition","process_instance","time stamp","type","source","details"\r\n`;
        //get selected rows
        var table = $('#processes-table').DataTable();
        var selectedRows = table.rows({selected: true});
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

    $("#json-bttn").click(function (e) {
        e.preventDefault();
        downloadListJSON();
    });

    function downloadListJSON() {
        var dt = $('#processes-table').DataTable();
        //number of rows
        var numRows = dt.rows({selected: true}).count();
        var jsonFile = {};
        var processes = {};

        dt.rows({selected: true, search: 'applied'}).every(function (rowIdx, tableLoop, rowLoop) {
            var data = this.data();
            console.log(data);
            var thisProcJSON = {};
            var startedOnWorker = "";
            var workerIP = "";
            var duration = "";
            var process_end = "";
            var inputVars = "";
            var inputVarsTemp = "";

            if (data["startedByWorker"] !== "") {
                startedOnWorker = data["startedByWorker"];
                workerIP = data["startedByWorker"].split("_").slice(0, -2).join(".");
            } else {
                startedOnWorker = data["startedByWorker"];
            }

            if (data["procEndTime"] !== "") {
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

            processes[data["procInstId"]] = thisProcJSON;
        });
        jsonFile["processes"] = processes;
        console.log(jsonFile);
        $.fn.dataTable.fileSave(
            new Blob([JSON.stringify(jsonFile)]),
            'processes_export.json'
        );
    }
</script>
<script src="/${base}/js/cws.js"></script>

</body>
</html>