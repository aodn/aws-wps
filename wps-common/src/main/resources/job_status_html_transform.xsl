<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://www.opengis.net/ows/1.1" xmlns:ns3="http://www.opengis.net/wps/1.0.0">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:param name="jobid" />
    <xsl:param name="statusDescription" />
    <xsl:param name="submittedTime" />

    <xsl:template match="/">

        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text>
        <html>
        <head>
            <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
            <link rel="stylesheet" type="text/css" href="https://portal.aodn.org.au/css/AODNTheme.css?v="/>

            <title>IMOS download - <xsl:value-of select="$jobid"/></title>
        </head>
        <body>
        <div class="portalheader">
            <div class="container">
                <a class="btn" role="button" href="https://portal.aodn.org.au">
                    <img src="https://portal.aodn.org.au/images/AODN/AODN_logo_fullText.png" alt="Portal logo"/>
                </a>
            </div>
        </div>
        <div class="container">
        <h2>WPS status</h2>

        <xsl:call-template name="response">
            <xsl:with-param name="jobid" />
            <xsl:with-param name="statusDescription" />
            <xsl:with-param name="submittedTime" />
        </xsl:call-template>

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

    <xsl:template match="ns3:ExecuteResponse" name="response">
        <xsl:variable name="statusLocation"><xsl:value-of select="@statusLocation"/></xsl:variable>

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
                <!--<dd><xsl:value-of select="$submittedTime" /></dd>-->
                <dt>Status :</dt>
                <dd><xsl:value-of select="$statusDescription" /></dd>
                <xsl:for-each select="ns3:ExecuteResponse/ns3:ProcessOutputs">
                    <dt>Download :</dt>
                    <xsl:for-each select="ns3:Output">
                        <xsl:variable name="downloadLink"><xsl:value-of select="ns3:Reference/@href"/></xsl:variable>
                        <xsl:variable name="outputName"><xsl:value-of select="ns1:Identifier"/></xsl:variable>
                        <dd>Download [<xsl:value-of select="$outputName"/>] : <a href="{$downloadLink}"><xsl:value-of select="$jobid"/></a></dd>
                    </xsl:for-each>
                </xsl:for-each>
            </dl>
    </xsl:template>


</xsl:stylesheet>