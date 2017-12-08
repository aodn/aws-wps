<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://www.opengis.net/ows/1.1" xmlns:ns3="http://www.opengis.net/wps/1.0.0">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <xsl:param name="jobid" />
        <xsl:param name="statusDescription" />
        <xsl:param name="submittedTime" />
        <xsl:param name="bootstrapCssLocation"/>
        <xsl:param name="aodnCssLocation"/>
        <xsl:param name="aodnLogoLocation"/>
        <xsl:param name="requestXML"/>
        <xsl:param name="logFileLink"/>

        <xsl:variable name="result">
            <xsl:value-of select="ns3:ExecuteResponse/ns3:ProcessOutputs/ns3:Output[ns1:Identifier = 'result']/ns3:Reference/@href"/>
        </xsl:variable>
        <xsl:variable name="provenance">
            <xsl:value-of select="ns3:ExecuteResponse/ns3:ProcessOutputs/ns3:Output[ns1:Identifier = 'provenance']/ns3:Reference/@href"/>
        </xsl:variable>
        <xsl:variable name="errorMsg">
            <xsl:value-of select="ns3:ExecuteResponse/ns3:Status/ns3:ProcessFailed/ns1:ExceptionReport/ns1:Exception/ns1:ExceptionText/text()"/>
        </xsl:variable>

        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text>

        <html>
        <head>
            <link rel="stylesheet" href="{$bootstrapCssLocation}"/>
            <link rel="stylesheet" type="text/css" href="{$aodnCssLocation}"/>

            <title>IMOS Job Status - <xsl:value-of select="$jobid"/></title>
        </head>
        <body>
        <div class="portalheader">
            <div class="container">
                <a class="btn" role="button" href="https://portal.aodn.org.au">
                    <img src="{$aodnLogoLocation}" alt="Portal logo"/>
                </a>
            </div>
        </div>
        <div class="container">
        <h2>WPS status</h2>

        <dl>
            <dt>Job Id :</dt>
            <dd><xsl:value-of select="$jobid" /></dd>
            <dt>Submitted :</dt>
            <dd>
                <script type="text/javascript">
                    var timeParamValue = <xsl:value-of select="$submittedTime"/>;
                    if(timeParamValue != -1) {
                    var submitTime = new Date(0);
                    submitTime.setUTCSeconds(timeParamValue);
                    document.write( submitTime.toString() );
                    } else {
                    document.write("Unknown");
                    }
                </script>
            </dd>
            <xsl:choose>
                <xsl:when test="$errorMsg != ''">
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
                        <xsl:value-of select="$errorMsg"/>
                        <br />
                    </dd>
                </xsl:when>
                <xsl:otherwise>
                    <dt>
                        Status :
                    </dt>
                    <dd>
                        <xsl:value-of select="$statusDescription"/>
                        <br />
                    </dd>
                    <xsl:if test="$result != ''">
                        <dt>
                            Download :
                        </dt>
                        <dd>
                            <a href="{$result}">IMOS download -
                                <xsl:value-of select="$jobid"/>
                            </a>
                            <br />
                        </dd>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="$requestXML != ''">
                    <dt>
                        Request XML :
                    </dt>
                    <dd>
                        <textarea rows="1" cols="120" onfocus="this.rows=15;" onblur="this.rows=1;" style="resize: none;" readonly="true"><xsl:value-of select="$requestXML"/></textarea>
                        <br />
                    </dd>
                </xsl:when>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="$logFileLink != ''">
                    <dt>
                        Log file :
                    </dt>
                    <dd>
                        <A HREF="{$logFileLink}">Log file</A>
                        <br />
                    </dd>
                </xsl:when>
            </xsl:choose>
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
    </xsl:template>

</xsl:stylesheet>