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

        <title>IMOS Job Status - ${jobId}</title>
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
    <h2>WPS status</h2>

    <dl>
        <dt>Job Id :</dt>
        <dd>${jobId}</dd>
        <dt>Submitted :</dt>
        <dd>
            <script type="text/javascript">
                var timeParamValue = ${submittedTime}/1000;
                if(timeParamValue != -1) {
                    var submitTime = new Date(0);
                    submitTime.setUTCSeconds(timeParamValue);
                    document.write( submitTime.toString() );
                } else {
                    document.write("Unknown");
                }
            </script>
        </dd>
        <#if executeResponse.status.processFailed??>
            <dt>
                Status :
            </dt>
            <dd>
                Failed
                <br />
            </dd>
            <dt>
                Error message :
            </dt>
            <dd>
                ${executeResponse.status.processFailed.exceptionReport.exception[0].exceptionText[0]}
                <br />
            </dd>
        <#else>
            <dt>
                Status :
            </dt>
            <dd>
                ${statusDescription}
                <br />
            </dd>
            <#if executeResponse.processOutputs??>
                <dt>
                    Download :
                </dt>
                <#list executeResponse.processOutputs.output as currentOutput>
                    <#if currentOutput.identifier.value == "result">
                        <dd>
                            <a href="${currentOutput.reference.href}">IMOS download - ${jobId}</a>
                            <br />
                        </dd>
                    </#if>
                    <#if currentOutput.identifier.value == "provenance">
                        <dd>
                            <a href="${currentOutput.reference.href}">Provenance file - ${jobId}</a>
                            <br />
                        </dd>
                    </#if>
                </#list>
            </#if>
        </#if>
        <#if requestXML??>
            <dt>
                Request XML :
            </dt>
            <dd>
                <textarea rows="1" cols="120" onfocus="this.rows=15;" onblur="this.rows=1;" style="resize: none;" readonly="true">${requestXML}</textarea>
                <br />
            </dd>
        </#if>
        <#if logFileLink??>
            <dt>
                Log file :
            </dt>
            <dd>
                <a href="${logFileLink}" rel="noopener noreferrer" target="_blank">Log file</a>
                <br />
            </dd>
        </#if>
    </dl>
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