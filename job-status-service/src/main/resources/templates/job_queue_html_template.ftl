<html>
    <head>
        <style type="text/css">
            html {background-color: white; margin-top: 0 ! important;}
            body, html, td, p, button { font-family: 'Arimo', sans-serif; }
            body, html, td { color: #4D5B63; }
            h2 {margin-bottom: 14px;}
            h1, h2, h3, h4,.x-panel-header,.x-window-header-text,.search-filter-panel,.filter-selection-panel-header-selected {color: #4D5B63;cursor: default;}
            p {margin-bottom: 10px;}
            .portalheader, .jumbotronFooter {padding: 10px 0;background-color: #1a5173;}
            .jumbotronFooter {padding: 48px 0;}
            .jumbotronFooter * {color: #ffffff;}
            .jumbotronFooter a {color: #ccc;}
            table { border: 0; width: 95%; font-size: 14px; }
            th {border: 1px solid black; color: #ffffff; background:#1a5273; }
            tr:hover td { background:#dfe4e6; }
            table td {border: 1px solid black; padding: 0px 10px 0px 10px; text-align: left; }
        </style>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>

        <script type="text/javascript">
            function formatTime(timeStamp) {
                if (timeStamp != -1) {
                    var submitTime = new Date(0);
                    submitTime.setUTCSeconds(timeParamValue);
                    var dateString = ('0' + submitTime.getDate()).slice(-2) + "/" +
                                     ('0' + (submitTime.getMonth() + 1)).slice(-2) + "/" +
                                     submitTime.getFullYear() + " " +
                                     ('0' + submitTime.getHours()).slice(-2) + ":" +
                                     ('0' + submitTime.getMinutes()).slice(-2) + ":" +
                                     ('0' + submitTime.getSeconds()).slice(-2);
                    return dateString;
                } else {
                    return "Unknown";
                }
            }

            function openJobStatusLink(jobId) {
                window.open('${statusServiceBaseLink}' + '&jobId=' + jobId,'_blank');
            }
        </SCRIPT>
        <title>WPS QUEUE CONTENTS: ${queueName}</title>
    </head>
    <body>
        <div class="portalheader">
            <div class="container">
                <a class="btn" role="button" href="https://portal.aodn.org.au">
                    <img src="https://static.emii.org.au/images/logo/AODN_logo_fullText.png" alt="Portal logo"/>
                </a>
            </div>
        </div>
        <div class="container">
            <H2>WPS QUEUE CONTENTS: ${queueName}</H2>

            <H3>Queued Jobs</H3>

            <#if queuedJobsList??>
                <TABLE BORDER>
                    <TR><TH>Position</TH><TH>Job ID</TH><TH>Submitted</TH><TH>Status</TH></TR>
                    <#list queuedJobsList as job>
                        <TR><TD>${job_index}</TD>
                            <TD><A HREF="javascript:openJobStatusLink('${job.jobId}');">${job.jobId}</A></TD>
                            <TD>
                                <script type="text/javascript">
                                    var timeParamValue = ${job.createdAt?c}/1000;
                                    document.write(formatTime(timeParamValue));
                                </script>
                            </TD>
                            <TD>${job.status}</TD>
                        </TR>
                    </#list>
                </TABLE>
            <#else>
                No jobs are currently queued.
            </#if>

            <H3>Running Jobs</H3>

            <#if runningJobsList??>
                <TABLE BORDER="1">
                <TR><TH>Job ID</TH><TH>Submitted</TH><TH>Started</TH><TH>Status</TH><TH>Log File</TH></TR>
                <#list runningJobsList as job>
                    <TR><TD><A HREF="javascript:openJobStatusLink('${job.awsBatchJobDetail.jobId}');">${job.awsBatchJobDetail.jobId}</A></TD>
                        <TD>
                            <script type="text/javascript">
                                var timeParamValue = ${job.awsBatchJobDetail.createdAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </TD>
                        <TD>
                            <script type="text/javascript">
                                var timeParamValue = ${job.awsBatchJobDetail.startedAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </TD>
                        <TD>${job.awsBatchJobDetail.status}</TD>
                        <TD><A HREF="${job.logFileLink}" rel="noopener noreferrer" target="_blank">Log</A></TD>
                    </TR>
                </#list>
                </TABLE>
            <#else>
                No jobs are currently running.
            </#if>

            <H3>Completed Jobs</H3>

            <#if completedJobsList??>
                <TABLE BORDER="1">
                <TR><TH>Job ID</TH><TH>Submitted</TH><TH>Started</TH><TH>Completed</TH><TH>Job Status</TH><TH>Aggregation Result</TH><TH>Log File</TH></TR>
                <#list completedJobsList as job>
                <TR><TD><A HREF="javascript:openJobStatusLink('${job.awsBatchJobDetail.jobId}');">${job.awsBatchJobDetail.jobId}</A></TD>
                    <TD>
                        <script type="text/javascript">
                            var timeParamValue = ${job.awsBatchJobDetail.createdAt?c}/1000;
                            document.write(formatTime(timeParamValue));
                        </script>
                    </TD>
                    <TD>
                        <#if (job.awsBatchJobDetail.startedAt)??>
                            <script type="text/javascript">
                                var timeParamValue = ${job.awsBatchJobDetail.startedAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </#if>
                    </TD>
                    <TD>
                        <#if (job.awsBatchJobDetail.stoppedAt)??>
                            <script type="text/javascript">
                                var timeParamValue = ${job.awsBatchJobDetail.stoppedAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </#if>
                    </TD>
                    <TD>${job.awsBatchJobDetail.status}</TD>
                    <TD>${job.wpsStatusDescription}</TD>
                    <TD><A HREF="${job.logFileLink}" rel="noopener noreferrer" target="_blank">Log</A></TD>
                </TR>
                </#list>
            </TABLE>
            <#else>
                No completed jobs found.
            </#if>

        </div>
        <BR/>
        <BR/>
        <div class="jumbotronFooter voffset5">
            <div class="container">
                <footer class="row">
                    <div class="col-md-4">
                        <p>If you've found this information useful, see something wrong, or have a suggestion,
                            please let us
                            know.
                            All feedback is very welcome. For help and information about this site
                            please contact <a href="mailto:info@aodn.org.au">info@aodn.org.au</a></p>
                    </div>
                    <div class="col-md-8">
                        <p>Use of this web site and information available from it is subject to our <a href="http://imos.org.au/imostermsofuse0.html">
                            Conditions of use
                        </a></p>
                    </div>
                </footer>
            </div>
        </div>
    </body>
</html>