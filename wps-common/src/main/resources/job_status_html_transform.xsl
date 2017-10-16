<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns3="http://www.opengis.net/wps/1.0.0">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/">
        <xsl:param name="jobid" />
        <xsl:variable name="status"><xsl:value-of select="ns3:ExecuteResponse/@statusLocation"/></xsl:variable>
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text>
<html>
    <head>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="/css/AODNTheme.css?v="/>
        
        <title>IMOS download - <xsl:value-of select="$jobid"/></title>
    </head>
    <body>
        <div class="portalheader">
            <div class="container">
                <a class="btn" role="button" href="https://portal.aodn.org.au">
                    <img src="/images/AODN/AODN_logo_fullText.png" alt="Portal logo"/>
                </a>
            </div>
        </div>
        <div class="container">
            <h2>WPS status</h2>
            <dl>

                <dt>
    Job Id
    :
</dt>
<dd>
    
        <a href="{$status}">

            <xsl:value-of select="$jobid" />

        </a>
    
</dd>

                <dt>
    Submitted
    :
</dt>
<dd>
    

            
                    <label id="localTime"></label>
                    <script type="text/javascript">

                       if('2017-08-09T05:08:35.530Z')
                       {
                            var date = new Date('2017-08-09T05:08:35.530Z');
                            document.getElementById('localTime').innerHTML = date;
                       }
                    
</script>

                

    
</dd>

                <dt>
    Status
    :
</dt>
<dd>
    

            
                  Need to calculate/pass in
                

    
</dd>

                <dt>
    Download
    :
</dt>
<dd>
    

            
                    <a href="{$status}">IMOS download - <xsl:value-of select="$jobid"/></a>
                

    
</dd>

                
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