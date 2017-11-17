<html>
    <head>
        <link rel="stylesheet" href="${bootstrapCssLocation}"/>
        <link rel="stylesheet" type="text/css" href="${aodnCssLocation}"/>

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
                        <TR><TD>${job_index}</TD><TD>${job.jobId}</TD><TD>${job.createdAt}</TD><TD>${job.status}</TD></TR>
                    </#list>
                </TABLE>
            <#else>
                No jobs are currently queued.
            </#if>

            <H3>Running Jobs</H3>

            <#if runningJobsList??>
                <TABLE BORDER="1">
                <TR><TH>Job ID</TH><TH>Submitted</TH><TH>Status</TH></TR>
                <#list runningJobsList as job>
                    <TR><TD>${job.jobId}</TD><TD>${job.createdAt}</TD><TD>${job.status}</TD></TR>
                </#list>
                </TABLE>
            <#else>
                No jobs are currently running.
            </#if>

            <H3>Completed Jobs</H3>

            <#if completedJobsList??>
                <TABLE BORDER="1">
                <TR><TH>Submitted</TH><TH>Job ID</TH><TH>Completed</TH><TH>Status</TH></TR>
                <#list completedJobsList as job>
                <TR><TD>${job.createdAt}</TD><TD>${job.jobId}</TD><TD>${job.stoppedAt}</TD><TD>${job.status}</TD></TR>
                </#list>
            </TABLE>
            <#else>
                No completed jobs found.
            </#if>

        </div>
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