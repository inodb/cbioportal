<%@ page import="org.mskcc.cbio.portal.servlet.QueryBuilder" %>
<%@ page import="org.mskcc.cbio.portal.util.GlobalProperties" %>

<%
    String siteTitle = GlobalProperties.getTitle();
%>

<% request.setAttribute(QueryBuilder.HTML_TITLE, siteTitle+"::Video Tutorial"); %>
<jsp:include page="WEB-INF/jsp/global/header.jsp" flush="true" />

    <div id="main">
        <table cellspacing="2px">
        <tr>
            <td>
                <h1>Tutorials</h1>
                <h2>Step-by-step Guide to cBioPortal: a Protocol Paper</h2>
                <p>Gao, Aksoy, Dogrusoz, Dresdner, Gross, Sumer, Sun, Jacobsen, Sinha, Larsson, Cerami, Sander, Schultz. <br/>
                    <b>Integrative analysis of complex cancer genomics and clinical profiles using the cBioPortal.</b> <br/>
                    <i>Sci. Signal.</i> 6, pl1 (2013).
                    [<a href="http://www.cbioportal.org/public-portal/sci_signal_reprint.jsp">Reprint</a>].</p>
                
                <h2>Tutorial #1:  Getting Started</h2>
                <div style="width:595px" id="__ss_10438088"> <strong style="display:block;margin:12px 0 4px"><a href="http://www.slideshare.net/EthanCerami/cbio-cancer-genomics-portal-getting-started" title="cBioPortal for Cancer Genomics: Getting started" target="_blank">cBioPortal for Cancer Genomics: Getting started</a></strong> <iframe src="http://www.slideshare.net/slideshow/embed_code/10438088" width="595" height="497" frameborder="0" marginwidth="0" marginheight="0" scrolling="no"></iframe> <div style="padding:5px 0 12px"> View more <a href="http://www.slideshare.net/" target="_blank">presentations</a> from <a href="http://www.slideshare.net/EthanCerami" target="_blank">Ethan Cerami</a> </div> </div>

                <h2>Tutorial #2:  Network View</h2>
                <div style="width:595px" id="__ss_10579031"> <strong style="display:block;margin:12px 0 4px"><a href="http://www.slideshare.net/EthanCerami/network-view" title="cBioPortal for Cancer Genomics: Network View" target="_blank">cBioPortal for Cancer Genomics: Network View</a></strong> <iframe src="http://www.slideshare.net/slideshow/embed_code/10579031" width="595" height="497" frameborder="0" marginwidth="0" marginheight="0" scrolling="no"></iframe> <div style="padding:5px 0 12px"> View more <a href="http://www.slideshare.net/" target="_blank">presentations</a> from <a href="http://www.slideshare.net/EthanCerami" target="_blank">Ethan Cerami</a> </div> </div>
            </td>
        </tr>
    </table>
        </div>
    </td>
    <td width="172">
	<jsp:include page="WEB-INF/jsp/global/right_column.jsp" flush="true" />
    </td>
  </tr>
  <tr>
    <td colspan="3">
	<jsp:include page="WEB-INF/jsp/global/footer.jsp" flush="true" />
    </td>
  </tr>
</table>
</center>
</div>
</form>
<jsp:include page="WEB-INF/jsp/global/xdebug.jsp" flush="true" />
</body>
</html>