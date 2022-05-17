<!-- override with adaptation actions 
     For example to add one action to the dropdown create a
     file adaptation-process-actions.ftl in your adaptation project with 
     
     <li id="action_retry_failed_error" class="disabled" role="presentation"><a role="menuitem" href="javascript:action_retry_failed_error();">Retry all selected failed rows (all rows selected must be 'fail')</a></li>

     Add the javascript functions invoked by these actions [action_retry_failed_error() in the action above] 
     to the adaptation-process-actions.js file in your adaptation project
     In your adaptation project override this file with
     cp your-cws-adaptation/src/main/resources/cws-ui/adaptation-process-actions.ftl common-workflow-service/install/cws-ui/adaptation-process-actions.ftl
-->
     
