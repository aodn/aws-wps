<html>
    <head>
        <link rel="stylesheet" href="${bootstrapCssLocation}"/>
        <link rel="stylesheet" type="text/css" href="${aodnCssLocation}"/>

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
                    <img src="${aodnLogoLocation}" alt="Portal logo"/>
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
                <TR><TH>Job ID</TH><TH>Submitted</TH><TH>Started</TH><TH>Status</TH></TR>
                <#list runningJobsList as job>
                    <TR><TD><A HREF="javascript:openJobStatusLink('${job.jobId}');">${job.jobId}</A></TD>
                        <TD>
                            <script type="text/javascript">
                                var timeParamValue = ${job.startedAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </TD>
                        <TD>
                            <script type="text/javascript">
                                var timeParamValue = ${job.createdAt?c}/1000;
                                document.write(formatTime(timeParamValue));
                            </script>
                        </TD>
                        <TD>${job.status}</TD></TR>
                </#list>
                </TABLE>
            <#else>
                No jobs are currently running.
            </#if>

            <H3>Completed Jobs</H3>

            <#if completedJobsList??>
                <TABLE BORDER="1">
                <TR><TH>Job ID</TH><TH>Submitted</TH><TH>Started</TH><TH>Completed</TH><TH>Status</TH></TR>
                <#list completedJobsList as job>
                <TR><TD><A HREF="javascript:openJobStatusLink('${job.jobId}');">${job.jobId}</A></TD>
                    <TD>
                        <script type="text/javascript">
                            var timeParamValue = ${job.createdAt?c}/1000;
                            document.write(formatTime(timeParamValue));
                        </script>
                    </TD>
                    <TD>
                        <script type="text/javascript">
                            var timeParamValue = ${job.startedAt?c}/1000;
                            document.write(formatTime(timeParamValue));
                        </script>
                    </TD>
                    <TD>
                        <script type="text/javascript">
                            var timeParamValue = ${job.stoppedAt?c}/1000;
                            document.write(formatTime(timeParamValue));
                        </script>
                    </TD>
                    <TD>${job.status}</TD></TR>
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