<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://www.opengis.net/ows/1.1" xmlns:ns3="http://www.opengis.net/wps/1.0.0">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <xsl:param name="jobid"/>
        <xsl:variable name="statusLocation">
            <xsl:value-of select="ns3:ExecuteResponse/@statusLocation"/>
        </xsl:variable>
        <xsl:variable name="status">
            <xsl:value-of select="ns3:ExecuteResponse/ns3:Status/*/text()"/>
        </xsl:variable>
        <xsl:variable name="creationTime">
            <xsl:value-of select="ns3:ExecuteResponse/ns3:Status/@creationTime"/>
        </xsl:variable>
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
                <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
                <link rel="stylesheet" type="text/css" href="https://portal.aodn.org.au/css/AODNTheme.css?v="/>

                <title>IMOS download -
                    <xsl:value-of select="$jobid"/>
                </title>
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
                    <dl>
                        <dt>
                            Job Id :
                        </dt>
                        <dd>
                            <xsl:value-of select="$jobid"/>
                            <br />
                        </dd>
                        <dt>
                            Submitted :
                        </dt>
                        <dd>
                            <label id="localTime">
                                <xsl:value-of select="$creationTime"/>
                            </label>
                            <script type="text/javascript">
                                var unformattedDate = document.getElementById('localTime').innerHTML;
                                var date = new Date(unformattedDate);
                                document.getElementById('localTime').innerHTML = date;
                            </script>
                            <br />
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
                                    <xsl:value-of select="$status"/>
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
                                    please contact
                                    <a href="mailto:info@aodn.org.au">info@aodn.org.au</a>
                                </p>
                            </div>
                            <div class="col-md-8">
                                <p>Use of this web site and information available from it is subject to our
                                    <a href="http://imos.org.au/imostermsofuse0.html">
                                        Conditions of use
                                    </a>
                                </p>
                            </div>
                        </footer>
                    </div>
                </div>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
