<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="/">

        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text>
<html>
    <head>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css"/>
        <link rel="stylesheet" type="text/css" href="/css/AODNTheme.css?v="/>
        
        <title>IMOS download - bdba1751-aa01-4c88-af95-43687eae8363</title>
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
    
        <a href="https://portal.aodn.org.au/proxy?url=http%3A%2F%2Fgeoserver-wps.aodn.org.au%2Fgeoserver%2Fows%3Fservice%3DWPS%26version%3D1.0.0%26request%3DGetExecutionResult%26executionId%3Dbdba1751-aa01-4c88-af95-43687eae8363%26outputId%3Dresult.nc%26mimetype%3Dapplication%252Fx-netcdf&amp;proxyContentType=true">
    

            bdba1751-aa01-4c88-af95-43687eae8363

    
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
    

            
                  Download ready
                

    
</dd>

                <dt>
    Download
    :
</dt>
<dd>
    

            
                    <a href="https://portal.aodn.org.au/proxy?url=http%3A%2F%2Fgeoserver-wps.aodn.org.au%2Fgeoserver%2Fows%3Fservice%3DWPS%26version%3D1.0.0%26request%3DGetExecutionResult%26executionId%3Dbdba1751-aa01-4c88-af95-43687eae8363%26outputId%3Dresult.nc%26mimetype%3Dapplication%252Fx-netcdf&amp;proxyContentType=true">IMOS download - bdba1751-aa01-4c88-af95-43687eae8363</a>
                

    
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